package dev.tuandoan.tasktracker

import android.content.Intent
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
import dev.tuandoan.tasktracker.navigation.StatsFilter
import dev.tuandoan.tasktracker.navigation.TaskTrackerRoutes
import dev.tuandoan.tasktracker.ui.manager.NotificationPermissionManager
import dev.tuandoan.tasktracker.ui.screens.ArchivedScreen
import dev.tuandoan.tasktracker.ui.screens.HelpScreen
import dev.tuandoan.tasktracker.ui.screens.OnboardingScreen
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

        // Check if launched from app shortcut deep-link
        val deepLinkRoute = resolveDeepLinkRoute(intent)

        setContent {
            val userPreferences by settingsRepository.userPreferences
                .collectAsStateWithLifecycle(initialValue = null)

            val prefs = userPreferences
            if (prefs != null) {
                TaskTrackerTheme(
                    themeMode = prefs.themeMode,
                    dynamicColor = prefs.dynamicColor,
                ) {
                    TaskTrackerApp(
                        notificationPermissionManager = notificationPermissionManager,
                        isOnboardingCompleted = prefs.onboardingCompleted,
                        deepLinkRoute = deepLinkRoute,
                    )
                }
            }
        }
    }

    private fun resolveDeepLinkRoute(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme == "tasktracker" && data.host == "task_editor") {
            return TaskTrackerRoutes.TASK_EDITOR_CREATE
        }
        return null
    }
}

/**
 * Main composable for the Task Tracker app.
 * Uses NavHost for navigation between screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerApp(
    notificationPermissionManager: NotificationPermissionManager? = null,
    isOnboardingCompleted: Boolean = true,
    deepLinkRoute: String? = null,
) {
    val navController = rememberNavController()
    val startDestination = if (isOnboardingCompleted) {
        TaskTrackerRoutes.TASK_LIST
    } else {
        TaskTrackerRoutes.ONBOARDING
    }

    // Handle deep-link navigation (e.g., from app shortcut)
    androidx.compose.runtime.LaunchedEffect(deepLinkRoute) {
        if (deepLinkRoute != null && isOnboardingCompleted) {
            navController.navigate(deepLinkRoute)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(TaskTrackerRoutes.ONBOARDING) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                OnboardingScreen(
                    onFinish = {
                        settingsViewModel.setOnboardingCompleted()
                        navController.navigate(TaskTrackerRoutes.TASK_LIST) {
                            popUpTo(TaskTrackerRoutes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }

            composable(
                route = TaskTrackerRoutes.TASK_LIST,
                arguments = listOf(
                    navArgument("statsFilter") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                val viewModel: TaskViewModel = hiltViewModel()
                val statsFilterName = backStackEntry.arguments?.getString("statsFilter")
                val statsFilter = statsFilterName?.let {
                    runCatching { StatsFilter.valueOf(it) }.getOrNull()
                }
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
                    initialStatsFilter = statsFilter,
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
                    onNavigateToFilteredList = { statsFilter ->
                        navController.navigate(TaskTrackerRoutes.taskListWithFilter(statsFilter.name))
                    },
                    onNavigateToCreateTask = {
                        navController.navigate(TaskTrackerRoutes.TASK_EDITOR_CREATE)
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
                    onNavigateToHelp = {
                        navController.navigate(TaskTrackerRoutes.HELP)
                    },
                )
            }

            composable(TaskTrackerRoutes.HELP) {
                HelpScreen(
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
