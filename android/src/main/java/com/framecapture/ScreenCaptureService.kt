package com.framecapture

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.framecapture.models.CaptureOptions
import com.framecapture.models.NotificationOptions
import com.framecapture.service.CaptureNotificationManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground service for reliable background screen capture
 *
 * This service ensures screen capture continues even when the app is minimized,
 * backgrounded, or the screen is off. It displays a persistent notification
 * (required for foreground services) and manages the CaptureManager lifecycle.
 *
 * Key responsibilities:
 * - Manages CaptureManager lifecycle (start/stop/pause/resume)
 * - Displays and updates foreground notification
 * - Bridges events between CaptureManager and FrameCaptureModule
 * - Handles notification button actions (pause/resume/stop)
 */
class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"

        // Notification constants
        const val CHANNEL_ID = Constants.CHANNEL_ID
        const val NOTIFICATION_ID = Constants.NOTIFICATION_ID

        // Service actions
        const val ACTION_START = Constants.ACTION_START_CAPTURE
        const val ACTION_STOP = Constants.ACTION_STOP_CAPTURE
        const val ACTION_PAUSE = Constants.ACTION_PAUSE_CAPTURE
        const val ACTION_RESUME = Constants.ACTION_RESUME_CAPTURE

        // Intent extras
        const val EXTRA_OPTIONS = Constants.EXTRA_CAPTURE_OPTIONS
        const val EXTRA_PROJECTION_DATA = Constants.EXTRA_PROJECTION_DATA

        // Static reference to FrameCaptureModule for event emission
        @Volatile
        private var FrameCaptureModule: FrameCaptureModule? = null

        // Static frame count shared between service and module
        @Volatile
        var currentFrameCount: Int = 0
            private set

        /**
         * Sets the FrameCaptureModule reference for event emission
         *
         * Must be called from FrameCaptureModule before starting the service.
         * This allows the service to emit events to JavaScript.
         */
        fun setFrameCaptureModule(module: FrameCaptureModule?) {
            FrameCaptureModule = module
        }

        /**
         * Updates the frame count
         * Called from the service to keep the count synchronized with the module
         */
        fun updateFrameCount(count: Int) {
            currentFrameCount = count
        }

        /**
         * Resets the frame count to zero
         * Called when a capture session starts or stops
         */
        fun resetFrameCount() {
            currentFrameCount = 0
        }
    }

    // CaptureManager instance for handling screen capture
    private var captureManager: CaptureManager? = null

    // StorageManager for saving frames
    private var storageManager: StorageManager? = null

    // Notification manager for handling notifications
    private var notificationManager: CaptureNotificationManager? = null

    // Frame counter for notification updates
    private var frameCounter: Int = 0

    // Paused state for notification updates
    private var isPaused: Boolean = false

    // Auto-stop timer (wall-clock since capture start, independent of pauses)
    private val autoStopHandlerThread: HandlerThread = HandlerThread("AutoStopThread").apply { start() }
    private val autoStopHandler: Handler = Handler(autoStopHandlerThread.looper)
    private var autoStopRunnable: Runnable? = null
    private val autoStopScheduled = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()

        // Notification channel is created in onStartCommand after parsing options
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // Extract options and projection data from intent
                val options = intent.getParcelableExtra<android.os.Bundle>(EXTRA_OPTIONS)
                val projectionData = intent.getParcelableExtra<Intent>(EXTRA_PROJECTION_DATA)

                if (options != null && projectionData != null) {
                    try {
                        // Parse notification options before creating notifications
                        val optionsMap = Arguments.fromBundle(options)
                        val captureOptions = CaptureOptions.fromReadableMap(optionsMap)

                        val notificationOptions = captureOptions.notification ?: NotificationOptions()

                        // Create notification manager with custom options
                        notificationManager = CaptureNotificationManager(this, notificationOptions)

                        // Create notification channel (Android 8.0+)
                        notificationManager?.createNotificationChannel()

                        // Start as foreground service (required for background capture)
                        startForeground(NOTIFICATION_ID, notificationManager?.createNotification())

                        // Initialize and start capture
                        startCapture(options, projectionData, notificationOptions)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start capture service", e)
                        stopSelf()
                    }
                } else {
                    stopSelf()
                }
            }

            ACTION_STOP -> {
                stopCapture()
                stopSelf()
            }

            ACTION_PAUSE -> {
                pauseCapture()
            }

            ACTION_RESUME -> {
                resumeCapture()
            }

            else -> {
                stopSelf()
            }
        }

        // START_STICKY: System will restart service if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This is not a bound service
        return null
    }

    override fun onDestroy() {
        // Ensure any pending auto-stop timer is dropped before stopping
        cancelAutoStop()

        // Ensure capture is stopped and resources are released
        try {
            stopCapture()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture in onDestroy", e)
        }

        autoStopHandlerThread.quitSafely()

        super.onDestroy()
    }

    // ========== Capture Management ==========

    /**
     * Starts screen capture with the specified options
     *
     * Creates and initializes CaptureManager and StorageManager, sets up event
     * handlers for frame capture and notification updates, then starts capture.
     *
     * @param optionsBundle Capture options as Android Bundle
     * @param projectionData MediaProjection permission data
     * @param notificationOptions Notification customization options
     */
    private fun startCapture(optionsBundle: android.os.Bundle, projectionData: Intent, notificationOptions: NotificationOptions) {
        try {
            // Convert Bundle to CaptureOptions
            val optionsMap = Arguments.fromBundle(optionsBundle)
            val captureOptions = CaptureOptions.fromReadableMap(optionsMap)

            // Create StorageManager if needed
            if (storageManager == null) {
                // Event emitter for StorageManager (handles storage warnings)
                val eventEmitter: (String, WritableMap?) -> Unit = { eventName, params ->
                    // Handle frame captured events for notification updates
                    if (eventName == Constants.EVENT_FRAME_CAPTURED) {
                        params?.let {
                            if (it.hasKey("frameNumber")) {
                                frameCounter = it.getInt("frameNumber") + 1

                                // Update notification based on configured interval
                                if (notificationOptions.showFrameCount &&
                                    notificationOptions.updateInterval > 0 &&
                                    frameCounter % notificationOptions.updateInterval == 0) {
                                    notificationManager?.updateNotification(frameCounter)
                                }
                            }
                        }
                    }

                    // Forward storage warnings to JavaScript
                    if (eventName == Constants.EVENT_STORAGE_WARNING) {
                        try {
                            val module = FrameCaptureModule
                            if (module != null && params != null) {
                                module.emitOnStorageWarning(params)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to emit storage warning: ${e.message}", e)
                        }
                    }
                }

                // Initialize StorageManager for frame persistence
                storageManager = StorageManager(
                    applicationContext,
                    eventEmitter
                )
            }

            // Initialize CaptureManager with event handlers
            captureManager = CaptureManager(
                applicationContext,
                storageManager!!,
                { eventName, params ->
                    // Handle frame captured events
                    if (eventName == Constants.EVENT_FRAME_CAPTURED) {
                        frameCounter++
                        // Sync frame count with module
                        updateFrameCount(frameCounter)

                        // Update notification based on configuration
                        if (!isPaused &&
                            notificationOptions.showFrameCount &&
                            notificationOptions.updateInterval > 0 &&
                            frameCounter % notificationOptions.updateInterval == 0) {
                            notificationManager?.updateNotification(frameCounter)
                        }
                    }

                    // Forward events to JavaScript via FrameCaptureModule
                    try {
                        val module = FrameCaptureModule
                        if (module != null && params != null) {
                            when (eventName) {
                                Constants.EVENT_FRAME_CAPTURED -> module.emitOnFrameCaptured(params)
                                Constants.EVENT_CAPTURE_ERROR -> module.emitOnCaptureError(params)
                                Constants.EVENT_CAPTURE_STOP -> module.emitOnCaptureStop(params)
                                Constants.EVENT_CAPTURE_START -> module.emitOnCaptureStart(params)
                                Constants.EVENT_STORAGE_WARNING -> module.emitOnStorageWarning(params)
                                Constants.EVENT_OVERLAY_ERROR -> module.emitOnOverlayError(params)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send event: $eventName", e)
                    }
                }
            )

            // Initialize with MediaProjection permission and options
            captureManager?.initialize(projectionData, captureOptions)

            // Start capture
            captureManager?.start()

            // Schedule auto-stop if configured
            scheduleAutoStop(captureOptions.autoStopTimeout)

            // Reset counters and state for new session
            frameCounter = 0
            resetFrameCount()
            isPaused = false

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture in service", e)
            throw e
        }
    }

    /**
     * Schedules automatic capture stop after the given wall-clock timeout.
     *
     * Uses a Handler on a background thread: the timer fires regardless of JS
     * being suspended, and pause/resume do not extend the timeout.
     *
     * @param timeout Timeout in milliseconds (0 = disabled)
     */
    private fun scheduleAutoStop(timeout: Long) {
        cancelAutoStop()
        if (timeout <= 0) return

        val runnable = Runnable {
            if (autoStopScheduled.compareAndSet(true, false)) {
                Log.i(TAG, "Auto-stop timeout reached, stopping capture")
                stopCapture(CaptureManager.STOP_REASON_AUTO_STOP_TIMEOUT)
            }
        }

        autoStopRunnable = runnable
        autoStopScheduled.set(true)
        autoStopHandler.postDelayed(runnable, timeout)
    }

    /**
     * Cancels any pending auto-stop timer.
     */
    private fun cancelAutoStop() {
        autoStopRunnable?.let { autoStopHandler.removeCallbacks(it) }
        autoStopRunnable = null
        autoStopScheduled.set(false)
    }

    /**
     * Stops screen capture and cleans up all resources
     *
     * Notifies the module, stops capture, releases CaptureManager,
     * and resets all state variables.
     *
     * @param reason Why capture stopped (manual, error, or auto-stop timeout)
     */
    private fun stopCapture(reason: String = CaptureManager.STOP_REASON_MANUAL) {
        cancelAutoStop()
        try {
            // Remove the foreground notification (it's setOngoing, so it
            // survives unless explicitly dismissed when the service stops)
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager = null

            // Notify module first to update state
            FrameCaptureModule?.updateStateFromService(isStopped = true)

            // Stop capture (emits onCaptureStop event)
            captureManager?.stop(reason)

            // Cleanup resources
            captureManager?.cleanup()
            captureManager = null

            // Reset counters and state
            frameCounter = 0
            resetFrameCount()
            isPaused = false

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture in service", e)
        }
    }

    /**
     * Pauses screen capture
     *
     * Stops capturing frames but keeps the service and notification active.
     * Updates notification to show paused state with resume button.
     */
    private fun pauseCapture() {
        try {
            // Pause capture
            captureManager?.pause()

            // Update paused state
            isPaused = true

            // Update notification to show paused state
            notificationManager?.updateNotificationPaused(frameCounter)

            // Notify module about pause
            FrameCaptureModule?.updateStateFromService(isPaused = true)

        } catch (e: Exception) {
            Log.e(TAG, "Error pausing capture in service", e)
        }
    }

    /**
     * Resumes screen capture from paused state
     *
     * Restarts frame capture and updates notification to show active state
     * with pause button.
     */
    private fun resumeCapture() {
        try {
            // Resume capture
            captureManager?.resume()

            // Update paused state
            isPaused = false

            // Update notification to show active state
            notificationManager?.updateNotification(frameCounter)

            // Notify module about resume
            FrameCaptureModule?.updateStateFromService(isPaused = false)

        } catch (e: Exception) {
            Log.e(TAG, "Error resuming capture in service", e)
        }
    }
}
