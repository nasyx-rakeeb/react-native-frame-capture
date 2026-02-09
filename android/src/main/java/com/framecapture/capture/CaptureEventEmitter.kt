package com.framecapture.capture

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.framecapture.Constants
import com.framecapture.models.ErrorCode
import com.framecapture.models.FrameInfo

/**
 * Handles event emission for capture operations
 *
 * Formats and emits capture-related events to the JavaScript layer.
 * Provides type-safe event emission with proper error handling.
 *
 * Events emitted:
 * - onFrameCaptured: When a frame is captured and saved
 * - onCaptureStart: When capture session starts
 * - onCaptureStop: When capture session stops (with statistics)
 * - onCaptureError: When an error occurs during capture
 */
class CaptureEventEmitter(
    private val eventEmitter: (String, WritableMap?) -> Unit
) {

    /**
     * Emits onFrameCaptured event to JavaScript
     *
     * Contains frame information: file path, size, timestamp, and frame number.
     *
     * @param frameInfo Frame metadata
     * @param frameNumber Zero-based frame number in the session
     */
    fun emitFrameCaptured(frameInfo: FrameInfo, frameNumber: Int) {
        try {
            val params = frameInfo.toWritableMap(frameNumber)
            eventEmitter(Constants.EVENT_FRAME_CAPTURED, params)
        } catch (e: Exception) {
            // Silently fail - event emission is not critical
        }
    }

    /**
     * Emits onCaptureStart event to JavaScript
     *
     * Contains session ID and capture options.
     *
     * @param sessionId Unique session identifier
     * @param options Capture configuration options
     */
    fun emitCaptureStart(sessionId: String, options: com.framecapture.models.CaptureOptions) {
        try {
            val params = Arguments.createMap().apply {
                putString("sessionId", sessionId)
                putMap("options", options.toWritableMap())
            }
            eventEmitter(Constants.EVENT_CAPTURE_START, params)
        } catch (e: Exception) {
            // Silently fail - event emission is not critical
        }
    }

    /**
     * Emits onCaptureStop event to JavaScript with session statistics
     *
     * Contains session ID, total frames captured, and session duration.
     *
     * @param sessionId Unique session identifier
     * @param totalFrames Total number of frames captured
     * @param duration Session duration in milliseconds
     */
    fun emitCaptureStop(sessionId: String, totalFrames: Int, duration: Long) {
        try {
            val params = Arguments.createMap().apply {
                putString("sessionId", sessionId)
                putInt("totalFrames", totalFrames)
                putDouble("duration", duration.toDouble())
            }
            eventEmitter(Constants.EVENT_CAPTURE_STOP, params)
        } catch (e: Exception) {
            // Silently fail - event emission is not critical
        }
    }

    /**
     * Emits onCaptureError event to JavaScript
     *
     * Contains error code, message, and optional details for debugging.
     *
     * @param errorCode Error code enum value
     * @param message Human-readable error message
     * @param details Optional map of additional error context
     */
    fun emitError(errorCode: ErrorCode, message: String, details: Map<String, Any>?) {
        try {
            val params = Arguments.createMap().apply {
                putString("code", errorCode.value)
                putString("message", message)

                details?.let { detailsMap ->
                    val detailsWritableMap = Arguments.createMap()
                    detailsMap.forEach { (key, value) ->
                        when (value) {
                            is String -> detailsWritableMap.putString(key, value)
                            is Int -> detailsWritableMap.putInt(key, value)
                            is Long -> detailsWritableMap.putDouble(key, value.toDouble())
                            is Double -> detailsWritableMap.putDouble(key, value)
                            is Boolean -> detailsWritableMap.putBoolean(key, value)
                            else -> detailsWritableMap.putString(key, value.toString())
                        }
                    }
                    putMap("details", detailsWritableMap)
                }
            }

            eventEmitter(Constants.EVENT_CAPTURE_ERROR, params)
        } catch (e: Exception) {
            // Silently fail - event emission is not critical
        }
    }

    /**
     * Emits onChangeDetected event to JavaScript for debugging/monitoring
     *
     * Contains change detection results even when capture is not triggered.
     *
     * @param changePercent Percentage of pixels that changed (0-100)
     * @param threshold The configured threshold for triggering capture
     * @param captured Whether a capture was triggered
     * @param timeSinceLastCapture Milliseconds since last capture
     */
    fun emitChangeDetected(
        changePercent: Float,
        threshold: Float,
        captured: Boolean,
        timeSinceLastCapture: Long
    ) {
        try {
            val params = Arguments.createMap().apply {
                putDouble("changePercent", changePercent.toDouble())
                putDouble("threshold", threshold.toDouble())
                putBoolean("captured", captured)
                putDouble("timeSinceLastCapture", timeSinceLastCapture.toDouble())
            }
            eventEmitter(Constants.EVENT_CHANGE_DETECTED, params)
        } catch (e: Exception) {
            // Silently fail - event emission is not critical
        }
    }
}

