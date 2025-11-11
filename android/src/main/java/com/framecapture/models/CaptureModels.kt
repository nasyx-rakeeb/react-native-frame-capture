package com.framecapture.models

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

/**
 * Custom capture region specification
 */
data class CaptureRegion(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val unit: String = com.framecapture.Constants.POSITION_UNIT_PERCENTAGE // "pixels" or "percentage"
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): CaptureRegion? {
            if (map == null) return null

            return CaptureRegion(
                x = if (map.hasKey("x")) map.getDouble("x").toFloat() else 0f,
                y = if (map.hasKey("y")) map.getDouble("y").toFloat() else 0f,
                width = if (map.hasKey("width")) map.getDouble("width").toFloat() else 1f,
                height = if (map.hasKey("height")) map.getDouble("height").toFloat() else 1f,
                unit = if (map.hasKey("unit")) map.getString("unit") ?: com.framecapture.Constants.POSITION_UNIT_PERCENTAGE else com.framecapture.Constants.POSITION_UNIT_PERCENTAGE
            )
        }
    }
}

/**
 * Storage configuration
 */
data class StorageConfig(
    val warningThreshold: Long = com.framecapture.Constants.DEFAULT_STORAGE_WARNING_THRESHOLD
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): StorageConfig {
            if (map == null) return StorageConfig()

            return StorageConfig(
                warningThreshold = if (map.hasKey("warningThreshold")) map.getDouble("warningThreshold").toLong() else com.framecapture.Constants.DEFAULT_STORAGE_WARNING_THRESHOLD
            )
        }
    }
}



/**
 * File naming configuration
 */
data class FileNamingConfig(
    val prefix: String = com.framecapture.Constants.DEFAULT_FILENAME_PREFIX,
    val dateFormat: String = com.framecapture.Constants.DEFAULT_FILENAME_DATE_FORMAT,
    val framePadding: Int = com.framecapture.Constants.DEFAULT_FILENAME_FRAME_PADDING
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): FileNamingConfig {
            if (map == null) return FileNamingConfig()

            return FileNamingConfig(
                prefix = map.getStringOrDefault("prefix", com.framecapture.Constants.DEFAULT_FILENAME_PREFIX),
                dateFormat = map.getStringOrDefault("dateFormat", com.framecapture.Constants.DEFAULT_FILENAME_DATE_FORMAT),
                framePadding = map.getIntOrDefault("framePadding", com.framecapture.Constants.DEFAULT_FILENAME_FRAME_PADDING)
            )
        }
    }
}

/**
 * Performance configuration
 */
data class PerformanceConfig(
    val overlayCacheSize: Int = com.framecapture.Constants.DEFAULT_OVERLAY_IMAGE_CACHE_SIZE,
    val imageReaderBuffers: Int = com.framecapture.Constants.DEFAULT_IMAGE_READER_MAX_IMAGES,
    val executorShutdownTimeout: Long = com.framecapture.Constants.DEFAULT_EXECUTOR_SHUTDOWN_TIMEOUT,
    val executorForcedShutdownTimeout: Long = com.framecapture.Constants.DEFAULT_EXECUTOR_FORCED_SHUTDOWN_TIMEOUT
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): PerformanceConfig {
            if (map == null) return PerformanceConfig()

            return PerformanceConfig(
                overlayCacheSize = if (map.hasKey("overlayCacheSize")) map.getDouble("overlayCacheSize").toInt() else com.framecapture.Constants.DEFAULT_OVERLAY_IMAGE_CACHE_SIZE,
                imageReaderBuffers = map.getIntOrDefault("imageReaderBuffers", com.framecapture.Constants.DEFAULT_IMAGE_READER_MAX_IMAGES),
                executorShutdownTimeout = if (map.hasKey("executorShutdownTimeout")) map.getDouble("executorShutdownTimeout").toLong() else com.framecapture.Constants.DEFAULT_EXECUTOR_SHUTDOWN_TIMEOUT,
                executorForcedShutdownTimeout = if (map.hasKey("executorForcedShutdownTimeout")) map.getDouble("executorForcedShutdownTimeout").toLong() else com.framecapture.Constants.DEFAULT_EXECUTOR_FORCED_SHUTDOWN_TIMEOUT
            )
        }
    }
}

/**
 * Advanced configuration options
 */
