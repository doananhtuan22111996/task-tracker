package dev.tuandoan.tasktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.tuandoan.tasktracker.ui.screens.ArchivedScreen
import dev.tuandoan.tasktracker.ui.screens.TaskListScreen
import dev.tuandoan.tasktracker.ui.theme.TaskTrackerTheme
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
    var showArchivedScreen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showArchivedScreen) "Archived Tasks" else "Task Tracker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (!showArchivedScreen) {
                        // Show archive button only on main screen
                        IconButton(onClick = { showArchivedScreen = true }) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "View archived tasks"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (showArchivedScreen) {
            ArchivedScreen(
                viewModel = viewModel,
                onNavigateBack = { showArchivedScreen = false },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            TaskListScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
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