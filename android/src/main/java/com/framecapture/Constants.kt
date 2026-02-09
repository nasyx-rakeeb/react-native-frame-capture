package com.framecapture

/**
 * Constants for the FrameCapture library
 *
 * Centralized configuration values including:
 * - Capture constraints (intervals, quality, formats)
 * - Storage thresholds
 * - Notification configuration
 * - Service actions and intent extras
 * - Event names for JavaScript communication
 * - Resource management timeouts
 */
object Constants {

    // Capture interval constraints (in milliseconds)
    const val MIN_INTERVAL = 100L
    const val MAX_INTERVAL = 60000L
    const val DEFAULT_INTERVAL = 1000L

    // Image quality constraints (0-100)
    const val MIN_QUALITY = 0
    const val MAX_QUALITY = 100
    const val DEFAULT_QUALITY = 80

    // Default image format
    const val DEFAULT_FORMAT = "jpeg"

    // Capture mode strings
    const val CAPTURE_MODE_INTERVAL = "interval"
    const val CAPTURE_MODE_CHANGE_DETECTION = "change-detection"

    // Change detection defaults
    const val DEFAULT_CHANGE_THRESHOLD = 5f              // 5% of pixels changed
    const val DEFAULT_CHANGE_MIN_INTERVAL = 500L         // 500ms minimum between captures
    const val DEFAULT_CHANGE_MAX_INTERVAL = 0L           // 0 = disabled (no forced captures)
    const val DEFAULT_CHANGE_SAMPLE_RATE = 16            // Sample every 16th pixel
    const val CHANGE_PIXEL_TOLERANCE = 10                // RGB difference tolerance for "changed" pixel

    // Storage warning threshold (100MB in bytes) - DEFAULT VALUE
    const val DEFAULT_STORAGE_WARNING_THRESHOLD = 100L * 1024L * 1024L

    // Storage location identifiers
    const val STORAGE_LOCATION_PUBLIC = "public"
    const val STORAGE_LOCATION_PRIVATE = "private"

    // File naming - DEFAULT VALUES
    const val DEFAULT_FILENAME_PREFIX = "capture_"
    const val DEFAULT_FILENAME_DATE_FORMAT = "yyyyMMdd_HHmmss_SSS"
    const val DEFAULT_FILENAME_FRAME_PADDING = 5 // Number of digits for frame number

    // Image format extensions
    const val FORMAT_EXTENSION_PNG = "png"
    const val FORMAT_EXTENSION_JPEG = "jpg"

    // MIME types
    const val MIME_TYPE_PNG = "image/png"
    const val MIME_TYPE_JPEG = "image/jpeg"

    // Storage directories
    const val TEMP_FRAMES_DIRECTORY = "captured_frames"

    // Foreground service notification
    const val NOTIFICATION_ID = 1001
    const val CHANNEL_ID = "screen_capture_channel"
    const val CHANNEL_NAME = "Screen Capture"
    const val CHANNEL_DESCRIPTION = "Notifications for screen capture service"
    const val CUSTOM_CHANNEL_ID_PREFIX = "screen_capture_custom_"

    // Notification priority levels
    const val NOTIFICATION_PRIORITY_HIGH = "high"
    const val NOTIFICATION_PRIORITY_DEFAULT = "default"
    const val NOTIFICATION_PRIORITY_LOW = "low"

    // Notification action labels
    const val NOTIFICATION_ACTION_PAUSE = "Pause"
    const val NOTIFICATION_ACTION_RESUME = "Resume"
    const val NOTIFICATION_ACTION_STOP = "Stop"

    // Notification default values
    const val NOTIFICATION_DEFAULT_TITLE = "Screen Capture Active"
    const val NOTIFICATION_DEFAULT_DESCRIPTION = "Capturing frames..."
    const val NOTIFICATION_DEFAULT_PAUSED_TITLE = "Screen Capture Paused"
    const val NOTIFICATION_DEFAULT_PAUSED_DESCRIPTION = "{frameCount} frames captured"
    const val NOTIFICATION_DEFAULT_UPDATE_INTERVAL = 10

    // Notification action request codes
    const val NOTIFICATION_REQUEST_CODE_STOP = 0
    const val NOTIFICATION_REQUEST_CODE_PAUSE_RESUME = 1

    // Notification template variable
    const val NOTIFICATION_TEMPLATE_FRAME_COUNT = "{frameCount}"

    // Service actions
    const val ACTION_START_CAPTURE = "com.framecapture.START_CAPTURE"
    const val ACTION_STOP_CAPTURE = "com.framecapture.STOP_CAPTURE"
    const val ACTION_PAUSE_CAPTURE = "com.framecapture.PAUSE_CAPTURE"
    const val ACTION_RESUME_CAPTURE = "com.framecapture.RESUME_CAPTURE"

    // Intent extras
    const val EXTRA_CAPTURE_OPTIONS = "capture_options"
    const val EXTRA_PROJECTION_DATA = "projection_data"

