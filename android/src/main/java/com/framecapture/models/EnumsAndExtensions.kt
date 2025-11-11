package com.framecapture.models

import com.facebook.react.bridge.ReadableMap

/**
 * Helper extension functions for safe ReadableMap access
 */
fun ReadableMap.getStringOrDefault(key: String, default: String): String {
    return if (hasKey(key)) getString(key) ?: default else default
}

fun ReadableMap.getStringOrNull(key: String): String? {
    return if (hasKey(key)) getString(key) else null
}

fun ReadableMap.getBooleanOrDefault(key: String, default: Boolean): Boolean {
    return if (hasKey(key)) getBoolean(key) else default
}

fun ReadableMap.getIntOrDefault(key: String, default: Int): Int {
    return if (hasKey(key)) getInt(key) else default
}

/**
 * Capture mode determines when frames are captured
 */
enum class CaptureMode(val value: String) {
    INTERVAL("interval")      // Fixed interval capture
}

/**
 * Capture state enum
 */
enum class CaptureState(val value: String) {
    IDLE("idle"),
    CAPTURING("capturing"),
    PAUSED("paused"),
    STOPPING("stopping")
}

/**
 * Error codes for capture operations
 */
enum class ErrorCode(val value: String) {
    PERMISSION_DENIED("PERMISSION_DENIED"),
    ALREADY_CAPTURING("ALREADY_CAPTURING"),
    INVALID_OPTIONS("INVALID_OPTIONS"),
    STORAGE_ERROR("STORAGE_ERROR"),
    SYSTEM_ERROR("SYSTEM_ERROR"),
    NOT_SUPPORTED("NOT_SUPPORTED")
}

/**
 * Permission status enum
 */
enum class PermissionStatus(val value: String) {
    GRANTED("granted"),
    DENIED("denied"),
    NOT_DETERMINED("not_determined")
}
