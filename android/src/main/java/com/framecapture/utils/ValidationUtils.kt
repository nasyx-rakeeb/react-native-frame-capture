package com.framecapture.utils

import com.framecapture.models.CaptureOptions
import com.framecapture.models.OverlayConfig
import com.framecapture.models.OverlayPosition
import java.io.File

/**
 * Sealed class representing validation result
 *
 * Either Success (validation passed) or Error (with list of error messages)
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val messages: List<String>) : ValidationResult()
}

/**
 * Validation utilities for capture options
 *
 * Provides comprehensive validation for all capture configuration options:
 * - Capture settings (interval, quality, format, resolution)
 * - Storage configuration (output directory, permissions)
 * - Overlay configuration (text, images, positions, styling)
 * - Color format validation (hex colors)
 *
 * Returns detailed error messages for debugging and user feedback.
 */
object ValidationUtils {

    // Import validation constants from Constants object
    // Support both "jpeg" and "jpg" for compatibility
    private val VALID_FORMATS = setOf(com.framecapture.Constants.FORMAT_EXTENSION_PNG, com.framecapture.Constants.FORMAT_EXTENSION_JPEG, "jpeg")
    private val VALID_OVERLAY_TYPES = setOf(com.framecapture.Constants.OVERLAY_TYPE_TEXT, com.framecapture.Constants.OVERLAY_TYPE_IMAGE)
    private val VALID_POSITION_PRESETS = setOf(
        com.framecapture.Constants.POSITION_TOP_LEFT,
        com.framecapture.Constants.POSITION_TOP_RIGHT,
        com.framecapture.Constants.POSITION_BOTTOM_LEFT,
        com.framecapture.Constants.POSITION_BOTTOM_RIGHT,
        com.framecapture.Constants.POSITION_CENTER
    )

    /**
     * Validates capture options comprehensively
     *
     * Checks all configuration values against constraints and returns detailed
     * error messages for any validation failures.
     *
     * @param options The CaptureOptions to validate
     * @return ValidationResult.Success if valid, ValidationResult.Error with messages if invalid
     */
    fun validateOptions(options: CaptureOptions): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate interval
        if (options.interval < com.framecapture.Constants.MIN_INTERVAL || options.interval > com.framecapture.Constants.MAX_INTERVAL) {
            errors.add(
                "interval must be between ${com.framecapture.Constants.MIN_INTERVAL} and ${com.framecapture.Constants.MAX_INTERVAL} milliseconds. " +
                "Provided: ${options.interval}ms"
            )
        }

        // Validate quality
        if (options.quality < com.framecapture.Constants.MIN_QUALITY || options.quality > com.framecapture.Constants.MAX_QUALITY) {
            errors.add(
                "quality must be between ${com.framecapture.Constants.MIN_QUALITY} and ${com.framecapture.Constants.MAX_QUALITY}. " +
                "Provided: ${options.quality}"
            )
        }

        // Validate format
        if (options.format !in VALID_FORMATS) {
            errors.add(
                "format must be one of: ${VALID_FORMATS.joinToString(", ")}. " +
                "Provided: '${options.format}'"
            )
        }

        // Validate output directory if provided
        options.outputDirectory?.let { directory ->
            val validationError = validateOutputDirectory(directory)
            if (validationError != null) {
                errors.add(validationError)
            }
        }

        // Validate scale resolution if provided
        options.scaleResolution?.let { scale ->
            if (scale < com.framecapture.Constants.MIN_SCALE_RESOLUTION || scale > com.framecapture.Constants.MAX_SCALE_RESOLUTION) {
                errors.add(
                    "scaleResolution must be between ${com.framecapture.Constants.MIN_SCALE_RESOLUTION} and ${com.framecapture.Constants.MAX_SCALE_RESOLUTION}. " +
                    "Provided: $scale"
                )
            }
        }

        // Validate overlays if provided
        options.overlays?.let { overlays ->
            overlays.forEachIndexed { index, overlay ->
                val overlayErrors = validateOverlay(overlay, index)
                errors.addAll(overlayErrors)
            }
        }

