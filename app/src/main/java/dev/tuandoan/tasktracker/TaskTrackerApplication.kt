package dev.tuandoan.tasktracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TaskTrackerApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    companion object {
        const val TASK_REMINDER_CHANNEL_ID = "task_reminders"
        private const val TAG = "TaskReminder"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TaskTrackerApplication.onCreate()")
        createNotificationChannel()
        logNotificationPermissionStatus()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannel() {
        Log.d(TAG, "Creating notification channel...")

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TASK_REMINDER_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.notification_channel_description)
            }

            val notificationManager = getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)

            Log.d(
                TAG,
                "Notification channel '$TASK_REMINDER_CHANNEL_ID' created with importance: ${NotificationManager.IMPORTANCE_DEFAULT}",
            )
        } else {
            Log.d(TAG, "Android version < O, no notification channel needed")
        }
    }

    private fun logNotificationPermissionStatus() {
        val notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        Log.d(TAG, "Notifications permission status: enabled=$notificationsEnabled")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.i(TAG, "Running on Android 13+ - notification permission may need to be granted manually")
            if (!notificationsEnabled) {
                Log.w(
                    TAG,
                    "Notifications are disabled! Reminders will not work. User needs to enable them in Settings > Apps > Task Tracker > Notifications",
                )
            }
        }
    }
}