    // MediaProjection request code
    const val REQUEST_MEDIA_PROJECTION = 1001

    // Module name
    const val MODULE_NAME = "FrameCapture"

    // Event names
    const val EVENT_FRAME_CAPTURED = "onFrameCaptured"
    const val EVENT_CAPTURE_ERROR = "onCaptureError"
    const val EVENT_CAPTURE_STOP = "onCaptureStop"
    const val EVENT_CAPTURE_START = "onCaptureStart"
    const val EVENT_STORAGE_WARNING = "onStorageWarning"
    const val EVENT_CAPTURE_PAUSE = "onCapturePause"
    const val EVENT_CAPTURE_RESUME = "onCaptureResume"
    const val EVENT_OVERLAY_ERROR = "onOverlayError"
    const val EVENT_CHANGE_DETECTED = "onChangeDetected"

    // Resource cleanup timeout (in milliseconds) - DEFAULT VALUES
    const val DEFAULT_EXECUTOR_SHUTDOWN_TIMEOUT = 5000L
    const val DEFAULT_EXECUTOR_FORCED_SHUTDOWN_TIMEOUT = 1000L

    // ImageReader buffer count - DEFAULT VALUE
    const val DEFAULT_IMAGE_READER_MAX_IMAGES = 2

    // Image format constants
    const val RGBA_BYTES_PER_PIXEL = 4

    // Resolution scaling constraints
    const val MIN_SCALE_RESOLUTION = 0.1f
    const val MAX_SCALE_RESOLUTION = 1.0f

    // Overlay rendering - DEFAULT VALUES
    const val DEFAULT_OVERLAY_IMAGE_CACHE_SIZE = 10 * 1024 * 1024 // 10MB in bytes
    const val OVERLAY_DEFAULT_PADDING = 10 // Padding from edges in pixels
    const val MIN_OVERLAY_OPACITY = 0.0f
    const val MAX_OVERLAY_OPACITY = 1.0f

    // Overlay types
    const val OVERLAY_TYPE_TEXT = "text"
    const val OVERLAY_TYPE_IMAGE = "image"

    // Overlay position presets
    const val POSITION_TOP_LEFT = "top-left"
    const val POSITION_TOP_RIGHT = "top-right"
    const val POSITION_BOTTOM_LEFT = "bottom-left"
    const val POSITION_BOTTOM_RIGHT = "bottom-right"
    const val POSITION_CENTER = "center"

    // Overlay position units
    const val POSITION_UNIT_PERCENTAGE = "percentage"
    const val POSITION_UNIT_PIXELS = "pixels"

    // Template variables
    const val TEMPLATE_VAR_FRAME_NUMBER = "{frameNumber}"
    const val TEMPLATE_VAR_SESSION_ID = "{sessionId}"
    const val TEMPLATE_VAR_TIMESTAMP = "{timestamp}"

    // Timestamp format for overlays
    const val OVERLAY_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    const val OVERLAY_TIMESTAMP_TIMEZONE = "UTC"

    // Text style values
    const val TEXT_WEIGHT_BOLD = "bold"
    const val TEXT_WEIGHT_NORMAL = "normal"
    const val TEXT_ALIGN_LEFT = "left"
    const val TEXT_ALIGN_CENTER = "center"
    const val TEXT_ALIGN_RIGHT = "right"

    // Text style defaults
    const val TEXT_DEFAULT_FONT_SIZE = 14
    const val TEXT_DEFAULT_COLOR = "#FFFFFF"
    const val TEXT_DEFAULT_BACKGROUND_COLOR = "#00000080"
    const val TEXT_DEFAULT_PADDING = 8

    // Image overlay defaults
    const val IMAGE_DEFAULT_OPACITY = 1.0f

    // URI schemes
    const val URI_SCHEME_FILE = "file"
    const val URI_SCHEME_CONTENT = "content"

    // Resource types
    const val RESOURCE_TYPE_DRAWABLE = "drawable"

    // Hex color validation
    const val HEX_COLOR_LENGTH_SHORT = 3  // #RGB
    const val HEX_COLOR_LENGTH_MEDIUM = 6 // #RRGGBB
    const val HEX_COLOR_LENGTH_LONG = 8   // #AARRGGBB

    // Error detail keys
    const val ERROR_DETAIL_PERMISSION = "permission"
    const val ERROR_DETAIL_CURRENT_STATE = "currentState"
    const val ERROR_DETAIL_AVAILABLE_SPACE = "availableSpace"
    const val ERROR_DETAIL_HEAP_SIZE = "heapSize"
    const val ERROR_DETAIL_USED_MEMORY = "usedMemory"
    const val ERROR_DETAIL_ERRORS = "errors"
    const val ERROR_DETAIL_ERROR_TYPE = "errorType"

    // Event payload keys
    const val EVENT_KEY_AVAILABLE_SPACE = "availableSpace"
    const val EVENT_KEY_THRESHOLD = "threshold"
}
