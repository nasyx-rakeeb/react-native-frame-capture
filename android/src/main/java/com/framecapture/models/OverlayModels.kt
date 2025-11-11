package com.framecapture.models

import com.facebook.react.bridge.ReadableMap

/**
 * Overlay position - either preset or custom coordinates
 */
sealed class OverlayPosition {
    data class Preset(val value: String) : OverlayPosition()
    data class Coordinates(val x: Float, val y: Float, val unit: String = com.framecapture.Constants.POSITION_UNIT_PIXELS) : OverlayPosition()

    companion object {
        fun fromReadableMap(map: ReadableMap?): OverlayPosition? {
            if (map == null) return null

            return try {
                val preset = map.getString("preset")
                if (preset != null) Preset(preset) else null
            } catch (e: Exception) {
                Coordinates(
                    x = if (map.hasKey("x")) map.getDouble("x").toFloat() else 0f,
                    y = if (map.hasKey("y")) map.getDouble("y").toFloat() else 0f,
                    unit = if (map.hasKey("unit")) map.getString("unit") ?: com.framecapture.Constants.POSITION_UNIT_PIXELS else com.framecapture.Constants.POSITION_UNIT_PIXELS
                )
            }
        }

        fun fromString(preset: String): OverlayPosition {
            return Preset(preset)
        }
    }
}

/**
 * Text style configuration
 */
data class TextStyle(
    val fontSize: Int = com.framecapture.Constants.TEXT_DEFAULT_FONT_SIZE,
    val color: String = com.framecapture.Constants.TEXT_DEFAULT_COLOR,
    val backgroundColor: String = com.framecapture.Constants.TEXT_DEFAULT_BACKGROUND_COLOR,
    val padding: Int = com.framecapture.Constants.TEXT_DEFAULT_PADDING,
    val fontWeight: String = com.framecapture.Constants.TEXT_WEIGHT_NORMAL,
    val textAlign: String = com.framecapture.Constants.TEXT_ALIGN_LEFT
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): TextStyle {
            if (map == null) return TextStyle()

            val bgColor = map.getStringOrDefault("backgroundColor", com.framecapture.Constants.TEXT_DEFAULT_BACKGROUND_COLOR)

            return TextStyle(
                fontSize = map.getIntOrDefault("fontSize", com.framecapture.Constants.TEXT_DEFAULT_FONT_SIZE),
                color = map.getStringOrDefault("color", com.framecapture.Constants.TEXT_DEFAULT_COLOR),
                backgroundColor = bgColor,
                padding = map.getIntOrDefault("padding", com.framecapture.Constants.TEXT_DEFAULT_PADDING),
                fontWeight = map.getStringOrDefault("fontWeight", com.framecapture.Constants.TEXT_WEIGHT_NORMAL),
                textAlign = map.getStringOrDefault("textAlign", com.framecapture.Constants.TEXT_ALIGN_LEFT)
            )
        }
    }
}

/**
 * Image size configuration
 */
data class ImageSize(
    val width: Int,
    val height: Int
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): ImageSize? {
            if (map == null) return null

            return ImageSize(
                width = if (map.hasKey("width")) map.getInt("width") else return null,
                height = if (map.hasKey("height")) map.getInt("height") else return null
            )
        }
    }
}

/**
 * Base overlay configuration
 */
sealed class OverlayConfig {
    abstract val type: String
    abstract val position: OverlayPosition

    /**
     * Text overlay configuration
     */
    data class Text(
        override val type: String = com.framecapture.Constants.OVERLAY_TYPE_TEXT,
        val content: String,
        override val position: OverlayPosition,
        val style: TextStyle = TextStyle()
    ) : OverlayConfig()

    /**
     * Image overlay configuration
     */
    data class Image(
        override val type: String = com.framecapture.Constants.OVERLAY_TYPE_IMAGE,
        val source: String,
        override val position: OverlayPosition,
        val size: ImageSize? = null,
        val opacity: Float = com.framecapture.Constants.IMAGE_DEFAULT_OPACITY
    ) : OverlayConfig()

    companion object {
        fun fromReadableMap(map: ReadableMap): OverlayConfig? {
            val type = map.getStringOrNull("type") ?: return null

            return when (type) {
                com.framecapture.Constants.OVERLAY_TYPE_TEXT -> {
                    val content = map.getStringOrNull("content") ?: return null
                    val position = parsePositionFromMap(map) ?: return null
                    val style = TextStyle.fromReadableMap(map.getMap("style"))

                    Text(content = content, position = position, style = style)
                }
                com.framecapture.Constants.OVERLAY_TYPE_IMAGE -> {
                    val source = map.getStringOrNull("source") ?: return null
                    val position = parsePositionFromMap(map) ?: return null
                    val size = ImageSize.fromReadableMap(map.getMap("size"))
                    val opacity = if (map.hasKey("opacity")) map.getDouble("opacity").toFloat() else com.framecapture.Constants.IMAGE_DEFAULT_OPACITY

                    Image(source = source, position = position, size = size, opacity = opacity)
                }
                else -> null
            }
        }

        private fun parsePositionFromMap(map: ReadableMap): OverlayPosition? {
            if (!map.hasKey("position")) return null

            return try {
                val positionString = map.getString("position")
                if (positionString != null) {
                    OverlayPosition.fromString(positionString)
                } else {
                    null
                }
            } catch (e: Exception) {
                try {
                    val positionMap = map.getMap("position")
                    OverlayPosition.fromReadableMap(positionMap)
                } catch (e2: Exception) {
                    null
                }
            }
        }
    }
}
