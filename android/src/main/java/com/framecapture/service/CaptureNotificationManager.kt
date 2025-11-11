package com.framecapture.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.framecapture.Constants
import com.framecapture.ScreenCaptureService
import com.framecapture.models.NotificationOptions

/**
 * Manages notification creation, updates, and styling for the capture service
 *
 * Handles all notification-related operations for the foreground service:
 * - Notification channel creation (Android 8.0+)
 * - Custom styling (colors, icons, priority)
 * - Action buttons (pause/resume/stop)
 * - Frame count updates with template substitution
 * - Paused state notifications
 *
 * Supports full customization via NotificationOptions.
 */
class CaptureNotificationManager(
    private val service: Service,
    private val notificationOptions: NotificationOptions
) {

    companion object {
        const val NOTIFICATION_ID = Constants.NOTIFICATION_ID
    }

    /**
     * Resolves an icon resource name to a resource ID
     * Falls back to default icon if resource not found or null
     */
    private fun getIconResource(iconName: String?): Int {
        if (iconName == null) {
            return android.R.drawable.ic_menu_camera
        }

        try {
            val resourceId = service.applicationContext.resources.getIdentifier(
                iconName,
                Constants.RESOURCE_TYPE_DRAWABLE,
                service.applicationContext.packageName
            )

            if (resourceId == 0) {
                return android.R.drawable.ic_menu_camera
            }

            return resourceId
        } catch (e: Exception) {
            return android.R.drawable.ic_menu_camera
        }
    }

    /**
     * Parses a color hex string to a color integer
     * Returns null if parsing fails or input is null
     */
    private fun parseColor(colorString: String?): Int? {
        if (colorString == null) return null

        try {
            return android.graphics.Color.parseColor(colorString)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Maps a priority string to a NotificationCompat priority constant
     * Supports "low", "default", "high" (case-insensitive)
     */
    private fun getNotificationPriority(priority: String): Int {
        return when (priority.lowercase()) {
            Constants.NOTIFICATION_PRIORITY_HIGH -> NotificationCompat.PRIORITY_HIGH
            Constants.NOTIFICATION_PRIORITY_DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            Constants.NOTIFICATION_PRIORITY_LOW -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_LOW
        }
    }

    /**
     * Formats a description template by replacing {frameCount} placeholder with actual count
     */
    private fun formatDescription(template: String, frameCount: Int): String {
        return template.replace(Constants.NOTIFICATION_TEMPLATE_FRAME_COUNT, frameCount.toString())
    }

    /**
     * Generates a unique channel ID based on notification options
     * Returns default CHANNEL_ID for default settings, or hash-based ID for custom channels
     */
    private fun getChannelId(): String {
        return if (notificationOptions.channelName == Constants.CHANNEL_NAME) {
            Constants.CHANNEL_ID
        } else {
            "${Constants.CUSTOM_CHANNEL_ID_PREFIX}${notificationOptions.channelName.hashCode()}"
        }
    }

    /**
     * Creates notification channel for API 26+
     * Uses custom channel settings from notificationOptions
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getChannelId()

            val channel = NotificationChannel(
                channelId,
                notificationOptions.channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = notificationOptions.channelDescription
                setShowBadge(false)
            }

            val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates a reusable notification builder with custom styling applied
     */
    private fun createNotificationBuilder(): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(service, getChannelId())
            .setSmallIcon(getIconResource(notificationOptions.smallIcon))
            .setPriority(getNotificationPriority(notificationOptions.priority))
            .setOngoing(true)

        // Apply color if provided
        parseColor(notificationOptions.color)?.let {
            builder.setColor(it)
        }

        // Apply large icon if provided
        notificationOptions.icon?.let { iconName ->
            try {
                val iconRes = getIconResource(iconName)
                val bitmap = android.graphics.BitmapFactory.decodeResource(service.resources, iconRes)
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                }
            } catch (e: Exception) {
                // Silently fail - large icon is optional
            }
        }

        return builder
    }

    /**
     * Creates a PendingIntent for service actions
     */
    private fun createActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(service, ScreenCaptureService::class.java).apply {
            this.action = action
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getService(
                service,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getService(
                service,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    /**
     * Creates the initial foreground service notification
     */
    fun createNotification(): Notification {
        val builder = createNotificationBuilder()
            .setContentTitle(notificationOptions.title)
            .setContentText(notificationOptions.description)

        // Add pause action if enabled
        if (notificationOptions.showPauseAction) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                Constants.NOTIFICATION_ACTION_PAUSE,
                createActionPendingIntent(Constants.ACTION_PAUSE_CAPTURE, Constants.NOTIFICATION_REQUEST_CODE_PAUSE_RESUME)
            )
        }

        // Add stop action if enabled
        if (notificationOptions.showStopAction) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                Constants.NOTIFICATION_ACTION_STOP,
                createActionPendingIntent(Constants.ACTION_STOP_CAPTURE, Constants.NOTIFICATION_REQUEST_CODE_STOP)
            )
        }

        return builder.build()
    }

    /**
     * Updates the notification with current frame count progress
     */
    fun updateNotification(frameCount: Int) {
        try {
            val builder = createNotificationBuilder()
                .setContentTitle(notificationOptions.title)
                .setContentText(formatDescription(notificationOptions.description, frameCount))

            // Add pause action if enabled
            if (notificationOptions.showPauseAction) {
                builder.addAction(
                    android.R.drawable.ic_media_pause,
                    Constants.NOTIFICATION_ACTION_PAUSE,
                    createActionPendingIntent(Constants.ACTION_PAUSE_CAPTURE, Constants.NOTIFICATION_REQUEST_CODE_PAUSE_RESUME)
                )
            }

            // Add stop action if enabled
            if (notificationOptions.showStopAction) {
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    Constants.NOTIFICATION_ACTION_STOP,
                    createActionPendingIntent(Constants.ACTION_STOP_CAPTURE, Constants.NOTIFICATION_REQUEST_CODE_STOP)
                )
            }

            val notification = builder.build()
            val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            // Silently fail - notification update is not critical
        }
    }

    /**
     * Updates the notification to show paused state
     */
    fun updateNotificationPaused(frameCount: Int) {
        try {
            val builder = createNotificationBuilder()
                .setContentTitle(notificationOptions.pausedTitle)
                .setContentText(formatDescription(notificationOptions.pausedDescription, frameCount))

            // Add resume action if enabled
            if (notificationOptions.showResumeAction) {
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    Constants.NOTIFICATION_ACTION_RESUME,
                    createActionPendingIntent(Constants.ACTION_RESUME_CAPTURE, Constants.NOTIFICATION_REQUEST_CODE_PAUSE_RESUME)
                )
            }

            // Add stop action if enabled
            if (notificationOptions.showStopAction) {
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    Constants.NOTIFICATION_ACTION_STOP,
                    createActionPendingIntent(Constants.ACTION_STOP_CAPTURE, Constants.NOTIFICATION_REQUEST_CODE_STOP)
                )
            }

            val notification = builder.build()
            val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            // Silently fail - notification update is not critical
        }
    }
}
