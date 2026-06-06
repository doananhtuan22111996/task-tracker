package dev.tuandoan.tasktracker.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.tasktracker.data.database.TaskDao
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.data.preferences.WidgetConfigurationRepository
import dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger
import dev.tuandoan.tasktracker.domain.ITaskManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {

    fun taskDao(): TaskDao

    fun settingsRepository(): SettingsRepository

    fun taskManager(): ITaskManager

    fun widgetConfigurationRepository(): WidgetConfigurationRepository

    fun analyticsLogger(): AnalyticsLogger

    companion object {
        fun get(context: Context): WidgetEntryPoint =
            EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
    }
}
