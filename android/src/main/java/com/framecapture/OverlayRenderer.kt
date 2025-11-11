package com.framecapture

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.util.LruCache
import com.framecapture.models.OverlayConfig
import com.framecapture.models.OverlayPosition
import com.framecapture.models.TextStyle
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Renders overlays (text and images) on captured frame bitmaps
 * Uses caching for performance optimization
 */
class OverlayRenderer(
    private val context: Context,
    private val eventEmitter: ((String, com.facebook.react.bridge.WritableMap?) -> Unit)? = null,
    cacheSize: Int = Constants.DEFAULT_OVERLAY_IMAGE_CACHE_SIZE
) {

    companion object {
        private const val TAG = "OverlayRenderer"
    }

    // Image cache to avoid repeated file I/O
    private var imageCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    /**
     * Updates the cache size (recreates the cache)
     */
    fun updateCacheSize(newSize: Int) {
        clearCaches()
        imageCache = object : LruCache<String, Bitmap>(newSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount
            }
        }
    }

    // Paint object pool for text rendering
    private val textPaintCache = mutableMapOf<String, Paint>()

    /**
     * Renders all overlays on the bitmap
     *
     * @param bitmap The bitmap to draw overlays on (modified in-place)
     * @param overlays List of overlay configurations
     * @param frameNumber Current frame number for variable substitution
     * @param sessionId Current session ID for variable substitution
     */
    fun renderOverlays(
        bitmap: Bitmap,
        overlays: List<OverlayConfig>,
        frameNumber: Int,
        sessionId: String
    ) {
        if (overlays.isEmpty()) return

        val canvas = Canvas(bitmap)

        overlays.forEachIndexed { index, overlay ->
            try {
                when (overlay) {
                    is OverlayConfig.Text -> {
                        // Validate text overlay has content
                        if (overlay.content.isEmpty()) {
                            return@forEachIndexed
                        }
                        renderTextOverlay(canvas, overlay, frameNumber, sessionId)
                    }
                    is OverlayConfig.Image -> {
                        // Validate image overlay has source
                        if (overlay.source.isEmpty()) {
                            return@forEachIndexed
                        }
                        renderImageOverlay(canvas, overlay, bitmap.width, bitmap.height)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to render overlay at index $index (type: ${overlay.type}): ${e.message}", e)
                emitOverlayError(index, overlay.type, e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Substitutes template variables in text content
     */
    private fun substituteVariables(content: String, frameNumber: Int, sessionId: String): String {
        var result = content

        // {frameNumber}
        result = result.replace(Constants.TEMPLATE_VAR_FRAME_NUMBER, frameNumber.toString())

        // {sessionId}
        result = result.replace(Constants.TEMPLATE_VAR_SESSION_ID, sessionId)

        // {timestamp}
        val timestamp = SimpleDateFormat(Constants.OVERLAY_TIMESTAMP_FORMAT, Locale.US).apply {
            timeZone = TimeZone.getTimeZone(Constants.OVERLAY_TIMESTAMP_TIMEZONE)
        }.format(Date())
        result = result.replace(Constants.TEMPLATE_VAR_TIMESTAMP, timestamp)

        return result
    }

    /**
     * Calculates absolute pixel position from OverlayPosition
     */
    private fun calculatePosition(
        position: OverlayPosition,
        canvasWidth: Int,
        canvasHeight: Int,
        overlayWidth: Int,
        overlayHeight: Int
    ): Pair<Int, Int> {
        return when (position) {
            is OverlayPosition.Preset -> {
                val padding = Constants.OVERLAY_DEFAULT_PADDING
                when (position.value) {
                    Constants.POSITION_TOP_LEFT -> padding to padding
                    Constants.POSITION_TOP_RIGHT -> (canvasWidth - overlayWidth - padding) to padding
                    Constants.POSITION_BOTTOM_LEFT -> padding to (canvasHeight - overlayHeight - padding)
                    Constants.POSITION_BOTTOM_RIGHT -> (canvasWidth - overlayWidth - padding) to (canvasHeight - overlayHeight - padding)
                    Constants.POSITION_CENTER -> ((canvasWidth - overlayWidth) / 2) to ((canvasHeight - overlayHeight) / 2)
                    else -> padding to padding
                }
            }
            is OverlayPosition.Coordinates -> {
                val x = if (position.unit == Constants.POSITION_UNIT_PERCENTAGE) {
                    (position.x * canvasWidth).toInt()
                } else {
                    position.x.toInt()
                }

                val y = if (position.unit == Constants.POSITION_UNIT_PERCENTAGE) {
                    (position.y * canvasHeight).toInt()
                } else {
                    position.y.toInt()
                }

                // Clamp to canvas bounds
                val clampedX = x.coerceIn(0, canvasWidth - overlayWidth)
                val clampedY = y.coerceIn(0, canvasHeight - overlayHeight)

                clampedX to clampedY
            }
        }
    }

    /**
     * Renders a text overlay on the canvas
     */
    private fun renderTextOverlay(
        canvas: Canvas,
        overlay: OverlayConfig.Text,
        frameNumber: Int,
        sessionId: String
    ) {
        try {
            // Substitute template variables
            val text = substituteVariables(overlay.content, frameNumber, sessionId)

            // Get or create paint object
            val paint = getTextPaint(overlay.style)

            // Measure text bounds
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)

            val textWidth = bounds.width()
            val textHeight = bounds.height()

            // Calculate position
            val (x, y) = calculatePosition(
                overlay.position,
                canvas.width,
                canvas.height,
                textWidth + overlay.style.padding * 2,
                textHeight + overlay.style.padding * 2
            )

            // Draw background if specified
            if (overlay.style.backgroundColor.isNotEmpty()) {
                val parsedColor = parseColor(overlay.style.backgroundColor)

                val bgPaint = Paint().apply {
                    color = parsedColor
                    style = Paint.Style.FILL
                }

                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    x + textWidth + overlay.style.padding * 2f,
                    y + textHeight + overlay.style.padding * 2f,
                    bgPaint
                )
            }

            // Draw text
            canvas.drawText(
                text,
                x + overlay.style.padding.toFloat(),
                y + textHeight + overlay.style.padding.toFloat(),
                paint
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering text overlay: ${e.message}", e)
            throw e
        }
    }

    /**
     * Renders an image overlay on the canvas
     */
    private fun renderImageOverlay(
        canvas: Canvas,
        overlay: OverlayConfig.Image,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        try {
            // Load image (from cache or file)
            val image = loadImage(overlay.source)
                ?: throw IllegalArgumentException("Failed to load image from source: ${overlay.source}")

            // Determine image size
            val (width, height) = overlay.size?.let { it.width to it.height }
                ?: (image.width to image.height)

            // Calculate position
            val (x, y) = calculatePosition(
                overlay.position,
                canvasWidth,
                canvasHeight,
                width,
                height
            )

            // Create paint with opacity
            val paint = Paint().apply {
                alpha = (overlay.opacity * 255).toInt()
            }

            // Scale image if needed
            val scaledImage = if (overlay.size != null) {
                Bitmap.createScaledBitmap(image, width, height, true)
            } else {
                image
            }

            // Draw image
            canvas.drawBitmap(scaledImage, x.toFloat(), y.toFloat(), paint)

            // Clean up scaled bitmap if created
            if (scaledImage != image) {
                scaledImage.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering image overlay from ${overlay.source}: ${e.message}", e)
            throw e
        }
    }

    /**
     * Gets or creates a Paint object for text rendering
     */
    private fun getTextPaint(style: TextStyle): Paint {
        val cacheKey = "${style.fontSize}_${style.color}_${style.fontWeight}_${style.textAlign}"

        return textPaintCache.getOrPut(cacheKey) {
            Paint().apply {
                color = parseColor(style.color)
                textSize = style.fontSize.toFloat()
                isAntiAlias = true
                isFakeBoldText = style.fontWeight == Constants.TEXT_WEIGHT_BOLD
                textAlign = when (style.textAlign) {
                    Constants.TEXT_ALIGN_CENTER -> Paint.Align.CENTER
                    Constants.TEXT_ALIGN_RIGHT -> Paint.Align.RIGHT
                    else -> Paint.Align.LEFT
                }
            }
        }
    }

    /**
     * Loads an image from drawable resource, file, or cache
     *
     * Supports:
     * - Drawable resource names: "logo", "ic_watermark" (looks in app's drawable folder)
     * - File URIs: "file:///path/to/image.png"
     * - Content URIs: "content://media/external/images/media/123"
     */
    private fun loadImage(source: String): Bitmap? {
        // Check cache first
        imageCache.get(source)?.let { return it }

        return try {
            val bitmap = when {
                // Check if it's a drawable resource name (no scheme, just a name)
                !source.contains("://") && !source.startsWith("/") -> {
                    loadImageFromDrawable(source)
                }
                // File or content URI
                else -> {
                    loadImageFromUri(source)
                }
            }

            // Cache the loaded image
            bitmap?.let { imageCache.put(source, it) }

            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads an image from drawable resources by name
     */
    private fun loadImageFromDrawable(resourceName: String): Bitmap? {
        return try {
            val resourceId = context.resources.getIdentifier(
                resourceName,
                Constants.RESOURCE_TYPE_DRAWABLE,
                context.packageName
            )

            if (resourceId == 0) {
                return null
            }

            BitmapFactory.decodeResource(context.resources, resourceId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads an image from file or content URI
     */
    private fun loadImageFromUri(source: String): Bitmap? {
        return try {
            val uri = Uri.parse(source)
            val path = when (uri.scheme) {
                Constants.URI_SCHEME_FILE -> uri.path
                Constants.URI_SCHEME_CONTENT -> uri.toString()
                else -> source
            }

            if (path != null && File(path).exists()) {
                BitmapFactory.decodeFile(path)
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses hex color string to Android Color int
     * Supports both #RRGGBB and #RRGGBBAA formats
     */
    private fun parseColor(colorString: String): Int {
        return try {
            var color = colorString.trim()

            // Handle #RRGGBBAA format (8 chars including #)
            if (color.length == 9 && color.startsWith("#")) {
                // Extract components
                val r = color.substring(1, 3).toInt(16)
                val g = color.substring(3, 5).toInt(16)
                val b = color.substring(5, 7).toInt(16)
                val a = color.substring(7, 9).toInt(16)

                // Construct ARGB color
                return Color.argb(a, r, g, b)
            }

            // For other formats, use standard parser
            Color.parseColor(color)
        } catch (e: Exception) {
            Color.WHITE
        }
    }

    /**
     * Emits overlay error event to JavaScript
     */
    private fun emitOverlayError(overlayIndex: Int, overlayType: String, errorMessage: String) {
        try {
            val params = com.facebook.react.bridge.Arguments.createMap().apply {
                putInt("overlayIndex", overlayIndex)
                putString("overlayType", overlayType)
                putString("message", errorMessage)
            }
            eventEmitter?.invoke(Constants.EVENT_OVERLAY_ERROR, params)
        } catch (e: Exception) {
            // Silently fail - event emission is not critical
        }
    }

    /**
     * Clears all caches
     */
    fun clearCaches() {
        imageCache.evictAll()
        textPaintCache.clear()
    }
}
