package dev.tuandoan.tasktracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.data.preferences.UserPreferences
import dev.tuandoan.tasktracker.navigation.TaskTrackerRoutes
import dev.tuandoan.tasktracker.ui.manager.NotificationPermissionManager
import dev.tuandoan.tasktracker.ui.screens.ArchivedScreen
import dev.tuandoan.tasktracker.ui.screens.SettingsScreen
import dev.tuandoan.tasktracker.ui.screens.StatsScreen
import dev.tuandoan.tasktracker.ui.screens.TaskEditorScreen
import dev.tuandoan.tasktracker.ui.screens.TaskListScreen
import dev.tuandoan.tasktracker.ui.theme.TaskTrackerTheme
import dev.tuandoan.tasktracker.ui.viewmodel.SettingsViewModel
import dev.tuandoan.tasktracker.ui.viewmodel.StatsViewModel
import dev.tuandoan.tasktracker.ui.viewmodel.TaskViewModel
import javax.inject.Inject

/**
 * Main activity for the Task Tracker app.
 * Uses @AndroidEntryPoint to enable Hilt dependency injection.
 * Extends AppCompatActivity to support per-app language selection via AppCompatDelegate.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Notification permission manager
    private lateinit var notificationPermissionManager: NotificationPermissionManager

    // Permission request launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        notificationPermissionManager.handlePermissionResult(isGranted)
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize notification permission manager
        notificationPermissionManager = NotificationPermissionManager(
            activity = this,
            permissionLauncher = notificationPermissionLauncher,
        )

        enableEdgeToEdge()
        setContent {
            val userPreferences by settingsRepository.userPreferences
                .collectAsStateWithLifecycle(initialValue = UserPreferences())

            TaskTrackerTheme(
                themeMode = userPreferences.themeMode,
                dynamicColor = userPreferences.dynamicColor,
            ) {
                TaskTrackerApp(notificationPermissionManager)
            }
        }
    }
}

/**
 * Main composable for the Task Tracker app.
 * Uses NavHost for navigation between screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerApp(notificationPermissionManager: NotificationPermissionManager? = null) {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavHost(
            navController = navController,
            startDestination = TaskTrackerRoutes.TASK_LIST,
        ) {
            composable(TaskTrackerRoutes.TASK_LIST) {
                val viewModel: TaskViewModel = hiltViewModel()
                TaskListScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onStatsClick = {
                        navController.navigate(TaskTrackerRoutes.STATS)
                    },
                    onArchiveClick = {
                        navController.navigate(TaskTrackerRoutes.ARCHIVED)
                    },
                    onSettingsClick = {
                        navController.navigate(TaskTrackerRoutes.SETTINGS)
                    },
                )
            }

            composable(TaskTrackerRoutes.ARCHIVED) {
                val viewModel: TaskViewModel = hiltViewModel()
                ArchivedScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable(TaskTrackerRoutes.STATS) {
                val statsViewModel: StatsViewModel = hiltViewModel()
                StatsScreen(
                    viewModel = statsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable(TaskTrackerRoutes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable(TaskTrackerRoutes.TASK_EDITOR_CREATE) {
                TaskEditorScreen(
                    navController = navController,
                    notificationPermissionManager = notificationPermissionManager,
                )
            }

            composable(
                route = TaskTrackerRoutes.TASK_EDITOR_EDIT,
                arguments = listOf(
                    navArgument("taskId") {
                        type = NavType.LongType
                    },
                ),
            ) {
                TaskEditorScreen(
                    navController = navController,
                    notificationPermissionManager = notificationPermissionManager,
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
