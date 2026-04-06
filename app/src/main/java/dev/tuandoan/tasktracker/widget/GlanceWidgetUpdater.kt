package dev.tuandoan.tasktracker.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.domain.scheduler.WidgetUpdater
import javax.inject.Inject

class GlanceWidgetUpdater @Inject constructor(@ApplicationContext private val context: Context) : WidgetUpdater {

    override suspend fun requestUpdate() {
        TaskTrackerWidget().updateAll(context)
    }
}
