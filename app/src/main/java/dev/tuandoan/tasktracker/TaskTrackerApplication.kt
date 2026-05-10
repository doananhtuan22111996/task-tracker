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
import dev.tuandoan.tasktracker.data.preferences.PrivacyRepository
import dev.tuandoan.tasktracker.diagnostics.PrivacyManager
import javax.inject.Inject

@HiltAndroidApp
class TaskTrackerApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var privacyManager: PrivacyManager

    companion object {
        const val TASK_REMINDER_CHANNEL_ID = "task_reminders"
        private const val TAG = "TaskReminder"
    }

    /**
     * Startup ordering invariant (v1.12.0, FB-06 — ADR-003):
     *
     * **`applyDiagnosticsConsent()` MUST run before any Firebase-touching code.**
     *
     * Firebase's `FirebaseInitProvider` ContentProvider auto-initializes `FirebaseApp`
     * before `Application.onCreate()` fires. The `firebase_*_collection_enabled=false`
     * manifest defaults from FB-02 keep the three SDKs inert at that point. This
     * method synchronously reads the persisted opt-in and reflects it to the SDKs
     * via [PrivacyManager.initCollectionState] — if the user previously opted in,
     * collection starts here; if not, the SDKs stay disabled.
     *
     * Do NOT insert any code that touches Crashlytics, Analytics, or Performance
     * (including KTX property accessors like `Firebase.analytics`) before this call.
     * A future refactor that violates this ordering would bypass the consent gate
     * and leak telemetry on the opt-out path.
     */
    override fun onCreate() {
        super.onCreate()
        applyDiagnosticsConsent()
        Log.d(TAG, "TaskTrackerApplication.onCreate()")
        createNotificationChannel()
        logNotificationPermissionStatus()
    }

    private fun applyDiagnosticsConsent() {
        val optIn = PrivacyRepository.readOptInOnce(this)
        privacyManager.initCollectionState(optIn)
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