        // Validate advanced config
        errors.addAll(validateAdvancedConfig(options.advanced))

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * Validates advanced configuration
     */
    private fun validateAdvancedConfig(advanced: com.framecapture.models.AdvancedConfig): List<String> {
        val errors = mutableListOf<String>()

        // Validate storage config
        if (advanced.storage.warningThreshold < 0) {
            errors.add("advanced.storage.warningThreshold must be non-negative. Provided: ${advanced.storage.warningThreshold}")
        }

        // Validate file naming config
        val fn = advanced.fileNaming

        if (fn.prefix.isEmpty()) {
            errors.add("advanced.fileNaming.prefix must not be empty")
        }

        if (fn.prefix.contains(Regex("[<>:\"/\\\\|?*]"))) {
            errors.add("advanced.fileNaming.prefix contains invalid filename characters. Provided: '${fn.prefix}'")
        }

        if (fn.dateFormat.isEmpty()) {
            errors.add("advanced.fileNaming.dateFormat must not be empty")
        }

        if (fn.framePadding < 1 || fn.framePadding > 10) {
            errors.add("advanced.fileNaming.framePadding must be between 1 and 10. Provided: ${fn.framePadding}")
        }

        // Validate performance config
        val perf = advanced.performance

        if (perf.overlayCacheSize < 0) {
            errors.add("advanced.performance.overlayCacheSize must be non-negative. Provided: ${perf.overlayCacheSize}")
        }

        if (perf.imageReaderBuffers < 1 || perf.imageReaderBuffers > 10) {
            errors.add("advanced.performance.imageReaderBuffers must be between 1 and 10. Provided: ${perf.imageReaderBuffers}")
        }

        if (perf.executorShutdownTimeout < 100 || perf.executorShutdownTimeout > 30000) {
            errors.add("advanced.performance.executorShutdownTimeout must be between 100 and 30000ms. Provided: ${perf.executorShutdownTimeout}")
        }

        if (perf.executorForcedShutdownTimeout < 100 || perf.executorForcedShutdownTimeout > 10000) {
            errors.add("advanced.performance.executorForcedShutdownTimeout must be between 100 and 10000ms. Provided: ${perf.executorForcedShutdownTimeout}")
        }

        return errors
    }

    /**
     * Validates a single overlay configuration
     *
     * @param overlay The overlay to validate
     * @param index The index of the overlay in the array (for error messages)
     * @return List of error messages (empty if valid)
     */
    private fun validateOverlay(overlay: OverlayConfig, index: Int): List<String> {
        val errors = mutableListOf<String>()
        val prefix = "overlays[$index]"

        // Validate overlay type
        if (overlay.type !in VALID_OVERLAY_TYPES) {
            errors.add(
                "$prefix: type must be one of: ${VALID_OVERLAY_TYPES.joinToString(", ")}. " +
                "Provided: '${overlay.type}'"
            )
        }

        // Validate based on overlay type
        when (overlay) {
            is OverlayConfig.Text -> {
                // Validate text content
                if (overlay.content.isBlank()) {
                    errors.add("$prefix: text overlay must have non-empty content")
                }

                // Validate text style colors
                val colorErrors = validateTextColors(overlay.style.color, overlay.style.backgroundColor, prefix)
                errors.addAll(colorErrors)
            }
            is OverlayConfig.Image -> {
                // Validate image source
                if (overlay.source.isBlank()) {
                    errors.add("$prefix: image overlay must have non-empty source")
                }

                // Validate opacity
                if (overlay.opacity < com.framecapture.Constants.MIN_OVERLAY_OPACITY || overlay.opacity > com.framecapture.Constants.MAX_OVERLAY_OPACITY) {
                    errors.add(
                        "$prefix: opacity must be between ${com.framecapture.Constants.MIN_OVERLAY_OPACITY} and ${com.framecapture.Constants.MAX_OVERLAY_OPACITY}. " +
                        "Provided: ${overlay.opacity}"
                    )
                }
            }
        }

        // Validate position
        val positionErrors = validatePosition(overlay.position, prefix)
        errors.addAll(positionErrors)

        return errors
    }

