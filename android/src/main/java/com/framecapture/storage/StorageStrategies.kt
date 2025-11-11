package com.framecapture.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.framecapture.Constants
import com.framecapture.models.FrameInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Implements different storage strategies for saving captured frames
 *
 * Provides multiple storage options with appropriate Android API handling:
 * - Temporary storage: Cache directory (auto-cleaned on app restart)
 * - App-specific storage: No permissions needed, cleaned on uninstall
 * - Public storage: MediaStore API (Android 10+) for gallery visibility
 * - Custom directory: User-specified path with validation
 *
 * Handles bitmap compression, file I/O, and error recovery (cleanup on failure).
 */
class StorageStrategies(private val context: Context) {

    companion object {
        private const val TAG = "StorageStrategies"
    }

    /**
     * Returns the app-specific pictures directory (no permissions needed)
     */
    fun getAppSpecificDirectory(): File {
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir // Fallback to internal storage

        // Create directory if it doesn't exist
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }

        return picturesDir
    }

    /**
     * Returns the temporary cache directory for frames when saveFrames is false
     */
    fun getTempDirectory(): File {
        val tempDir = File(context.cacheDir, Constants.TEMP_FRAMES_DIRECTORY)
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return tempDir
    }

    /**
     * Returns the MIME type for the given format
     */
    fun getMimeType(format: String): String {
        return when (format.lowercase()) {
            Constants.FORMAT_EXTENSION_PNG -> Constants.MIME_TYPE_PNG
            else -> Constants.MIME_TYPE_JPEG
        }
    }

    /**
     * Returns the Bitmap.CompressFormat for the given format string
     */
    fun getCompressFormat(format: String): Bitmap.CompressFormat {
        return when (format.lowercase()) {
            Constants.FORMAT_EXTENSION_PNG -> Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG
        }
    }

    /**
     * Saves frame to temporary cache directory (when saveFrames is false)
     */
    fun saveToTempDirectory(
        bitmap: Bitmap,
        sessionId: String,
        filename: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): FrameInfo {
        // Create session-specific temp directory
        val tempBaseDir = getTempDirectory()
        val sessionTempDir = File(tempBaseDir, sessionId)

        if (!sessionTempDir.exists()) {
            if (!sessionTempDir.mkdirs()) {
                throw IOException("Failed to create temp directory: ${sessionTempDir.absolutePath}")
            }
        }

        val file = File(sessionTempDir, filename)
        return saveToFile(file, bitmap, format, quality)
    }

    /**
     * Saves frame to a custom directory specified by the user
     */
    fun saveToCustomDirectory(
        bitmap: Bitmap,
        customPath: String,
        filename: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): FrameInfo {
        val directory = File(customPath)

        // Validate directory
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw IOException("Failed to create custom directory: $customPath")
            }
        }

        if (!directory.isDirectory) {
            throw IOException("Custom path is not a directory: $customPath")
        }

        if (!directory.canWrite()) {
            throw IOException("No write permission for custom directory: $customPath")
        }

        val file = File(directory, filename)
        return saveToFile(file, bitmap, format, quality)
    }

    /**
     * Saves frame to MediaStore (API 29+) for gallery visibility with session folder
     */
    fun saveToMediaStore(
        bitmap: Bitmap,
        sessionId: String,
        filename: String,
        formatString: String,
        quality: Int
    ): FrameInfo {
        val mimeType = getMimeType(formatString)
        val format = getCompressFormat(formatString)

        // Create session-specific subfolder in Pictures
        val relativePath = "${Environment.DIRECTORY_PICTURES}/$sessionId"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IOException("Failed to create MediaStore entry")

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                if (!bitmap.compress(format, quality, outputStream)) {
                    throw IOException("Failed to compress bitmap to MediaStore")
                }
            } ?: throw IOException("Failed to open output stream for MediaStore")

            // Mark as not pending
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)

            // Get file size and actual file path from MediaStore
            val fileSize = getMediaStoreFileSize(uri)
            val actualFilePath = getMediaStoreFilePath(uri) ?: uri.toString()

            return FrameInfo(
                filePath = actualFilePath,
                fileSize = fileSize,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // Clean up on error
            context.contentResolver.delete(uri, null, null)
            throw IOException("Failed to save frame to MediaStore: ${e.message}", e)
        }
    }

    /**
     * Saves frame to app-specific directory with session folder (default, no permissions needed)
     */
    fun saveToAppSpecificDirectory(
        bitmap: Bitmap,
        sessionId: String,
        filename: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): FrameInfo {
        // Create session-specific directory
        val baseDirectory = getAppSpecificDirectory()
        val sessionDirectory = File(baseDirectory, sessionId)

        if (!sessionDirectory.exists()) {
            if (!sessionDirectory.mkdirs()) {
                throw IOException("Failed to create session directory: ${sessionDirectory.absolutePath}")
            }
        }

        val file = File(sessionDirectory, filename)
        return saveToFile(file, bitmap, format, quality)
    }

    /**
     * Saves bitmap to a file
     */
    fun saveToFile(
        file: File,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        quality: Int
    ): FrameInfo {
        try {
            FileOutputStream(file).use { outputStream ->
                if (!bitmap.compress(format, quality, outputStream)) {
                    throw IOException("Failed to compress bitmap to file")
                }
                outputStream.flush()
            }

            return FrameInfo(
                filePath = file.absolutePath,
                fileSize = file.length(),
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // Clean up partial file on error
            if (file.exists()) {
                file.delete()
            }
            throw IOException("Failed to save frame to file: ${e.message}", e)
        }
    }

    /**
     * Gets the file size from MediaStore URI (approximate)
     */
    private fun getMediaStoreFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                    if (sizeIndex >= 0) {
                        cursor.getLong(sizeIndex)
                    } else {
                        0L
                    }
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Gets the actual file path from MediaStore URI
     * Returns the absolute file path that can be used with file:// scheme
     */
    private fun getMediaStoreFilePath(uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DATA),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (dataIndex >= 0) {
                        cursor.getString(dataIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cleans up all temporary frame files
     * Called on app startup and can be called manually
     */
    fun cleanupAllTempFiles() {
        try {
            val tempDir = File(context.cacheDir, Constants.TEMP_FRAMES_DIRECTORY)
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup temp files: ${e.message}", e)
        }
    }
}
