package com.framecapture.capture

import android.graphics.Bitmap
import android.util.Log
import com.framecapture.Constants
import com.framecapture.models.CaptureRegion
import java.nio.ByteBuffer

/**
 * Detects changes between consecutive frames using pixel sampling
 *
 * Uses an efficient sampling algorithm that compares a subset of pixels
 * to determine if the screen has changed significantly enough to warrant capture.
 */
class ChangeDetector(
    private val threshold: Float = Constants.DEFAULT_CHANGE_THRESHOLD,
    private val sampleRate: Int = Constants.DEFAULT_CHANGE_SAMPLE_RATE,
    private val detectionRegion: CaptureRegion? = null
) {
    companion object {
        private const val TAG = "ChangeDetector"
    }

    // Store previous frame's sampled pixel data
    private var previousSamples: IntArray? = null
    private var previousWidth: Int = 0
    private var previousHeight: Int = 0

    /**
     * Detects change between current bitmap and previous frame
     * @return Change percentage (0-100), or 100 if no previous frame exists
     */
    fun detectChange(currentBitmap: Bitmap): Float {
        val width = currentBitmap.width
        val height = currentBitmap.height

        // Calculate sampling bounds based on detection region
        val (startX, startY, endX, endY) = calculateBounds(width, height)

        // Sample current frame
        val currentSamples = samplePixels(currentBitmap, startX, startY, endX, endY)

        // If no previous frame, treat as 100% change (always capture first frame)
        if (previousSamples == null || previousWidth != width || previousHeight != height) {
            previousSamples = currentSamples
            previousWidth = width
            previousHeight = height
            Log.d(TAG, "No previous frame - returning 100% change")
            return 100f
        }

        // Compare samples
        val changedPixels = countChangedPixels(previousSamples!!, currentSamples)
        val totalSamples = currentSamples.size
        val changePercent = if (totalSamples > 0) {
            (changedPixels.toFloat() / totalSamples.toFloat()) * 100f
        } else {
            0f
        }

        Log.d(TAG, "Change detected: $changePercent% ($changedPixels/$totalSamples samples)")
        return changePercent
    }

    /**
     * Updates the previous frame data for next comparison
     */
    fun updatePreviousFrame(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val (startX, startY, endX, endY) = calculateBounds(width, height)

        previousSamples = samplePixels(bitmap, startX, startY, endX, endY)
        previousWidth = width
        previousHeight = height
    }

    /**
     * Clears stored frame data
     */
    fun clear() {
        previousSamples = null
        previousWidth = 0
        previousHeight = 0
    }

    /**
     * Calculates bounds for sampling based on detection region
     */
    private fun calculateBounds(width: Int, height: Int): BoundsResult {
        return if (detectionRegion != null) {
            when (detectionRegion.unit) {
                Constants.POSITION_UNIT_PERCENTAGE -> {
                    val startX = (detectionRegion.x * width).toInt().coerceIn(0, width - 1)
                    val startY = (detectionRegion.y * height).toInt().coerceIn(0, height - 1)
                    val endX = ((detectionRegion.x + detectionRegion.width) * width).toInt().coerceIn(startX + 1, width)
                    val endY = ((detectionRegion.y + detectionRegion.height) * height).toInt().coerceIn(startY + 1, height)
                    BoundsResult(startX, startY, endX, endY)
                }
                Constants.POSITION_UNIT_PIXELS -> {
                    val startX = detectionRegion.x.toInt().coerceIn(0, width - 1)
                    val startY = detectionRegion.y.toInt().coerceIn(0, height - 1)
                    val endX = (detectionRegion.x + detectionRegion.width).toInt().coerceIn(startX + 1, width)
                    val endY = (detectionRegion.y + detectionRegion.height).toInt().coerceIn(startY + 1, height)
                    BoundsResult(startX, startY, endX, endY)
                }
                else -> BoundsResult(0, 0, width, height)
            }
        } else {
            BoundsResult(0, 0, width, height)
        }
    }

    /**
     * Samples pixels at regular intervals from the bitmap
     */
    private fun samplePixels(bitmap: Bitmap, startX: Int, startY: Int, endX: Int, endY: Int): IntArray {
        val regionWidth = endX - startX
        val regionHeight = endY - startY

        // Calculate sample points
        val samplesX = maxOf(1, regionWidth / sampleRate)
        val samplesY = maxOf(1, regionHeight / sampleRate)
        val totalSamples = samplesX * samplesY

        val samples = IntArray(totalSamples)
        var index = 0

        for (sy in 0 until samplesY) {
            val y = startY + (sy * sampleRate).coerceIn(0, regionHeight - 1)
            for (sx in 0 until samplesX) {
                val x = startX + (sx * sampleRate).coerceIn(0, regionWidth - 1)
                if (x < bitmap.width && y < bitmap.height) {
                    samples[index] = bitmap.getPixel(x, y)
                }
                index++
            }
        }

        return samples
    }

    /**
     * Counts pixels that have changed significantly between frames
     */
    private fun countChangedPixels(previous: IntArray, current: IntArray): Int {
        if (previous.size != current.size) {
            return current.size // All pixels considered changed if sizes don't match
        }

        var changedCount = 0
        val tolerance = Constants.CHANGE_PIXEL_TOLERANCE

        for (i in previous.indices) {
            if (isPixelChanged(previous[i], current[i], tolerance)) {
                changedCount++
            }
        }

        return changedCount
    }

    /**
     * Checks if a pixel has changed beyond the tolerance threshold
     */
    private fun isPixelChanged(pixel1: Int, pixel2: Int, tolerance: Int): Boolean {
        // Extract RGB components (ignore alpha)
        val r1 = (pixel1 shr 16) and 0xFF
        val g1 = (pixel1 shr 8) and 0xFF
        val b1 = pixel1 and 0xFF

        val r2 = (pixel2 shr 16) and 0xFF
        val g2 = (pixel2 shr 8) and 0xFF
        val b2 = pixel2 and 0xFF

        // Check if any channel differs beyond tolerance
        return kotlin.math.abs(r1 - r2) > tolerance ||
               kotlin.math.abs(g1 - g2) > tolerance ||
               kotlin.math.abs(b1 - b2) > tolerance
    }

    /**
     * Simple data class for bounds result
     */
    private data class BoundsResult(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int
    )
}
