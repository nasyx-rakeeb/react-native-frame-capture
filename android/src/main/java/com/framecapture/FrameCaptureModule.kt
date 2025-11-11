package com.framecapture

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.framecapture.models.*
import com.framecapture.utils.ValidationResult
import com.framecapture.utils.ValidationUtils
import java.io.IOException

/**
 * Main TurboModule implementation for React Native screen capture
 *
 * This module provides the bridge between JavaScript and native Android screen capture
 * functionality. It coordinates between StorageManager, CaptureManager, and the
 * ScreenCaptureService to provide reliable foreground screen capture.
 *
 * Key responsibilities:
 * - Permission management (MediaProjection)
 * - Capture session lifecycle (start/stop/pause/resume)
 * - Event emission to JavaScript
 * - Error handling and reporting
 */
@ReactModule(name = FrameCaptureModule.NAME)
class FrameCaptureModule(reactContext: ReactApplicationContext) :
    NativeFrameCaptureSpec(reactContext),
    LifecycleEventListener {

    // Component dependencies
    private val storageManager: StorageManager
    private var captureManager: CaptureManager? = null

    // State management
    private var currentSession: CaptureSession? = null
    private var captureState: CaptureState = CaptureState.IDLE

    // Specialized handlers
    private val permissionHandler: PermissionHandler
    private val errorHandler: ErrorHandler

    // Activity event listener
    private val activityEventListener = object : BaseActivityEventListener() {
        override fun onActivityResult(
            activity: Activity,
            requestCode: Int,
            resultCode: Int,
            data: Intent?
        ) {
            permissionHandler.handleActivityResult(requestCode, resultCode, data)
        }
    }

    init {
        // Initialize helper components
        storageManager = StorageManager(reactContext, ::sendEvent)

        // Initialize handlers
        permissionHandler = PermissionHandler { currentActivity }
        errorHandler = ErrorHandler(
            getStorageSpace = { storageManager.checkStorageSpace() },
            getCaptureState = { captureState }
        )

        // Register lifecycle listener
        reactContext.addLifecycleEventListener(this)
        reactContext.addActivityEventListener(activityEventListener)
    }

    override fun getName(): String {
        return NAME
    }

    // ========== Permission Management ==========

    /**
     * Requests MediaProjection permission from the user
     *
     * Opens the Android system permission dialog for screen capture access.
     * The result is handled asynchronously through the activity result callback.
     */
    override fun requestPermission(promise: Promise) {
        try {
            permissionHandler.requestPermission(promise)
        } catch (e: Exception) {
            handleError(e, promise)
        }
    }

    /**
     * Checks current MediaProjection permission status
     *
     * Note: MediaProjection permission cannot be checked programmatically on Android.
     * This method only verifies if permission data has been stored from a previous grant.
     *
     * @return PermissionStatus.GRANTED if permission data exists, NOT_DETERMINED otherwise
     */
    override fun checkPermission(promise: Promise) {
        try {
            val status = permissionHandler.checkPermission()
            promise.resolve(status.value)
        } catch (e: Exception) {
            handleError(e, promise)
        }
    }

    // ========== Capture Control ==========

    /**
     * Starts a new screen capture session with the specified options
     *
     * Creates and starts a foreground service (ScreenCaptureService) to handle
     * capture in the background. This ensures capture continues even when the
     * app is minimized or the screen is off.
     *
     * @param options Capture configuration (interval, format, quality, etc.)
     * @param promise Resolves with session information or rejects with error
     */
    override fun startCapture(options: ReadableMap, promise: Promise) {
        try {
            // Check if already capturing
            if (captureState == CaptureState.CAPTURING || captureState == CaptureState.PAUSED) {
                promise.reject(
                    ErrorCode.ALREADY_CAPTURING.value,
                    "Capture session is already active",
                    errorHandler.createErrorMap(
                        ErrorCode.ALREADY_CAPTURING,
                        "Capture session is already active",
                        mapOf(Constants.ERROR_DETAIL_CURRENT_STATE to captureState.value)
                    )
                )
                return
            }

            // Check if MediaProjection permission granted
            val mediaProjectionData = permissionHandler.mediaProjectionData
            if (mediaProjectionData == null) {
                promise.reject(
                    ErrorCode.PERMISSION_DENIED.value,
                    "MediaProjection permission not granted. Call requestPermission() first.",
                    errorHandler.createErrorMap(
                        ErrorCode.PERMISSION_DENIED,
                        "MediaProjection permission not granted",
                        mapOf(Constants.ERROR_DETAIL_PERMISSION to "MEDIA_PROJECTION")
                    )
                )
                return
            }

            // Parse and validate options
            val captureOptions = CaptureOptions.fromReadableMap(options)
            val validationResult = ValidationUtils.validateOptions(captureOptions)

            if (validationResult is ValidationResult.Error) {
                val errorMessage = ValidationUtils.formatValidationError(validationResult)
                    ?: "Invalid options"
                promise.reject(
                    ErrorCode.INVALID_OPTIONS.value,
                    errorMessage,
                    errorHandler.createErrorMap(
                        ErrorCode.INVALID_OPTIONS,
                        errorMessage,
                        mapOf(Constants.ERROR_DETAIL_ERRORS to validationResult.messages)
                    )
                )
                return
            }

            // Use foreground service for reliable background capture
            // This ensures capture continues when app is backgrounded

            // Set module reference so service can emit events to JavaScript
            ScreenCaptureService.setFrameCaptureModule(this)

            // Create intent for ScreenCaptureService
            val serviceIntent = Intent(reactApplicationContext, ScreenCaptureService::class.java).apply {
                action = Constants.ACTION_START_CAPTURE

                // Convert options ReadableMap to Bundle
                val optionsBundle = Arguments.toBundle(options)
                putExtra(Constants.EXTRA_CAPTURE_OPTIONS, optionsBundle)

                // Add projection data
                putExtra(Constants.EXTRA_PROJECTION_DATA, mediaProjectionData)
            }

            // Start service (use startForegroundService for API 26+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                reactApplicationContext.startForegroundService(serviceIntent)
            } else {
                reactApplicationContext.startService(serviceIntent)
            }

            // Update state
            captureState = CaptureState.CAPTURING

            // Create session object
            val sessionId = java.util.UUID.randomUUID().toString()
            currentSession = CaptureSession(
                id = sessionId,
                startTime = System.currentTimeMillis(),
                frameCount = 0,
                options = captureOptions
            )

            // Return session information
            promise.resolve(currentSession?.toWritableMap())

        } catch (e: Exception) {
            captureState = CaptureState.IDLE
            handleError(e, promise)
        }
    }

    /**
     * Stops the active capture session
     *
     * Sends a stop action to the ScreenCaptureService, which will terminate
     * the capture, clean up resources, and stop the foreground service.
     *
     * @param promise Resolves when stop is initiated or rejects with error
     */
    override fun stopCapture(promise: Promise) {
        try {
            if (captureState == CaptureState.IDLE) {
                promise.reject(
                    ErrorCode.SYSTEM_ERROR.value,
                    "No active capture session to stop"
                )
                return
            }

            // Send stop action to service
            val serviceIntent = Intent(reactApplicationContext, ScreenCaptureService::class.java).apply {
                action = Constants.ACTION_STOP_CAPTURE
            }
            reactApplicationContext.startService(serviceIntent)

            // Reset state
            captureState = CaptureState.IDLE
            currentSession = null

            promise.resolve(null)

        } catch (e: Exception) {
            handleError(e, promise)
        }
    }

    /**
     * Pauses the active capture session
     *
     * Stops capturing frames but keeps the service running. The session can be
     * resumed later without losing configuration or session state.
     *
     * @param promise Resolves when pause is initiated or rejects with error
     */
    override fun pauseCapture(promise: Promise) {
        try {
            if (captureState != CaptureState.CAPTURING) {
                promise.reject(
                    ErrorCode.SYSTEM_ERROR.value,
                    "No active capture session to pause"
                )
                return
            }

            // Send pause action to service (captureManager is null in service mode)
            if (captureManager == null) {
                val serviceIntent = Intent(reactApplicationContext, ScreenCaptureService::class.java).apply {
                    action = Constants.ACTION_PAUSE_CAPTURE
                }
                reactApplicationContext.startService(serviceIntent)
            } else {
                // Fallback: Direct mode (not currently used)
                captureManager?.pause()
            }

            captureState = CaptureState.PAUSED

            // Emit pause event to JavaScript
            val sessionId = currentSession?.id ?: ""
            val params = Arguments.createMap().apply {
                putString("sessionId", sessionId)
            }
            sendEvent(Constants.EVENT_CAPTURE_PAUSE, params)

            promise.resolve(null)

        } catch (e: Exception) {
            handleError(e, promise)
        }
    }

    /**
     * Resumes a paused capture session
     *
     * Restarts frame capture at the configured interval. The session continues
     * with the same configuration and session ID.
     *
     * @param promise Resolves when resume is initiated or rejects with error
     */
    override fun resumeCapture(promise: Promise) {
        try {
            if (captureState != CaptureState.PAUSED) {
                promise.reject(
                    ErrorCode.SYSTEM_ERROR.value,
                    "Capture session is not paused"
                )
                return
            }

            // Send resume action to service (captureManager is null in service mode)
            if (captureManager == null) {
                val serviceIntent = Intent(reactApplicationContext, ScreenCaptureService::class.java).apply {
                    action = Constants.ACTION_RESUME_CAPTURE
                }
                reactApplicationContext.startService(serviceIntent)
            } else {
                // Fallback: Direct mode (not currently used)
                captureManager?.resume()
            }

            captureState = CaptureState.CAPTURING

            // Emit resume event to JavaScript
            val sessionId = currentSession?.id ?: ""
            val params = Arguments.createMap().apply {
                putString("sessionId", sessionId)
            }
            sendEvent(Constants.EVENT_CAPTURE_RESUME, params)

            promise.resolve(null)

        } catch (e: Exception) {
            handleError(e, promise)
        }
    }

    /**
     * Gets the current capture status including state and session information
     *
     * Returns the current capture state (IDLE, CAPTURING, PAUSED) along with
     * session details like frame count, session ID, and options.
     *
     * @param promise Resolves with CaptureStatus object or rejects with error
     */
    override fun getCaptureStatus(promise: Promise) {
        try {
            val status = if (captureManager != null) {
                // Fallback: Direct mode (not currently used)
                captureManager?.getStatus()
            } else {
                // Service mode - create status with frame count from service
                currentSession?.let { session ->
                    CaptureStatus(
                        state = captureState,
                        session = session.copy(frameCount = ScreenCaptureService.currentFrameCount),
                        isPaused = captureState == CaptureState.PAUSED
                    )
                }
            } ?: CaptureStatus(
                state = captureState,
                session = currentSession,
                isPaused = captureState == CaptureState.PAUSED
            )

            promise.resolve(status.toWritableMap())

        } catch (e: Exception) {
            handleError(e, promise)
        }
    }

    // ========== Notification Permission (Android 13+) ==========

    /**
     * Checks if notification permission is granted (Android 13+)
     *
     * On Android 13 (API 33) and above, apps need runtime permission to post notifications.
     * This method checks if that permission has been granted.
     *
     * Note: This only checks the permission status. To request the permission,
     * use React Native's PermissionsAndroid.request() API with POST_NOTIFICATIONS.
     *
     * On Android 12 and below, this always returns GRANTED as no permission is needed.
     *
     * @param promise Resolves with PermissionStatus (GRANTED or DENIED)
     */
    override fun checkNotificationPermission(promise: Promise) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = reactApplicationContext.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val status = if (hasPermission) {
                    PermissionStatus.GRANTED
                } else {
                    PermissionStatus.DENIED
                }
                promise.resolve(status.value)
            } else {
                // Below Android 13, notification permission is not required
                promise.resolve(PermissionStatus.GRANTED.value)
            }
        } catch (e: Exception) {
            handleError(e, promise)
        }
    }

    /**
     * Manually cleans up all temporary frame files
     *
     * Temporary files are created when saveFrames is false. They are automatically
     * cleaned on app startup, but this method allows manual cleanup when needed
     * (e.g., after processing frames or to free up storage space).
     *
     * @param promise Resolves when cleanup is complete or rejects with error
     */
    override fun cleanupTempFrames(promise: Promise) {
        try {
            storageManager.cleanupAllTempFiles()
            promise.resolve(null)
        } catch (e: Exception) {
            handleError(e, promise)
        }
    }

    // ========== Event Emission ==========

    /**
     * Sends events to JavaScript using TurboModule EventEmitter
     *
     * Routes events to the appropriate emit method based on event name.
     * Uses the generated emit methods from NativeFrameCaptureSpec.
     *
     * @param eventName The event type (e.g., EVENT_FRAME_CAPTURED)
     * @param params Event data to send to JavaScript
     */
    private fun sendEvent(eventName: String, params: WritableMap?) {
        try {
            // Use the generated emit methods from NativeFrameCaptureSpec
            when (eventName) {
                Constants.EVENT_FRAME_CAPTURED -> params?.let { emitOnFrameCaptured(it) }
                Constants.EVENT_CAPTURE_ERROR -> params?.let { emitOnCaptureError(it) }
                Constants.EVENT_CAPTURE_STOP -> params?.let { emitOnCaptureStop(it) }
                Constants.EVENT_CAPTURE_START -> params?.let { emitOnCaptureStart(it) }
                Constants.EVENT_STORAGE_WARNING -> params?.let { emitOnStorageWarning(it) }
                Constants.EVENT_CAPTURE_PAUSE -> params?.let { emitOnCapturePause(it) }
                Constants.EVENT_CAPTURE_RESUME -> params?.let { emitOnCaptureResume(it) }
                Constants.EVENT_OVERLAY_ERROR -> params?.let { emitOnOverlayError(it) }
            }
        } catch (e: Exception) {
            Log.e(NAME, "Failed to send event $eventName: ${e.message}", e)
        }
    }



    /**
     * Emits frame captured event to JavaScript
     * Contains frame information (path, size, timestamp, frame number)
     */
    private fun emitFrameCapturedEvent(frameInfo: FrameInfo, frameNumber: Int) {
        sendEvent(Constants.EVENT_FRAME_CAPTURED, frameInfo.toWritableMap(frameNumber))
    }

    /**
     * Emits capture error event to JavaScript
     * Contains error code, message, and optional details
     */
    private fun emitCaptureErrorEvent(code: ErrorCode, message: String, details: Map<String, Any>?) {
        val params = errorHandler.createErrorMap(code, message, details)
        sendEvent(Constants.EVENT_CAPTURE_ERROR, params)
    }

    /**
     * Emits capture stop event to JavaScript with session statistics
     * Contains session ID, total frames captured, and duration
     */
    private fun emitCaptureStopEvent(sessionId: String, totalFrames: Int, duration: Long) {
        val params = Arguments.createMap().apply {
            putString("sessionId", sessionId)
            putInt("totalFrames", totalFrames)
            putDouble("duration", duration.toDouble())
        }
        sendEvent(Constants.EVENT_CAPTURE_STOP, params)
    }

    /**
     * Emits capture start event to JavaScript with session information
     * Contains session ID and capture options
     */
    private fun emitCaptureStartEvent(session: CaptureSession) {
        val params = Arguments.createMap().apply {
            putString("sessionId", session.id)
            putMap("options", session.options.toWritableMap())
        }
        sendEvent(Constants.EVENT_CAPTURE_START, params)
    }

    // ========== Lifecycle Management ==========

    /**
     * Initializes the module
     * Called when the module is first created by React Native
     */
    override fun initialize() {
        super.initialize()
        // Activity event listener already registered in init block
    }

    /**
     * Cleans up resources when module is invalidated
     *
     * Called when the React Native bridge is destroyed (app reload, app close).
     * Ensures all capture sessions are stopped and resources are released.
     */
    override fun invalidate() {
        try {
            // Stop capture if active
            if (captureState == CaptureState.CAPTURING || captureState == CaptureState.PAUSED) {
                captureManager?.stop()
            }

            // Cleanup all resources
            captureManager?.cleanup()
            captureManager = null

            // Reset state
            captureState = CaptureState.IDLE
            currentSession = null
            permissionHandler.clear()

            // Remove listeners
            reactApplicationContext.removeLifecycleEventListener(this)
            reactApplicationContext.removeActivityEventListener(activityEventListener)

        } catch (e: Exception) {
            // Silent cleanup failure
        }

        super.invalidate()
    }

    /**
     * Called when the host activity resumes
     * Lifecycle callback - currently unused but available for future enhancements
     */
    override fun onHostResume() {
        // Can be used to resume capture if needed
    }

    /**
     * Called when the host activity pauses
     * Lifecycle callback - currently unused but available for future enhancements
     */
    override fun onHostPause() {
        // Can be used to pause capture if needed
    }

    /**
     * Called when the host activity is destroyed
     * Ensures cleanup is performed to prevent resource leaks
     */
    override fun onHostDestroy() {
        // Ensure cleanup is called
        try {
            if (captureState == CaptureState.CAPTURING || captureState == CaptureState.PAUSED) {
                captureManager?.stop()
                captureManager?.cleanup()
            }
        } catch (e: Exception) {
            // Silent cleanup failure
        }
    }

    // ========== Error Handling ==========

    /**
     * Handles errors and classifies them into appropriate error codes
     *
     * Delegates to ErrorHandler for classification and reporting. Emits error
     * events to JavaScript and performs cleanup if needed.
     *
     * @param error The exception that occurred
     * @param promise Optional promise to reject with error details
     */
    private fun handleError(error: Exception, promise: Promise? = null) {
        errorHandler.handleError(
            error = error,
            promise = promise,
            onEmitError = { code, message, details ->
                emitCaptureErrorEvent(code, message, details)
            },
            onCleanup = {
                if (captureState != CaptureState.IDLE) {
                    captureManager?.cleanup()
                    captureState = CaptureState.IDLE
                    currentSession = null
                }
            }
        )
    }

    // ========== Service State Synchronization ==========

    /**
     * Updates module state from the ScreenCaptureService
     *
     * This keeps the module's state synchronized when actions are triggered
     * from notification buttons (pause/resume/stop). Also emits appropriate
     * events to JavaScript to keep the JS side in sync.
     *
     * @param isPaused True if paused, false if resumed, null if no change
     * @param isStopped True if capture was stopped
     */
    fun updateStateFromService(isPaused: Boolean? = null, isStopped: Boolean = false) {
        try {
            if (isStopped) {
                // Service was stopped
                captureState = CaptureState.IDLE
                currentSession = null
            } else if (isPaused != null) {
                // Pause/resume state changed
                captureState = if (isPaused) CaptureState.PAUSED else CaptureState.CAPTURING

                // Emit pause/resume event to JavaScript
                val sessionId = currentSession?.id ?: ""
                val params = Arguments.createMap().apply {
                    putString("sessionId", sessionId)
                }

                if (isPaused) {
                    sendEvent(Constants.EVENT_CAPTURE_PAUSE, params)
                } else {
                    sendEvent(Constants.EVENT_CAPTURE_RESUME, params)
                }
            }
        } catch (e: Exception) {
            Log.e(NAME, "Error updating state from service", e)
        }
    }

    companion object {
        const val NAME = "FrameCapture"
    }
}