    /**
     * Validates overlay position
     *
     * @param position The position to validate
     * @param prefix Error message prefix
     * @return List of error messages (empty if valid)
     */
    private fun validatePosition(position: OverlayPosition, prefix: String): List<String> {
        val errors = mutableListOf<String>()

        when (position) {
            is OverlayPosition.Preset -> {
                if (position.value !in VALID_POSITION_PRESETS) {
                    errors.add(
                        "$prefix.position: preset must be one of: ${VALID_POSITION_PRESETS.joinToString(", ")}. " +
                        "Provided: '${position.value}'"
                    )
                }
            }
            is OverlayPosition.Coordinates -> {
                // Validate unit
                if (position.unit !in setOf(com.framecapture.Constants.POSITION_UNIT_PIXELS, com.framecapture.Constants.POSITION_UNIT_PERCENTAGE)) {
                    errors.add(
                        "$prefix.position: unit must be '${com.framecapture.Constants.POSITION_UNIT_PIXELS}' or '${com.framecapture.Constants.POSITION_UNIT_PERCENTAGE}'. " +
                        "Provided: '${position.unit}'"
                    )
                }

                // Validate percentage coordinates are in valid range
                if (position.unit == com.framecapture.Constants.POSITION_UNIT_PERCENTAGE) {
                    if (position.x < 0f || position.x > 1f) {
                        errors.add(
                            "$prefix.position: x coordinate with percentage unit must be between 0.0 and 1.0. " +
                            "Provided: ${position.x}"
                        )
                    }
                    if (position.y < 0f || position.y > 1f) {
                        errors.add(
                            "$prefix.position: y coordinate with percentage unit must be between 0.0 and 1.0. " +
                            "Provided: ${position.y}"
                        )
                    }
                }

                // Validate pixel coordinates are non-negative
                if (position.unit == com.framecapture.Constants.POSITION_UNIT_PIXELS) {
                    if (position.x < 0f) {
                        errors.add("$prefix.position: x coordinate cannot be negative. Provided: ${position.x}")
                    }
                    if (position.y < 0f) {
                        errors.add("$prefix.position: y coordinate cannot be negative. Provided: ${position.y}")
                    }
                }
            }
        }

        return errors
    }

    /**
     * Validates text overlay colors
     *
     * @param color Text color
     * @param backgroundColor Background color
     * @param prefix Error message prefix
     * @return List of error messages (empty if valid)
     */
    private fun validateTextColors(color: String, backgroundColor: String, prefix: String): List<String> {
        val errors = mutableListOf<String>()

        // Validate text color format
        if (!isValidHexColor(color)) {
            errors.add(
                "$prefix.style.color: must be a valid hex color (e.g., '#FFFFFF', '#FFF', '#AARRGGBB'). " +
                "Provided: '$color'"
            )
        }

        // Validate background color format
        if (backgroundColor.isNotEmpty() && !isValidHexColor(backgroundColor)) {
            errors.add(
                "$prefix.style.backgroundColor: must be a valid hex color (e.g., '#FFFFFF', '#FFF', '#AARRGGBB'). " +
                "Provided: '$backgroundColor'"
            )
        }

        return errors
    }

    /**
     * Checks if a string is a valid hex color format
     *
     * @param color The color string to validate
     * @return true if valid hex color, false otherwise
     */
    private fun isValidHexColor(color: String): Boolean {
        if (!color.startsWith("#")) return false

        val hex = color.substring(1)

        // Valid formats: #RGB, #RRGGBB, #AARRGGBB
        return when (hex.length) {
            com.framecapture.Constants.HEX_COLOR_LENGTH_SHORT,
            com.framecapture.Constants.HEX_COLOR_LENGTH_MEDIUM,
            com.framecapture.Constants.HEX_COLOR_LENGTH_LONG -> hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            else -> false
        }
    }

    /**
     * Validates output directory path
     *
     * @param directory The directory path to validate
     * @return Error message if invalid, null if valid
     */
    private fun validateOutputDirectory(directory: String): String? {
        if (directory.isBlank()) {
            return "outputDirectory cannot be empty or blank"
        }

        try {
            val file = File(directory)

            // Check if path is absolute
            if (!file.isAbsolute) {
                return "outputDirectory must be an absolute path. Provided: '$directory'"
            }

            // Check if directory exists and is writable (if it exists)
            if (file.exists()) {
                if (!file.isDirectory) {
                    return "outputDirectory must be a directory, not a file. Provided: '$directory'"
                }
                if (!file.canWrite()) {
                    return "outputDirectory is not writable. Provided: '$directory'"
                }
            }
        } catch (e: Exception) {
            return "outputDirectory path is invalid: ${e.message}"
        }

        return null
    }

    /**
     * Creates a user-friendly error message from validation result
     *
     * @param result The ValidationResult to format
     * @return Formatted error message or null if validation succeeded
     */
    fun formatValidationError(result: ValidationResult): String? {
        return when (result) {
            is ValidationResult.Success -> null
            is ValidationResult.Error -> {
                "Invalid capture options:\n" + result.messages.joinToString("\n") { "  - $it" }
            }
        }
    }
}