data class AdvancedConfig(
    val storage: StorageConfig = StorageConfig(),
    val fileNaming: FileNamingConfig = FileNamingConfig(),
    val performance: PerformanceConfig = PerformanceConfig()
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): AdvancedConfig {
            if (map == null) return AdvancedConfig()

            return AdvancedConfig(
                storage = StorageConfig.fromReadableMap(map.getMap("storage")),
                fileNaming = FileNamingConfig.fromReadableMap(map.getMap("fileNaming")),
                performance = PerformanceConfig.fromReadableMap(map.getMap("performance"))
            )
        }
    }
}

/**
 * Configuration options for notification customization
 */
data class NotificationOptions(
    val title: String = com.framecapture.Constants.NOTIFICATION_DEFAULT_TITLE,
    val description: String = com.framecapture.Constants.NOTIFICATION_DEFAULT_DESCRIPTION,
    val icon: String? = null,
    val smallIcon: String? = null,
    val color: String? = null,
    val channelName: String = com.framecapture.Constants.CHANNEL_NAME,
    val channelDescription: String = com.framecapture.Constants.CHANNEL_DESCRIPTION,
    val priority: String = com.framecapture.Constants.NOTIFICATION_PRIORITY_LOW,
    val showFrameCount: Boolean = true,
    val updateInterval: Int = com.framecapture.Constants.NOTIFICATION_DEFAULT_UPDATE_INTERVAL,
    val pausedTitle: String = com.framecapture.Constants.NOTIFICATION_DEFAULT_PAUSED_TITLE,
    val pausedDescription: String = com.framecapture.Constants.NOTIFICATION_DEFAULT_PAUSED_DESCRIPTION,
    val showStopAction: Boolean = true,
    val showPauseAction: Boolean = false,
    val showResumeAction: Boolean = true
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): NotificationOptions {
            if (map == null) return NotificationOptions()

            return NotificationOptions(
                title = map.getStringOrDefault("title", com.framecapture.Constants.NOTIFICATION_DEFAULT_TITLE),
                description = map.getStringOrDefault("description", com.framecapture.Constants.NOTIFICATION_DEFAULT_DESCRIPTION),
                icon = map.getStringOrNull("icon"),
                smallIcon = map.getStringOrNull("smallIcon"),
                color = map.getStringOrNull("color"),
                channelName = map.getStringOrDefault("channelName", com.framecapture.Constants.CHANNEL_NAME),
                channelDescription = map.getStringOrDefault("channelDescription", com.framecapture.Constants.CHANNEL_DESCRIPTION),
                priority = map.getStringOrDefault("priority", com.framecapture.Constants.NOTIFICATION_PRIORITY_LOW),
                showFrameCount = map.getBooleanOrDefault("showFrameCount", true),
                updateInterval = map.getIntOrDefault("updateInterval", com.framecapture.Constants.NOTIFICATION_DEFAULT_UPDATE_INTERVAL),
                pausedTitle = map.getStringOrDefault("pausedTitle", com.framecapture.Constants.NOTIFICATION_DEFAULT_PAUSED_TITLE),
                pausedDescription = map.getStringOrDefault("pausedDescription", com.framecapture.Constants.NOTIFICATION_DEFAULT_PAUSED_DESCRIPTION),
                showStopAction = map.getBooleanOrDefault("showStopAction", true),
                showPauseAction = map.getBooleanOrDefault("showPauseAction", false),
                showResumeAction = map.getBooleanOrDefault("showResumeAction", true)
            )
        }
    }
}

/**
 * Configuration options for screen capture
 */
