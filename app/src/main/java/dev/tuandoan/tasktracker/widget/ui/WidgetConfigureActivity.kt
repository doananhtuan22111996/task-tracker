package dev.tuandoan.tasktracker.widget.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.tuandoan.tasktracker.ui.theme.TaskTrackerTheme

/**
 * Full-screen configuration activity launched by the launcher when the user places the widget
 * (android:configure in task_widget_info.xml, V13-08).
 *
 * Contract:
 *  - On confirm: calls setResult(RESULT_OK) with EXTRA_APPWIDGET_ID and finish().
 *  - On cancel / back: calls setResult(RESULT_CANCELED) and finish().
 *    The OS will remove the widget placement when RESULT_CANCELED is returned.
 *
 * The activity must return RESULT_CANCELED by default (set in init) to guard against
 * the case where the user kills the process before the result is set.
 */
@AndroidEntryPoint
class WidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default result is CANCELED; overridden to OK only on explicit confirm.
        setResult(RESULT_CANCELED)

        appWidgetId = intent
            ?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            TaskTrackerTheme {
                val viewModel: WidgetConfigureViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val tags by viewModel.tags.collectAsStateWithLifecycle()

                WidgetConfigureScreen(
                    uiState = uiState,
                    tags = tags,
                    onSelectionChange = { source -> viewModel.setSelection(source) },
                    onConfirm = {
                        viewModel.confirm(appWidgetId) {
                            val resultIntent = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                )
            }
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
