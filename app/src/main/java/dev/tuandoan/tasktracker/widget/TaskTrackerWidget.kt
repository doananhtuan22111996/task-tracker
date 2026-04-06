package dev.tuandoan.tasktracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import dev.tuandoan.tasktracker.widget.ui.WidgetContent
import kotlinx.coroutines.flow.first

class TaskTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = WidgetEntryPoint.get(context)
        val dataProvider = WidgetDataProvider(entryPoint.taskDao())
        val tasks = dataProvider.getWidgetTasks()

        val settingsRepository = entryPoint.settingsRepository()
        val prefs = settingsRepository.userPreferences.first()

        provideContent {
            WidgetTheme(
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColor,
            ) {
                WidgetContent(tasks = tasks)
            }
        }
    }
}
