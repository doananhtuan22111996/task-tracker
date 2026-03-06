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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.work.OverdueDigestWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class TaskTrackerApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val TASK_REMINDER_CHANNEL_ID = "task_reminders"
        private const val TAG = "TaskReminder"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TaskTrackerApplication.onCreate()")
        createNotificationChannel()
        logNotificationPermissionStatus()
        scheduleOverdueDigest()
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

    private fun scheduleOverdueDigest() {
        applicationScope.launch {
            try {
                val prefs = settingsRepository.userPreferences.first()
                val workManager = WorkManager.getInstance(this@TaskTrackerApplication)
                if (prefs.overdueDigestEnabled) {
                    val delay = millisUntilNextRun(prefs.overdueDigestHour)
                    val request =
                        PeriodicWorkRequestBuilder<OverdueDigestWorker>(1, TimeUnit.DAYS)
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .build()
                    workManager.enqueueUniquePeriodicWork(
                        OverdueDigestWorker.WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        request,
                    )
                    Log.d(TAG, "Overdue digest scheduled at hour ${prefs.overdueDigestHour}")
                } else {
                    workManager.cancelUniqueWork(OverdueDigestWorker.WORK_NAME)
                    Log.d(TAG, "Overdue digest cancelled (disabled)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule overdue digest", e)
            }
        }
    }

    private fun millisUntilNextRun(hour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
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
