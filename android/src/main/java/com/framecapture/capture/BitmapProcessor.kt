package com.framecapture.capture

import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import com.framecapture.models.CaptureOptions
import com.framecapture.models.CaptureRegion
import java.nio.ByteBuffer

/**
 * Handles bitmap conversion and processing operations
 *
 * Converts Android Image objects to Bitmap and applies transformations:
 * - Pixel buffer conversion from RGBA_8888 format
 * - Row padding removal
 * - Status bar cropping (if excludeStatusBar is enabled)
 * - Custom region cropping (if captureRegion is specified)
 *
 * Properly manages bitmap memory by recycling intermediate bitmaps.
 */
class BitmapProcessor(
    private val statusBarHeight: Int
) {

    /**
     * Converts an Image to a Bitmap with optional cropping
     *
     * Performs the following operations in order:
     * 1. Converts RGBA_8888 pixel buffer to Bitmap
     * 2. Removes row padding if present
     * 3. Crops status bar if excludeStatusBar is enabled
     * 4. Crops to custom region if captureRegion is specified
     *
     * @param image The Image from ImageReader
     * @param captureOptions Capture options (for cropping configuration)
     * @return Processed Bitmap ready for overlay rendering and storage
     */
    fun imageToBitmap(image: Image, captureOptions: CaptureOptions?): Bitmap {
        try {
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            // Create bitmap with the correct dimensions
            var bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )

            // Copy pixel data to bitmap
            bitmap.copyPixelsFromBuffer(buffer)

            // Crop row padding if present
            if (rowPadding > 0) {
                val croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    image.width,
                    image.height
                )
                bitmap.recycle()
                bitmap = croppedBitmap
            }

            // Crop status bar if requested
            val excludeStatusBar = captureOptions?.excludeStatusBar ?: false
            if (excludeStatusBar && statusBarHeight > 0 && statusBarHeight < bitmap.height) {
                val croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    statusBarHeight,  // Start below status bar
                    bitmap.width,
                    bitmap.height - statusBarHeight  // Reduced height
                )
                bitmap.recycle()
                bitmap = croppedBitmap
            }

            // Crop custom region if specified
            val captureRegion = captureOptions?.captureRegion
            if (captureRegion != null) {
                bitmap = cropToRegion(bitmap, captureRegion)
            }

            return bitmap

        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert image to bitmap", e)
            throw e
        }
    }

    /**
     * Crops bitmap to the specified custom region
     *
     * Supports both coordinate systems:
     * - Pixel-based: Absolute pixel coordinates
     * - Percentage-based: Relative to bitmap dimensions (0.0 to 1.0)
     *
     * Validates and clamps coordinates to prevent out-of-bounds errors.
     *
     * @param bitmap The bitmap to crop
     * @param region Custom region specification
     * @return Cropped bitmap (original is recycled)
     */
    private fun cropToRegion(bitmap: Bitmap, region: CaptureRegion): Bitmap {
        try {
            // Calculate actual pixel coordinates
            val (x, y, width, height) = if (region.unit == com.framecapture.Constants.POSITION_UNIT_PERCENTAGE) {
                // Convert percentages to pixels
                val cropX = (region.x * bitmap.width).toInt()
                val cropY = (region.y * bitmap.height).toInt()
                val cropWidth = (region.width * bitmap.width).toInt()
                val cropHeight = (region.height * bitmap.height).toInt()
                listOf(cropX, cropY, cropWidth, cropHeight)
            } else {
                // Use pixel values directly
                listOf(region.x.toInt(), region.y.toInt(), region.width.toInt(), region.height.toInt())
            }

            // Validate bounds
            val safeX = maxOf(0, minOf(x, bitmap.width - 1))
            val safeY = maxOf(0, minOf(y, bitmap.height - 1))
            val safeWidth = minOf(width, bitmap.width - safeX)
            val safeHeight = minOf(height, bitmap.height - safeY)

            if (safeWidth <= 0 || safeHeight <= 0) {
                return bitmap
            }

            // Crop the bitmap
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                safeX,
                safeY,
                safeWidth,
                safeHeight
            )

            // Recycle original bitmap
            bitmap.recycle()

            return croppedBitmap

        } catch (e: Exception) {
            return bitmap
        }
    }

    companion object {
        private const val TAG = "BitmapProcessor"
    }
}
