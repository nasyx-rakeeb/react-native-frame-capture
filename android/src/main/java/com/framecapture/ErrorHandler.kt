package com.framecapture

import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.framecapture.models.CaptureState
import com.framecapture.models.ErrorCode
import java.io.IOException

/**
 * Handles error classification, formatting, and reporting
 *
 * Provides consistent error handling across the module by:
 * - Classifying exceptions into appropriate ErrorCode values
 * - Formatting error details for JavaScript consumption
 * - Coordinating error emission and cleanup
 *
 * Uses dependency injection for storage space and capture state checks
 * to avoid tight coupling with other components.
 */
class ErrorHandler(
    private val getStorageSpace: () -> Long,
    private val getCaptureState: () -> CaptureState
) {

    /**
     * Classifies exception into error code, message, and details
     *
     * Maps common exceptions to appropriate ErrorCode values and provides
     * contextual details for debugging (e.g., available storage, current state).
     *
     * @param error The exception to classify
     * @return Triple of (ErrorCode, error message, optional details map)
     */
    fun classifyError(error: Exception): Triple<ErrorCode, String, Map<String, Any>?> {
        return when (error) {
            is SecurityException -> Triple(
                ErrorCode.PERMISSION_DENIED,
                "Required permission not granted: ${error.message}",
                mapOf(Constants.ERROR_DETAIL_PERMISSION to "MEDIA_PROJECTION")
            )

            is IllegalStateException -> Triple(
                ErrorCode.ALREADY_CAPTURING,
                "Capture session is already active: ${error.message}",
                mapOf(Constants.ERROR_DETAIL_CURRENT_STATE to getCaptureState().value)
            )

            is IllegalArgumentException -> Triple(
                ErrorCode.INVALID_OPTIONS,
                "Invalid configuration: ${error.message}",
                null
            )

            is IOException -> Triple(
                ErrorCode.STORAGE_ERROR,
                "Failed to save frame: ${error.message}",
                mapOf(Constants.ERROR_DETAIL_AVAILABLE_SPACE to getStorageSpace())
            )

            is OutOfMemoryError -> Triple(
                ErrorCode.SYSTEM_ERROR,
                "Out of memory during capture",
                mapOf(Constants.ERROR_DETAIL_HEAP_SIZE to Runtime.getRuntime().maxMemory())
            )

            else -> Triple(
                ErrorCode.SYSTEM_ERROR,
                "Unexpected error: ${error.message ?: "Unknown error"}",
                null
            )
        }
    }

    /**
     * Creates error map for promise rejection and event emission
     *
     * Formats error information into a WritableMap that can be sent to JavaScript.
     * Handles various data types in the details map (String, Int, Long, Boolean, List).
     *
     * @param code Error code enum value
     * @param message Human-readable error message
     * @param details Optional map of additional error context
     * @return WritableMap formatted for React Native bridge
     */
    fun createErrorMap(
        code: ErrorCode,
        message: String,
        details: Map<String, Any>?
    ): WritableMap {
        return Arguments.createMap().apply {
            putString("code", code.value)
            putString("message", message)
            details?.let { detailsMap ->
                val detailsWritableMap = Arguments.createMap()
                detailsMap.forEach { (key, value) ->
                    when (value) {
                        is String -> detailsWritableMap.putString(key, value)
                        is Int -> detailsWritableMap.putInt(key, value)
                        is Double -> detailsWritableMap.putDouble(key, value)
                        is Long -> detailsWritableMap.putDouble(key, value.toDouble())
                        is Boolean -> detailsWritableMap.putBoolean(key, value)
                        is List<*> -> {
                            val array = Arguments.createArray()
                            value.forEach { item ->
                                when (item) {
                                    is String -> array.pushString(item)
                                    else -> array.pushString(item.toString())
                                }
                            }
                            detailsWritableMap.putArray(key, array)
                        }
                        else -> detailsWritableMap.putString(key, value.toString())
                    }
                }
                putMap("details", detailsWritableMap)
            }
        }
    }

    /**
     * Handles error with logging, classification, and promise rejection
     *
     * Orchestrates the complete error handling flow:
     * 1. Classifies the error
     * 2. Logs for debugging
     * 3. Performs cleanup via callback
     * 4. Emits error event to JavaScript
     * 5. Rejects promise if provided
     *
     * @param error The exception that occurred
     * @param promise Optional promise to reject
     * @param onEmitError Callback to emit error event
     * @param onCleanup Callback to perform cleanup
     */
    fun handleError(
        error: Exception,
        promise: Promise?,
        onEmitError: (ErrorCode, String, Map<String, Any>?) -> Unit,
        onCleanup: () -> Unit
    ) {
        val (code, message, details) = classifyError(error)

        // Log for debugging
        Log.e(TAG, "Capture error: $message", error)

        // Clean up resources on error
        try {
            onCleanup()
        } catch (cleanupError: Exception) {
            Log.e(TAG, "Error during cleanup: ${cleanupError.message}", cleanupError)
        }

        // Emit error event
        onEmitError(code, message, details)

        // Reject promise if provided
        promise?.reject(code.value, message, createErrorMap(code, message, details))
    }

    companion object {
        private const val TAG = "ErrorHandler"
    }
}
