package com.framecapture

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.framecapture.models.CaptureOptions
import com.framecapture.models.CaptureSession
import com.framecapture.models.CaptureState
import com.framecapture.models.CaptureStatus
import com.framecapture.models.ErrorCode
import com.framecapture.models.FrameInfo
import com.framecapture.models.CaptureMode
import com.framecapture.capture.CaptureEventEmitter
import com.framecapture.capture.BitmapProcessor
import com.framecapture.capture.ChangeDetector
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Manages screen capture operations using Android's MediaProjection API
 *
 * This class orchestrates the entire screen capture process, including:
 * - MediaProjection and VirtualDisplay lifecycle management
 * - ImageReader for frame acquisition
 * - Interval-based periodic capture
 * - Frame processing (bitmap conversion, overlays, storage)
 * - Event emission to JavaScript
 *
 * Delegates specialized tasks to:
 * - CaptureEventEmitter: Event emission
 * - BitmapProcessor: Image processing and cropping
 */
class CaptureManager(
    private val context: android.content.Context,
    private val storageManager: StorageManager,
    private val eventEmitter: (String, WritableMap?) -> Unit
) {

    // MediaProjection components
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    // Capture state
    private var captureOptions: CaptureOptions? = null
    private var sessionId: String? = null
    private var sessionStartTime: Long = 0
    private var frameCount: Int = 0
    private var isCapturing: Boolean = false
    private var isPaused: Boolean = false

    // Threading for capture operations
    private val captureThread: HandlerThread = HandlerThread("CaptureThread").apply { start() }
    private val captureHandler: Handler = Handler(captureThread.looper)

    // Background processing for image conversion and saving
    private val processingExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Timing for throttling
    private var lastCaptureTime: Long = 0

    // Runnable for periodic capture
    private var captureRunnable: Runnable? = null

    // Overlay renderer for adding watermarks and metadata
    private val overlayRenderer: OverlayRenderer by lazy {
        OverlayRenderer(context, eventEmitter)
    }

    // Cached status bar height
    private var statusBarHeight: Int = 0

    // Specialized managers
    private lateinit var eventEmitterManager: CaptureEventEmitter
    private lateinit var bitmapProcessor: BitmapProcessor

    // Change detection support
    private var changeDetector: ChangeDetector? = null

    companion object {
        private const val TAG = "CaptureManager"
        private const val VIRTUAL_DISPLAY_NAME = "ScreenCapture"
    }

    init {
        // Initialize MediaProjectionManager
        mediaProjectionManager = context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE)
            as? MediaProjectionManager

        // Calculate status bar height once
        statusBarHeight = getStatusBarHeight()

        // Initialize specialized managers
        eventEmitterManager = CaptureEventEmitter(eventEmitter)
        bitmapProcessor = BitmapProcessor(statusBarHeight)
    }

    /**
     * Gets the status bar height in pixels
     *
     * Uses Android's resource system to retrieve the standard status bar height.
     * This is cached and used for cropping when excludeStatusBar is enabled.
     *
     * @return Status bar height in pixels, or 0 if not found
     */
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * Initializes the capture manager with MediaProjection data and options
     *
     * @param projectionData Intent containing MediaProjection permission result
     * @param options Capture configuration options
     * @throws SecurityException if MediaProjection permission not granted
     * @throws IllegalStateException if already initialized
     */
    fun initialize(projectionData: Intent, options: CaptureOptions) {
        try {
            // Check if already initialized
            if (mediaProjection != null) {
                throw IllegalStateException("CaptureManager already initialized")
            }

            // Store capture options
            this.captureOptions = options

            // Generate unique session ID and record start time
            this.sessionId = UUID.randomUUID().toString()
            this.sessionStartTime = System.currentTimeMillis()

            // Get MediaProjection from the permission result
            mediaProjection = mediaProjectionManager?.getMediaProjection(
                android.app.Activity.RESULT_OK,
                projectionData
            ) ?: throw SecurityException("Failed to obtain MediaProjection")

            // Register callback to handle projection stop events
            mediaProjection?.registerCallback(projectionCallback, captureHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CaptureManager", e)
            cleanup()
            throw e
        }
    }

    /**
     * Callback for MediaProjection lifecycle events
     *
     * Handles system-initiated MediaProjection stops (e.g., user revokes permission,
     * system kills projection). Emits stop event and cleans up resources.
     */
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            // Emit capture stop event
            try {
                val session = getCurrentSession()
                val duration = System.currentTimeMillis() - (session?.startTime ?: 0)

                val params = Arguments.createMap().apply {
                    putString("sessionId", sessionId ?: "")
                    putInt("totalFrames", frameCount)
                    putDouble("duration", duration.toDouble())
                }

                eventEmitter(Constants.EVENT_CAPTURE_STOP, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to emit capture stop event", e)
            }

            // Clean up resources
            cleanup()
        }
    }

    /**
     * Gets the current capture session information
     *
     * @return CaptureSession with current state, or null if no active session
     */
    private fun getCurrentSession(): CaptureSession? {
        val id = sessionId ?: return null
        val options = captureOptions ?: return null

        return CaptureSession(
            id = id,
            startTime = sessionStartTime,
            frameCount = frameCount,
            options = options
        )
    }

    /**
     * Data class to hold screen metrics
     */
    private data class ScreenMetrics(
        val width: Int,
        val height: Int,
        val density: Int
    )

    /**
     * Gets the device screen dimensions and density
     *
     * Retrieves real screen metrics and applies resolution scaling if configured.
     *
     * @return ScreenMetrics with width, height, and density
     */
    private fun getScreenMetrics(): ScreenMetrics {
        val windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()

        windowManager.defaultDisplay.getRealMetrics(displayMetrics)

        var width = displayMetrics.widthPixels
        var height = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi

        // Apply resolution scaling if specified
        captureOptions?.scaleResolution?.let { scale ->
            if (scale > 0 && scale <= 1.0f) {
                width = (width * scale).toInt()
                height = (height * scale).toInt()
            }
        }

        return ScreenMetrics(width, height, density)
    }

    /**
     * Sets up the ImageReader for capturing frames
     *
     * Creates an ImageReader with RGBA_8888 format for interval-based capture.
     * No listener is configured as frames are captured periodically.
     *
     * @param width Screen width in pixels
     * @param height Screen height in pixels
     */
    private fun setupImageReader(width: Int, height: Int) {
        try {
            // Create ImageReader with RGBA_8888 format
            val bufferCount = captureOptions?.advanced?.performance?.imageReaderBuffers
                ?: Constants.DEFAULT_IMAGE_READER_MAX_IMAGES

            imageReader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                bufferCount
            )
            // No listener needed for interval mode - periodic capture handles timing

        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup ImageReader", e)
            throw e
        }
    }

    /**
     * Creates the VirtualDisplay for screen capture
     *
     * The VirtualDisplay mirrors the device screen to the ImageReader's surface,
     * allowing frame capture without affecting the actual display.
     *
     * @param metrics Screen dimensions and density for the virtual display
     */
    private fun createVirtualDisplay(metrics: ScreenMetrics) {
        try {
            val surface = imageReader?.surface
                ?: throw IllegalStateException("ImageReader not initialized")

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                metrics.width,
                metrics.height,
                metrics.density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                captureHandler
            ) ?: throw IllegalStateException("Failed to create VirtualDisplay")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create VirtualDisplay", e)
            throw e
        }
    }

    /**
     * Starts periodic frame capture at the configured interval
     *
     * Posts a recurring runnable that captures frames at the specified interval.
     * This is the only capture mechanism for interval-based capture.
     */
    private fun startPeriodicCapture() {
        try {
            val interval = captureOptions?.interval ?: Constants.DEFAULT_INTERVAL

            captureRunnable = object : Runnable {
                override fun run() {
                    try {
                        // Check if we should continue running
                        if (isCapturing) {
                            // Only capture if not paused
                            if (!isPaused && shouldCapture()) {
                                captureFrame()
                            }

                            // Always schedule next capture while isCapturing is true
                            captureHandler.postDelayed(this, interval)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in capture runnable", e)
                        handleError(e)
                    }
                }
            }

            // Start the periodic capture
            captureHandler.post(captureRunnable!!)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start periodic capture", e)
            throw e
        }
    }

    /**
     * Stops periodic frame capture
     * Removes the recurring capture runnable from the handler
     */
    private fun stopPeriodicCapture() {
        try {
            captureRunnable?.let { runnable ->
                captureHandler.removeCallbacks(runnable)
                captureRunnable = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping periodic capture", e)
        }
    }

    /**
     * Starts change detection capture mode
     *
     * Polls for frames at minInterval and compares them to detect changes.
     * Only captures when change exceeds threshold or maxInterval forces capture.
     */
    private fun startChangeDetectionCapture() {
        try {
            val changeConfig = captureOptions?.changeDetection
            val minInterval = changeConfig?.minInterval ?: Constants.DEFAULT_CHANGE_MIN_INTERVAL
            val maxInterval = changeConfig?.maxInterval ?: Constants.DEFAULT_CHANGE_MAX_INTERVAL
            val threshold = changeConfig?.threshold ?: Constants.DEFAULT_CHANGE_THRESHOLD

            captureRunnable = object : Runnable {
                override fun run() {
                    try {
                        if (isCapturing && !isPaused) {
                            val image = imageReader?.acquireLatestImage()
                            if (image != null) {
                                processingExecutor.execute {
                                    try {
                                        processChangeDetectionFrame(image, threshold, minInterval, maxInterval)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error processing change detection frame", e)
                                        handleError(e)
                                    } finally {
                                        image.close()
                                    }
                                }
                            }
                        }

                        // Schedule next check if still capturing
                        if (isCapturing) {
                            captureHandler.postDelayed(this, minInterval)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in change detection runnable", e)
                        handleError(e)
                    }
                }
            }

            // Start the change detection polling
            captureHandler.post(captureRunnable!!)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start change detection capture", e)
            throw e
        }
    }

    /**
     * Processes a frame for change detection
     *
     * Compares frame with previous, determines if capture should occur,
     * and emits change detection events.
     */
    private fun processChangeDetectionFrame(
        image: Image,
        threshold: Float,
        minInterval: Long,
        maxInterval: Long
    ) {
        var bitmap: Bitmap? = null

        try {
            // Convert to bitmap for comparison
            bitmap = bitmapProcessor.imageToBitmap(image, captureOptions)

            val currentTime = System.currentTimeMillis()
            val timeSinceLastCapture = currentTime - lastCaptureTime

            // Detect change percentage
            val detector = changeDetector ?: return
            val changePercent = detector.detectChange(bitmap)

            // Determine if we should capture
            val shouldCaptureDueToChange = changePercent >= threshold
            val shouldCaptureDueToMaxInterval = maxInterval > 0 && timeSinceLastCapture >= maxInterval
            val hasMinIntervalPassed = timeSinceLastCapture >= minInterval

            val shouldDoCapture = hasMinIntervalPassed && (shouldCaptureDueToChange || shouldCaptureDueToMaxInterval)

            // Emit change detected event for debugging/monitoring
            eventEmitterManager.emitChangeDetected(
                changePercent = changePercent,
                threshold = threshold,
                captured = shouldDoCapture,
                timeSinceLastCapture = timeSinceLastCapture
            )

            if (shouldDoCapture) {
                // Update previous frame for next comparison
                detector.updatePreviousFrame(bitmap)
                lastCaptureTime = currentTime

                // Process and save the frame (reuse processImage logic)
                processImageFromBitmap(bitmap)
                // Don't recycle - processImageFromBitmap handles it
                bitmap = null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in change detection frame processing", e)
            handleError(e)
        } finally {
            bitmap?.recycle()
        }
    }

    /**
     * Processes an already-converted bitmap (for change detection mode)
     */
    private fun processImageFromBitmap(bitmap: Bitmap) {
        try {
            val currentSessionId = sessionId ?: throw IllegalStateException("No active session")
            val currentOptions = captureOptions ?: throw IllegalStateException("No capture options")

            frameCount++

            // Render overlays if configured
            val overlays = currentOptions.overlays
            if (!overlays.isNullOrEmpty()) {
                overlayRenderer.renderOverlays(
                    bitmap = bitmap,
                    overlays = overlays,
                    frameNumber = frameCount - 1,
                    sessionId = currentSessionId
                )
            }

            // Check storage space and emit warning if low
            val threshold = currentOptions.advanced.storage.warningThreshold
            storageManager.isStorageAvailable(threshold)

            // Save frame (temp or permanent based on saveFrames option)
            val frameInfo = storageManager.saveFrame(
                bitmap = bitmap,
                sessionId = currentSessionId,
                frameNumber = frameCount - 1,
                options = currentOptions
            )

            // Emit frame captured event
            eventEmitterManager.emitFrameCaptured(frameInfo, frameCount - 1)

        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Checks if a frame should be captured based on throttling logic
     *
     * Enforces minimum interval between captures to prevent excessive frame rates.
     *
     * @return true if enough time has passed since last capture, false otherwise
     */
    private fun shouldCapture(): Boolean {
        val currentTime = System.currentTimeMillis()
        val interval = captureOptions?.interval ?: Constants.DEFAULT_INTERVAL
        val timeSinceLastCapture = currentTime - lastCaptureTime

        // Throttle: ensure minimum interval has passed
        return timeSinceLastCapture >= interval
    }

    /**
     * Captures a single frame from the ImageReader
     *
     * Acquires the latest image and submits it to the background executor
     * for processing (bitmap conversion, overlay rendering, storage).
     */
    private fun captureFrame() {
        try {
            // Acquire the latest image from ImageReader
            val image = imageReader?.acquireLatestImage()

            if (image != null) {
                // Update last capture time
                lastCaptureTime = System.currentTimeMillis()

                // Submit image processing to background executor
                processingExecutor.execute {
                    try {
                        processImage(image)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing image", e)
                        handleError(e)
                    } finally {
                        // Always close the image to release the buffer
                        image.close()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error capturing frame", e)
            handleError(e)
        }
    }



    /**
     * Processes a captured image: converts to Bitmap and saves to storage
     *
     * Runs on background thread to avoid blocking capture. Performs:
     * 1. Image to Bitmap conversion (with cropping if configured)
     * 2. Overlay rendering (if configured)
     * 3. Storage (temp or permanent based on saveFrames option)
     * 4. Event emission to JavaScript
     *
     * @param image The captured Image from ImageReader
     */
    private fun processImage(image: Image) {
        var bitmap: Bitmap? = null

        try {
            // Convert Image to Bitmap
            bitmap = bitmapProcessor.imageToBitmap(image, captureOptions)

            // Get current session info
            val currentSessionId = sessionId ?: throw IllegalStateException("No active session")
            val currentOptions = captureOptions ?: throw IllegalStateException("No capture options")

            // Increment frame count
            frameCount++

            // Render overlays if configured
            val overlays = currentOptions.overlays
            if (!overlays.isNullOrEmpty()) {
                overlayRenderer.renderOverlays(
                    bitmap = bitmap,
                    overlays = overlays,
                    frameNumber = frameCount - 1,
                    sessionId = currentSessionId
                )
            }

            // Check storage space and emit warning if low (doesn't block save)
            val threshold = currentOptions.advanced.storage.warningThreshold
            storageManager.isStorageAvailable(threshold)

            // Save frame (temp or permanent based on saveFrames option)
            val frameInfo = storageManager.saveFrame(
                bitmap = bitmap,
                sessionId = currentSessionId,
                frameNumber = frameCount - 1,
                options = currentOptions
            )

            // Emit frame captured event to JavaScript
            eventEmitterManager.emitFrameCaptured(frameInfo, frameCount - 1)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image", e)
            handleError(e)
        } finally {
            // Always recycle bitmap to free memory
            bitmap?.recycle()
        }
    }



    /**
     * Starts the capture session
     *
     * Sets up VirtualDisplay and ImageReader, then begins capture based on configured mode.
     * Supports interval-based capture and change-detection capture.
     *
     * @throws IllegalStateException if already capturing or not initialized
     */
    fun start() {
        try {
            // Check if already capturing
            if (isCapturing) {
                throw IllegalStateException("Capture already in progress")
            }

            // Check if initialized
            if (mediaProjection == null || captureOptions == null) {
                throw IllegalStateException("CaptureManager not initialized")
            }

            // Set state
            isCapturing = true
            isPaused = false
            frameCount = 0
            lastCaptureTime = 0

            // Get screen metrics
            val metrics = getScreenMetrics()

            // Setup ImageReader
            setupImageReader(metrics.width, metrics.height)

            // Create VirtualDisplay
            createVirtualDisplay(metrics)

            // Start capture based on mode
            val options = captureOptions!!
            when (options.captureMode) {
                CaptureMode.CHANGE_DETECTION -> {
                    // Initialize change detector
                    val changeConfig = options.changeDetection
                    changeDetector = ChangeDetector(
                        threshold = changeConfig?.threshold ?: Constants.DEFAULT_CHANGE_THRESHOLD,
                        sampleRate = changeConfig?.sampleRate ?: Constants.DEFAULT_CHANGE_SAMPLE_RATE,
                        detectionRegion = changeConfig?.detectionRegion
                    )
                    startChangeDetectionCapture()
                }
                else -> {
                    // Default to interval mode
                    startPeriodicCapture()
                }
            }

            // Emit capture start event to JavaScript
            val currentSessionId = sessionId ?: throw IllegalStateException("No session ID")
            val currentOptions = captureOptions ?: throw IllegalStateException("No capture options")
            eventEmitterManager.emitCaptureStart(currentSessionId, currentOptions)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture", e)
            isCapturing = false
            cleanup()
            throw e
        }
    }

    /**
     * Stops the capture session
     *
     * Stops periodic capture, emits final statistics (total frames, duration),
     * and triggers cleanup of all resources.
     */
    fun stop() {
        try {
            if (!isCapturing) {
                return
            }

            // Set state
            isCapturing = false

            // Stop periodic capture
            stopPeriodicCapture()

            // Emit capture stop event with statistics to JavaScript
            val currentSessionId = sessionId ?: ""
            val session = getCurrentSession()
            val duration = if (session != null) {
                System.currentTimeMillis() - session.startTime
            } else {
                0L
            }
            eventEmitterManager.emitCaptureStop(currentSessionId, frameCount, duration)

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture", e)
            handleError(e)
        } finally {
            // Always cleanup resources
            cleanup()
        }
    }

    /**
     * Pauses the capture session
     *
     * Stops capturing frames but keeps MediaProjection and VirtualDisplay active.
     * The session can be resumed without re-initialization.
     *
     * @throws IllegalStateException if no active capture session
     */
    fun pause() {
        try {
            if (!isCapturing) {
                throw IllegalStateException("No active capture session to pause")
            }

            if (isPaused) {
                return
            }

            // Set paused state
            isPaused = true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause capture", e)
            throw e
        }
    }

    /**
     * Resumes a paused capture session
     *
     * Restarts frame capture at the configured interval. Resets the last capture
     * time to avoid immediate capture on resume.
     *
     * @throws IllegalStateException if no active capture session or not paused
     */
    fun resume() {
        try {
            if (!isCapturing) {
                throw IllegalStateException("No active capture session to resume")
            }

            if (!isPaused) {
                return
            }

            // Clear paused state
            isPaused = false

            // Reset last capture time to avoid immediate capture
            lastCaptureTime = System.currentTimeMillis()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume capture", e)
            throw e
        }
    }

    /**
     * Gets the current capture status
     *
     * @return CaptureStatus with current state (IDLE/CAPTURING/PAUSED) and session info
     */
    fun getStatus(): CaptureStatus {
        val state = when {
            !isCapturing -> CaptureState.IDLE
            isPaused -> CaptureState.PAUSED
            else -> CaptureState.CAPTURING
        }

        val session = if (isCapturing && sessionId != null && captureOptions != null) {
            CaptureSession(
                id = sessionId!!,
                startTime = System.currentTimeMillis(),
                frameCount = frameCount,
                options = captureOptions!!
            )
        } else {
            null
        }

        return CaptureStatus(
            state = state,
            session = session,
            isPaused = isPaused
        )
    }



    /**
     * Cleans up all resources
     *
     * Releases VirtualDisplay, ImageReader, MediaProjection, shuts down executors,
     * clears overlay caches, and resets all state variables. Safe to call multiple times.
     */
    fun cleanup() {
        try {
            // Stop periodic capture
            stopPeriodicCapture()

            // Release VirtualDisplay
            try {
                virtualDisplay?.release()
                virtualDisplay = null
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing VirtualDisplay", e)
            }

            // Close ImageReader
            try {
                imageReader?.close()
                imageReader = null
            } catch (e: Exception) {
                Log.e(TAG, "Error closing ImageReader", e)
            }

            // Unregister callback and stop MediaProjection
            try {
                mediaProjection?.unregisterCallback(projectionCallback)
                mediaProjection?.stop()
                mediaProjection = null
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MediaProjection", e)
            }

            // Shutdown ExecutorService with timeout
            try {
                processingExecutor.shutdown()

                val shutdownTimeout = captureOptions?.advanced?.performance?.executorShutdownTimeout
                    ?: Constants.DEFAULT_EXECUTOR_SHUTDOWN_TIMEOUT
                val forcedTimeout = captureOptions?.advanced?.performance?.executorForcedShutdownTimeout
                    ?: Constants.DEFAULT_EXECUTOR_FORCED_SHUTDOWN_TIMEOUT

                if (!processingExecutor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) {
                    processingExecutor.shutdownNow()

                    // Wait a bit more for forced shutdown
                    if (!processingExecutor.awaitTermination(forcedTimeout, TimeUnit.MILLISECONDS)) {
                        Log.e(TAG, "ExecutorService did not terminate after forced shutdown")
                    }
                }
            } catch (e: InterruptedException) {
                Log.e(TAG, "Interrupted while shutting down ExecutorService", e)
                processingExecutor.shutdownNow()
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                Log.e(TAG, "Error shutting down ExecutorService", e)
            }

            // Clear overlay caches
            try {
                overlayRenderer.clearCaches()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing overlay caches", e)
            }

            // Clear change detector
            try {
                changeDetector?.clear()
                changeDetector = null
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing change detector", e)
            }

            // Clear all state variables
            captureOptions = null
            sessionId = null
            frameCount = 0
            isCapturing = false
            isPaused = false
            lastCaptureTime = 0
            captureRunnable = null

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    /**
     * Releases the capture thread
     *
     * Should be called when the CaptureManager is no longer needed (e.g., module
     * invalidation). Performs cleanup and quits the background capture thread.
     */
    fun release() {
        try {
            // Cleanup resources first
            cleanup()

            // Quit the capture thread
            captureThread.quitSafely()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing CaptureManager", e)
        }
    }

    /**
     * Handles errors that occur during capture operations
     *
     * Classifies exceptions into appropriate error codes, emits error events
     * to JavaScript, and performs cleanup if capture is active.
     *
     * @param error The exception that occurred
     */
    private fun handleError(error: Exception) {
        try {
            // Classify the error and determine error code
            val (errorCode, errorMessage, errorDetails) = classifyError(error)

            Log.e(TAG, "Capture error: $errorMessage (code: ${errorCode.value})", error)

            // Emit error event
            eventEmitterManager.emitError(errorCode, errorMessage, errorDetails)

            // Cleanup resources on error
            if (isCapturing) {
                isCapturing = false
                cleanup()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in error handler", e)
        }
    }

    /**
     * Classifies an exception into an error code, message, and details
     *
     * Maps common exceptions to appropriate ErrorCode values and provides
     * contextual details for debugging.
     *
     * @param error The exception to classify
     * @return Triple of (ErrorCode, error message, optional details map)
     */
    private fun classifyError(error: Exception): Triple<ErrorCode, String, Map<String, Any>?> {
        return when (error) {
            is SecurityException -> Triple(
                ErrorCode.PERMISSION_DENIED,
                "Permission denied: ${error.message}",
                mapOf(Constants.ERROR_DETAIL_PERMISSION to "MEDIA_PROJECTION")
            )

            is IllegalStateException -> Triple(
                ErrorCode.ALREADY_CAPTURING,
                "Invalid state: ${error.message}",
                mapOf(Constants.ERROR_DETAIL_CURRENT_STATE to getStatus().state.value)
            )

            is IllegalArgumentException -> Triple(
                ErrorCode.INVALID_OPTIONS,
                "Invalid configuration: ${error.message}",
                null
            )

            is java.io.IOException -> Triple(
                ErrorCode.STORAGE_ERROR,
                "Storage error: ${error.message}",
                mapOf(Constants.ERROR_DETAIL_AVAILABLE_SPACE to storageManager.checkStorageSpace())
            )

            is OutOfMemoryError -> Triple(
                ErrorCode.SYSTEM_ERROR,
                "Out of memory during capture",
                mapOf(
                    Constants.ERROR_DETAIL_HEAP_SIZE to Runtime.getRuntime().maxMemory(),
                    Constants.ERROR_DETAIL_USED_MEMORY to (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                )
            )

            else -> Triple(
                ErrorCode.SYSTEM_ERROR,
                "Unexpected error: ${error.message ?: error.javaClass.simpleName}",
                mapOf(Constants.ERROR_DETAIL_ERROR_TYPE to error.javaClass.simpleName)
            )
        }
    }
}