data class CaptureOptions(
    val interval: Long = com.framecapture.Constants.DEFAULT_INTERVAL,
    val quality: Int = com.framecapture.Constants.DEFAULT_QUALITY,
    val format: String = com.framecapture.Constants.DEFAULT_FORMAT,
    val saveFrames: Boolean = false,
    val storageLocation: String = com.framecapture.Constants.STORAGE_LOCATION_PRIVATE,
    val outputDirectory: String? = null,
    val scaleResolution: Float? = null,
    val captureRegion: CaptureRegion? = null,
    val excludeStatusBar: Boolean = false,
    val notification: NotificationOptions? = null,
    val overlays: List<OverlayConfig>? = null,
    val advanced: AdvancedConfig = AdvancedConfig()
) {
    companion object {
        fun fromReadableMap(map: ReadableMap): CaptureOptions {
            val notificationOptions = if (map.hasKey("notification")) {
                NotificationOptions.fromReadableMap(map.getMap("notification"))
            } else {
                null
            }

            val captureRegion = if (map.hasKey("captureRegion")) {
                CaptureRegion.fromReadableMap(map.getMap("captureRegion"))
            } else {
                null
            }

            val overlays = if (map.hasKey("overlays")) {
                val overlaysArray = map.getArray("overlays")
                if (overlaysArray != null) {
                    val overlayList = mutableListOf<OverlayConfig>()
                    for (i in 0 until overlaysArray.size()) {
                        val overlayMap = overlaysArray.getMap(i)
                        if (overlayMap != null) {
                            val overlay = OverlayConfig.fromReadableMap(overlayMap)
                            if (overlay != null) {
                                overlayList.add(overlay)
                            }
                        }
                    }
                    overlayList.ifEmpty { null }
                } else {
                    null
                }
            } else {
                null
            }

            val advancedConfig = if (map.hasKey("advanced")) {
                AdvancedConfig.fromReadableMap(map.getMap("advanced"))
            } else {
                AdvancedConfig()
            }

            return CaptureOptions(
                interval = if (map.hasKey("interval")) map.getDouble("interval").toLong() else com.framecapture.Constants.DEFAULT_INTERVAL,
                quality = if (map.hasKey("quality")) map.getInt("quality") else com.framecapture.Constants.DEFAULT_QUALITY,
                format = if (map.hasKey("format")) map.getString("format") ?: com.framecapture.Constants.DEFAULT_FORMAT else com.framecapture.Constants.DEFAULT_FORMAT,
                saveFrames = if (map.hasKey("saveFrames")) map.getBoolean("saveFrames") else false,
                storageLocation = if (map.hasKey("storageLocation")) map.getString("storageLocation") ?: com.framecapture.Constants.STORAGE_LOCATION_PRIVATE else com.framecapture.Constants.STORAGE_LOCATION_PRIVATE,
                outputDirectory = if (map.hasKey("outputDirectory")) map.getString("outputDirectory") else null,
                scaleResolution = if (map.hasKey("scaleResolution")) map.getDouble("scaleResolution").toFloat() else null,
                captureRegion = captureRegion,
                excludeStatusBar = if (map.hasKey("excludeStatusBar")) map.getBoolean("excludeStatusBar") else false,
                notification = notificationOptions,
                overlays = overlays,
                advanced = advancedConfig
            )
        }
    }

    fun toWritableMap(): WritableMap {
        return Arguments.createMap().apply {
            putDouble("interval", interval.toDouble())
            putInt("quality", quality)
            putString("format", format)
            putBoolean("saveFrames", saveFrames)
            putString("storageLocation", storageLocation)
            outputDirectory?.let { putString("outputDirectory", it) }
            scaleResolution?.let { putDouble("scaleResolution", it.toDouble()) }
            putBoolean("excludeStatusBar", excludeStatusBar)
        }
    }
}

/**
 * Represents an active capture session
 */
data class CaptureSession(
    val id: String,
    val startTime: Long,
    var frameCount: Int,
    val options: CaptureOptions
) {
    fun toWritableMap(): WritableMap {
        return Arguments.createMap().apply {
            putString("id", id)
            putDouble("startTime", startTime.toDouble())
            putInt("frameCount", frameCount)
            putMap("options", options.toWritableMap())
        }
    }
}

/**
 * Current status of the capture system
 */
data class CaptureStatus(
    val state: CaptureState,
    val session: CaptureSession?,
    val isPaused: Boolean
) {
    fun toWritableMap(): WritableMap {
        return Arguments.createMap().apply {
            putString("state", state.value)
            session?.let { putMap("session", it.toWritableMap()) }
            putBoolean("isPaused", isPaused)
        }
    }
}

/**
 * Information about a captured frame
 */
data class FrameInfo(
    val filePath: String,
    val fileSize: Long,
    val timestamp: Long
) {
    fun toWritableMap(frameNumber: Int): WritableMap {
        return Arguments.createMap().apply {
            putString("filePath", filePath)
            putDouble("fileSize", fileSize.toDouble())
            putDouble("timestamp", timestamp.toDouble())
            putInt("frameNumber", frameNumber)
        }
    }
}
