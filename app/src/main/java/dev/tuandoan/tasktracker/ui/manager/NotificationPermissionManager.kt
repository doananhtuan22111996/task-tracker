package dev.tuandoan.tasktracker.ui.manager

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import dev.tuandoan.tasktracker.R

/**
 * Production-ready notification permission manager for Android 13+ compatibility
 * Handles permission requests, user education, and fallback flows
 *
 * Supports minSDK 26 to maxSDK 36 with proper Android 15 compatibility
 */
class NotificationPermissionManager(
    private val activity: Activity,
    private val permissionLauncher: ActivityResultLauncher<String>,
) {
    companion object {
        private const val TAG = "NotificationPermission"
        private const val POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS
    }

    private var pendingPermissionCallback: ((Boolean) -> Unit)? = null

    // Dialog state management
    var showPermissionRationale by mutableStateOf(false)
        private set

    var showPermissionDenied by mutableStateOf(false)
        private set

    /**
     * Smart permission request - only prompts when needed
     * @param onResult Callback with permission result
     */
    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        Log.d(TAG, "requestNotificationPermission called")

        // For Android 12 and below, permissions are granted at install time
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Android < 13: permission granted by default")
            onResult(true)
            return
        }

        // Check if already granted
        if (isPermissionGranted()) {
            Log.d(TAG, "Permission already granted")
            onResult(true)
            return
        }

        // Store callback for result handling
        pendingPermissionCallback = onResult

        // For first-time users, show rationale dialog to educate them
        // For users who previously denied, still show rationale as education
        Log.d(TAG, "Showing permission rationale dialog for education")
        showPermissionRationale = true
    }

    /**
     * Handle permission result from ActivityResultLauncher
     * @param isGranted Whether permission was granted
     */
    fun handlePermissionResult(isGranted: Boolean) {
        Log.d(TAG, "Permission result: granted=$isGranted")

        if (isGranted) {
            // Permission granted - success!
            pendingPermissionCallback?.invoke(true)
            pendingPermissionCallback = null
        } else {
            // Permission denied - show settings dialog
            Log.w(TAG, "Notification permission denied - showing settings dialog")
            showPermissionDenied = true
        }
    }

    /**
     * Check if notification permission is granted
     * @return true if granted or not needed (Android < 13)
     */
    fun isPermissionGranted(): Boolean {
        // For Android 12 and below, no runtime permission required
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        // Check using NotificationManagerCompat (recommended approach)
        val areNotificationsEnabled = NotificationManagerCompat.from(activity)
            .areNotificationsEnabled()

        // Double-check with PermissionChecker for accuracy
        val permissionCheck = ContextCompat.checkSelfPermission(
            activity,
            POST_NOTIFICATIONS,
        ) == PermissionChecker.PERMISSION_GRANTED

        val result = areNotificationsEnabled && permissionCheck
        Log.d(
            TAG,
            "Permission check: notifications=$areNotificationsEnabled, permission=$permissionCheck, result=$result",
        )

        return result
    }

    /**
     * Show permission rationale dialog - called internally
     * Dialog state is managed via showPermissionRationale variable
     */
    fun showPermissionRationaleDialog() {
        Log.d(TAG, "Showing permission rationale dialog")
        showPermissionRationale = true
    }

    /**
     * Open device notification settings for this app
     */
    private fun openNotificationSettings() {
        Log.d(TAG, "Opening notification settings")
        try {
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        // Android 8.0+: Open app-specific notification settings
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    }
                    else -> {
                        // Android 7.1 and below: Open general app settings
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification settings", e)
            // Fallback: open general settings
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activity.startActivity(fallbackIntent)
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Error opening fallback settings", fallbackException)
            }
        }
    }

    /**
     * Check if permission request is needed for the current Android version
     * @return true if permission request is needed
     */
    fun shouldRequestPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPermissionGranted()

    /**
     * Get user-friendly explanation for notification permission
     * @return Explanation text
     */
    fun getPermissionExplanation(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        activity.getString(R.string.notification_rationale)
    } else {
        activity.getString(R.string.notification_explanation_enabled)
    }

    // ===========================================
    // Dialog Interaction Methods
    // ===========================================

    /**
     * Handle user confirming permission rationale dialog
     * Proceeds with permission request
     */
    fun onPermissionRationaleConfirm() {
        Log.d(TAG, "User confirmed permission rationale")
        showPermissionRationale = false

        try {
            permissionLauncher.launch(POST_NOTIFICATIONS)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching permission request from rationale", e)
            pendingPermissionCallback?.invoke(false)
            pendingPermissionCallback = null
        }
    }

    /**
     * Handle user dismissing permission rationale dialog
     */
    fun onPermissionRationaleDismiss() {
        Log.d(TAG, "User dismissed permission rationale")
        showPermissionRationale = false
        pendingPermissionCallback?.invoke(false)
        pendingPermissionCallback = null
    }

    /**
     * Handle user choosing to open settings from permission denied dialog
     */
    fun onPermissionDeniedOpenSettings() {
        Log.d(TAG, "User chose to open settings from denied dialog")
        showPermissionDenied = false
        openNotificationSettings()
        // Don't invoke callback yet - user might enable permission in settings
    }

    /**
     * Handle user dismissing permission denied dialog
     */
    fun onPermissionDeniedDismiss() {
        Log.d(TAG, "User dismissed permission denied dialog")
        showPermissionDenied = false
        pendingPermissionCallback?.invoke(false)
        pendingPermissionCallback = null
    }

    /**
     * Reset all dialog states (useful for cleanup)
     */
    fun resetDialogStates() {
        showPermissionRationale = false
        showPermissionDenied = false
        pendingPermissionCallback = null
    }
}
