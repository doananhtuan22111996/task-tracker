package dev.tuandoan.tasktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.tuandoan.tasktracker.ui.screens.ArchivedScreen
import dev.tuandoan.tasktracker.ui.screens.StatsScreen
import dev.tuandoan.tasktracker.ui.screens.TaskListScreen
import dev.tuandoan.tasktracker.ui.theme.TaskTrackerTheme
import dev.tuandoan.tasktracker.ui.viewmodel.StatsViewModel
import dev.tuandoan.tasktracker.ui.viewmodel.TaskViewModel

/**
 * Main activity for the Task Tracker app.
 * Uses @AndroidEntryPoint to enable Hilt dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskTrackerTheme {
                TaskTrackerApp()
            }
        }
    }
}

/**
 * Main composable for the Task Tracker app.
 * Uses hiltViewModel() to get ViewModel instance with proper dependency injection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerApp() {
    val viewModel: TaskViewModel = hiltViewModel()
    val statsViewModel: StatsViewModel = hiltViewModel()
    var showArchivedScreen by remember { mutableStateOf(false) }
    var showStatsScreen by remember { mutableStateOf(false) }

    // Remove parent Scaffold with TopAppBar to avoid duplication - each screen manages its own TopAppBar
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when {
            showArchivedScreen -> {
                ArchivedScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        showArchivedScreen = false
                        showStatsScreen = false
                    },
                )
            }
            showStatsScreen -> {
                StatsScreen(
                    viewModel = statsViewModel,
                    onNavigateBack = {
                        showStatsScreen = false
                        showArchivedScreen = false
                    },
                )
            }
            else -> {
                TaskListScreen(
                    viewModel = viewModel,
                    onStatsClick = {
                        showStatsScreen = true
                        showArchivedScreen = false
                    },
                    onArchiveClick = {
                        showArchivedScreen = true
                        showStatsScreen = false
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskTrackerAppPreview() {
    TaskTrackerTheme {
        TaskTrackerApp()
    }
}
