package com.framecapture

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import com.facebook.react.bridge.Promise
import com.framecapture.models.ErrorCode
import com.framecapture.models.PermissionStatus

/**
 * Handles MediaProjection permission requests and status checks
 *
 * Manages the complete MediaProjection permission flow:
 * - Requests permission via system dialog
 * - Handles activity result callbacks
 * - Stores permission data for later use
 * - Checks permission status
 *
 * Note: MediaProjection permission cannot be checked programmatically on Android.
 * This handler only tracks whether permission data has been granted and stored.
 */
class PermissionHandler(
    private val getActivity: () -> Activity?
) {

    // Permission request promise
    private var permissionPromise: Promise? = null

    // Stored projection data after permission granted
    var mediaProjectionData: Intent? = null
        private set

    /**
     * Requests MediaProjection permission from the user
     *
     * Opens the Android system permission dialog for screen capture access.
     * The result is handled asynchronously via handleActivityResult().
     *
     * @param promise Resolves with PermissionStatus.GRANTED or rejects with error
     */
    fun requestPermission(promise: Promise) {
        try {
            val activity = getActivity()
            if (activity == null) {
                promise.reject(
                    ErrorCode.SYSTEM_ERROR.value,
                    "Activity is null, cannot request permission"
                )
                return
            }

            // Check if already requesting permission
            if (permissionPromise != null) {
                promise.reject(
                    ErrorCode.SYSTEM_ERROR.value,
                    "Permission request already in progress"
                )
                return
            }

            // Store promise for later resolution
            permissionPromise = promise

            // Create MediaProjection intent
            val mediaProjectionManager = activity.getSystemService(
                Activity.MEDIA_PROJECTION_SERVICE
            ) as? MediaProjectionManager

            if (mediaProjectionManager == null) {
                permissionPromise = null
                promise.reject(
                    ErrorCode.NOT_SUPPORTED.value,
                    "MediaProjection service not available"
                )
                return
            }

            val intent = mediaProjectionManager.createScreenCaptureIntent()
            activity.startActivityForResult(intent, Constants.REQUEST_MEDIA_PROJECTION, null)

        } catch (e: Exception) {
            permissionPromise = null
            throw e
        }
    }

    /**
     * Checks current permission status
     *
     * Note: MediaProjection permission cannot be checked programmatically on Android.
     * This only verifies if permission data has been stored from a previous grant.
     *
     * @return PermissionStatus.GRANTED if data exists, NOT_DETERMINED otherwise
     */
    fun checkPermission(): PermissionStatus {
        return if (mediaProjectionData != null) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.NOT_DETERMINED
        }
    }

    /**
     * Handles activity result from permission request
     *
     * Should be called from the activity event listener. Resolves or rejects
     * the stored promise based on the user's permission decision.
     *
     * @param requestCode Activity request code
     * @param resultCode Activity result code (RESULT_OK or RESULT_CANCELED)
     * @param data Intent containing MediaProjection permission data
     * @return true if this was a MediaProjection request, false otherwise
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != Constants.REQUEST_MEDIA_PROJECTION) {
            return false
        }

        val promise = permissionPromise
        permissionPromise = null

        if (promise == null) {
            // No promise stored - result already handled or unexpected callback
            return true
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            // Store projection data for later use
            mediaProjectionData = data
            promise.resolve(PermissionStatus.GRANTED.value)
        } else {
            // User denied permission
            promise.reject(
                ErrorCode.PERMISSION_DENIED.value,
                "User denied MediaProjection permission"
            )
        }

        return true
    }

    /**
     * Clears stored permission data
     * Called during module cleanup to reset state
     */
    fun clear() {
        mediaProjectionData = null
        permissionPromise = null
    }
}
