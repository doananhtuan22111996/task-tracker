package dev.tuandoan.tasktracker.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import dev.tuandoan.tasktracker.data.preferences.ThemeMode
import dev.tuandoan.tasktracker.widget.model.WidgetTask
import dev.tuandoan.tasktracker.widget.ui.WidgetContent
import kotlinx.coroutines.flow.first

class TaskTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var tasks: List<WidgetTask> = emptyList()
        var themeMode = ThemeMode.SYSTEM
        var dynamicColor = true

        try {
            val entryPoint = WidgetEntryPoint.get(context)
            tasks = WidgetDataProvider(entryPoint.taskDao()).getWidgetTasks()
            val prefs = entryPoint.settingsRepository().userPreferences.first()
            themeMode = prefs.themeMode
            dynamicColor = prefs.dynamicColor
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load widget data", e)
        }

        provideContent {
            WidgetTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                WidgetContent(tasks = tasks)
            }
        }
    }

    companion object {
        private const val TAG = "TaskTrackerWidget"
    }
}
