package com.framecapture

import android.graphics.Bitmap
import android.os.Build
import android.os.StatFs
import android.util.Log
import com.framecapture.models.CaptureOptions
import com.framecapture.models.FrameInfo
import com.framecapture.storage.StorageStrategies
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages storage operations for captured frames
 *
 * Coordinates frame storage across different Android versions and storage locations.
 * Delegates actual storage operations to StorageStrategies for different scenarios:
 * - Temporary storage (when saveFrames is false)
 * - App-specific storage (default, no permissions needed)
 * - Public storage via MediaStore (Android 10+)
 * - Custom directory storage
 *
 * Also handles storage space monitoring and warning events.
 */
class StorageManager(
    private val context: android.content.Context,
    private val eventEmitter: ((String, com.facebook.react.bridge.WritableMap?) -> Unit)? = null
) {

    // Storage strategies handler
    private val storageStrategies = StorageStrategies(context)

    init {
        // Clean up temp files from previous sessions on startup
        cleanupAllTempFiles()
    }

    /**
     * Returns the app-specific pictures directory
     *
     * This directory doesn't require storage permissions and is automatically
     * cleaned when the app is uninstalled.
     *
     * @return File pointing to app-specific pictures directory
     */
    fun getAppSpecificDirectory(): File {
        return storageStrategies.getAppSpecificDirectory()
    }

    /**
     * Cleans up all temporary frame files
     *
     * Automatically called on app startup to remove temp files from previous sessions.
     * Can also be called manually via cleanupTempFrames() API.
     */
    fun cleanupAllTempFiles() {
        storageStrategies.cleanupAllTempFiles()
    }

    /**
     * Generates a unique filename for a captured frame
     *
     * Format: <prefix><sessionId>_<frameNumber>_<timestamp>.<format>
     * Example: capture_abc123_00042_20231215_143022_456.jpg
     *
     * @param sessionId Unique session identifier
     * @param frameNumber Frame number (zero-padded based on config)
     * @param format Image format ("png" or "jpg")
     * @param fileNamingConfig File naming configuration
     * @return Generated filename
     */
    fun generateFilename(
        sessionId: String,
        frameNumber: Int,
        format: String,
        fileNamingConfig: com.framecapture.models.FileNamingConfig = com.framecapture.models.FileNamingConfig()
    ): String {
        val timestamp = SimpleDateFormat(fileNamingConfig.dateFormat, Locale.US).format(Date())
        val extension = when (format.lowercase()) {
            Constants.FORMAT_EXTENSION_PNG -> Constants.FORMAT_EXTENSION_PNG
            else -> Constants.FORMAT_EXTENSION_JPEG
        }
        val paddingFormat = "%0${fileNamingConfig.framePadding}d"
        return "${fileNamingConfig.prefix}${sessionId}_${String.format(paddingFormat, frameNumber)}_${timestamp}.${extension}"
    }



    /**
     * Saves a captured frame to storage
     *
     * Storage location is determined by options in this priority order:
     * 1. Temporary storage (if saveFrames is false)
     * 2. Custom directory (if outputDirectory is specified)
     * 3. Public storage via MediaStore (if storageLocation is "public" and Android 10+)
     * 4. App-specific directory (default)
     *
     * @param bitmap The captured frame as a Bitmap
     * @param sessionId Unique session identifier
     * @param frameNumber Frame number in the session
     * @param options Capture options (format, quality, storage location)
     * @return FrameInfo containing file path, size, and timestamp
     * @throws IOException if the save operation fails
     */
    fun saveFrame(
        bitmap: Bitmap,
        sessionId: String,
        frameNumber: Int,
        options: CaptureOptions
    ): FrameInfo {
        val filename = generateFilename(sessionId, frameNumber, options.format, options.advanced.fileNaming)
        val format = storageStrategies.getCompressFormat(options.format)
        val quality = options.quality

        // If saveFrames is false, save to temp directory
        if (!options.saveFrames) {
            return storageStrategies.saveToTempDirectory(bitmap, sessionId, filename, format, quality)
        }

        return when {
            // 1. Custom directory takes precedence
            options.outputDirectory != null -> {
                storageStrategies.saveToCustomDirectory(bitmap, options.outputDirectory, filename, format, quality)
            }

            // 2. Public storage request
            options.storageLocation == Constants.STORAGE_LOCATION_PUBLIC -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+: Use MediaStore for public Pictures folder
                    storageStrategies.saveToMediaStore(bitmap, sessionId, filename, options.format, quality)
                } else {
                    // Android 9 and below: Fall back to app-specific storage
                    storageStrategies.saveToAppSpecificDirectory(bitmap, sessionId, filename, format, quality)
                }
            }

            // 3. Default: App-specific directory (no permissions needed)
            else -> {
                storageStrategies.saveToAppSpecificDirectory(bitmap, sessionId, filename, format, quality)
            }
        }
    }

    /**
     * Checks available storage space in bytes
     *
     * Uses the app-specific directory to determine available space.
     * Handles API level differences for StatFs methods.
     *
     * @return Available storage space in bytes, or 0 if check fails
     */
    fun checkStorageSpace(): Long {
        return try {
            val directory = getAppSpecificDirectory()
            val stat = StatFs(directory.path)

            // Use API level-appropriate method
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.availableBlocksLong * stat.blockSizeLong
            } else {
                @Suppress("DEPRECATION")
                stat.availableBlocks.toLong() * stat.blockSize.toLong()
            }
        } catch (e: Exception) {
            Log.e(Constants.MODULE_NAME, "Failed to check storage space: ${e.message}", e)
            0L
        }
    }

    /**
     * Checks if sufficient storage is available
     *
     * Compares available space against the configured warning threshold.
     * Emits a warning event to JavaScript if below threshold.
     *
     * @param threshold Storage warning threshold in bytes (0 to disable warnings)
     * @return true if storage is sufficient, false if below threshold
     */
    fun isStorageAvailable(threshold: Long = Constants.DEFAULT_STORAGE_WARNING_THRESHOLD): Boolean {
        // If threshold is 0, warnings are disabled
        if (threshold == 0L) {
            return true
        }

        val availableSpace = checkStorageSpace()
        val isAvailable = availableSpace >= threshold

        // Debug logging

        // Emit warning if below threshold
        if (!isAvailable && eventEmitter != null) {
            emitStorageWarning(availableSpace, threshold)
        } else if (!isAvailable) {
            Log.w(Constants.MODULE_NAME, "Storage below threshold but eventEmitter is null!")
        }

        return isAvailable
    }

    /**
     * Emits a storage warning event to JavaScript
     *
     * Sends available space and threshold values to allow the app to
     * handle low storage situations (e.g., stop capture, notify user).
     *
     * @param availableSpace Current available storage in bytes
     * @param threshold The configured warning threshold
     */
    private fun emitStorageWarning(availableSpace: Long, threshold: Long) {
        try {
            val params = com.facebook.react.bridge.Arguments.createMap().apply {
                putDouble(Constants.EVENT_KEY_AVAILABLE_SPACE, availableSpace.toDouble())
                putDouble(Constants.EVENT_KEY_THRESHOLD, threshold.toDouble())
            }
            eventEmitter?.invoke(Constants.EVENT_STORAGE_WARNING, params)
        } catch (e: Exception) {
            // Silently fail - event emission is not critical
        }
    }
}
