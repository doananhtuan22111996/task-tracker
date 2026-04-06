package dev.tuandoan.tasktracker.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.tasktracker.data.scheduler.WorkManagerTaskReminderScheduler
import dev.tuandoan.tasktracker.domain.scheduler.TaskReminderScheduler
import dev.tuandoan.tasktracker.domain.scheduler.WidgetUpdater
import dev.tuandoan.tasktracker.widget.GlanceWidgetUpdater
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkManagerModule {

    @Binds
    @Singleton
    abstract fun bindTaskReminderScheduler(
        workManagerScheduler: WorkManagerTaskReminderScheduler,
    ): TaskReminderScheduler

    @Binds
    @Singleton
    abstract fun bindWidgetUpdater(glanceWidgetUpdater: GlanceWidgetUpdater): WidgetUpdater

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
    }
}
