package dev.tuandoan.tasktracker.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import dev.tuandoan.tasktracker.widget.ui.WidgetContent
import kotlinx.coroutines.flow.first

class TaskTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = try {
            val entryPoint = WidgetEntryPoint.get(context)
            val dataProvider = WidgetDataProvider(entryPoint.taskDao())
            dataProvider.getWidgetTasks()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load widget tasks", e)
            emptyList()
        }

        val prefs = try {
            val entryPoint = WidgetEntryPoint.get(context)
            entryPoint.settingsRepository().userPreferences.first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load widget preferences", e)
            null
        }

        provideContent {
            WidgetTheme(
                themeMode = prefs?.themeMode
                    ?: dev.tuandoan.tasktracker.data.preferences.ThemeMode.SYSTEM,
                dynamicColor = prefs?.dynamicColor ?: true,
            ) {
                WidgetContent(tasks = tasks)
            }
        }
    }

    companion object {
        private const val TAG = "TaskTrackerWidget"
    }
}
