package dev.tuandoan.tasktracker.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Helper object for handling notification permissions on Android 13+ (API 33+).
 */
object NotificationPermission {

    /**
     * Check if notification permission is granted.
     * Always returns true for API < 33 since no runtime permission is required.
     */
    fun isGranted(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // No runtime permission required before API 33
    }

    /**
     * Check if we should request notification permission.
     * Only returns true for API >= 33 where runtime permission is required.
     */
    fun shouldRequest(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * Check if we need to request notification permission for the given context.
     * Combines both API level check and current permission status.
     */
    fun needsPermissionRequest(context: Context): Boolean = shouldRequest() && !isGranted(context)

    /**
     * Open the app's notification settings screen.
     * Uses ACTION_APP_NOTIFICATION_SETTINGS with fallback to app details.
     */
    fun openAppNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general app settings if specific notification settings fail
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Get the notification permission string for requesting.
     * Returns the permission constant - safe to call on any API level.
     * Only meaningful for permission requests on API >= 33.
     */
    fun getPermissionString(): String = Manifest.permission.POST_NOTIFICATIONS
}
