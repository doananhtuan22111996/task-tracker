package dev.tuandoan.tasktracker.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.tuandoan.tasktracker.BuildConfig
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.navigation.StatsFilter
import dev.tuandoan.tasktracker.navigation.TaskTrackerRoutes
import dev.tuandoan.tasktracker.ui.components.FeatureTip
import dev.tuandoan.tasktracker.ui.components.TagChipRow
import dev.tuandoan.tasktracker.ui.components.TaskListContent
import dev.tuandoan.tasktracker.ui.components.TaskListTopBar
import dev.tuandoan.tasktracker.ui.events.UiEvent
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter
import dev.tuandoan.tasktracker.ui.viewmodel.TaskViewModel

/**
 * Main task list screen that coordinates all task-related UI components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel,
    navController: NavController,
    onStatsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    initialStatsFilter: StatsFilter? = null,
) {
    // Collect all required state
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val visibleTasks by viewModel.visibleTasks.collectAsStateWithLifecycle()
    val groupedVisibleTasks by viewModel.groupedVisibleTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()
    val currentTagFilter by viewModel.tagFilter.collectAsStateWithLifecycle()
    val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()
    val pendingDeleteTask by viewModel.pendingDeleteTask.collectAsStateWithLifecycle()

    // Selection state
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val pendingBulkDeleteTasks by viewModel.pendingBulkDeleteTasks.collectAsStateWithLifecycle()
    val pendingBulkArchiveTasks by viewModel.pendingBulkArchiveTasks.collectAsStateWithLifecycle()

    // Feature tips state
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Debug-only mock rating dialog state
    var showDebugRatingDialog by remember { mutableStateOf(false) }

    // Apply initial filter from Stats screen navigation (SPEC-S01)
    LaunchedEffect(initialStatsFilter) {
        when (initialStatsFilter) {
            StatsFilter.ACTIVE -> viewModel.setFilter(TaskFilter.ACTIVE)
            StatsFilter.COMPLETED, StatsFilter.COMPLETED_TODAY -> viewModel.setFilter(TaskFilter.COMPLETED)
            StatsFilter.DUE_TODAY, StatsFilter.OVERDUE -> viewModel.setFilter(TaskFilter.ACTIVE)
            null -> {} // No filter to apply
        }
    }

    // Handle UI events
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowUndoDelete -> {
                    val taskCount = event.tasks.size
                    val message = event.message ?: if (taskCount == 1) {
                        context.getString(R.string.snackbar_task_deleted)
                    } else {
                        context.getString(R.string.snackbar_tasks_deleted, taskCount)
                    }

                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = context.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onUndo()
                    }
                }
                is UiEvent.ShowRatingPrompt -> {
                    if (BuildConfig.DEBUG) {
                        showDebugRatingDialog = true
                    } else {
                        context.findActivity()?.let { activity ->
                            dev.tuandoan.tasktracker.ui.components.RatingPromptManager
                                .maybeRequestReview(activity)
                        }
                    }
                }
                is UiEvent.ShowSnackbar -> {
                    if (event.actionLabel != null) {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            event.onActionClick()
                        }
                    } else {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
        }
    }

    // Debug-only mock rating dialog that simulates Google Play In-App Review
    if (showDebugRatingDialog) {
        DebugRatingDialog(onDismiss = { showDebugRatingDialog = false })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TaskListTopBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedCount,
                onBulkMarkCompleted = viewModel::bulkMarkCompleted,
                onBulkMarkActive = viewModel::bulkMarkActive,
                onBulkArchive = viewModel::requestBulkArchive,
                onClearSelection = viewModel::clearSelection,
                onSelectAll = { viewModel.selectAll(visibleTasks.map { it.id }) },
                onStatsClick = onStatsClick,
                onArchiveClick = onArchiveClick,
                onSettingsClick = onSettingsClick,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(TaskTrackerRoutes.TASK_EDITOR_CREATE) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_task),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            if (!isSelectionMode) { // Hide bottom nav during selection mode
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    val navigationItems = listOf(
                        TaskFilter.ALL to stringResource(R.string.nav_all) to
                            Pair(Icons.Outlined.List, Icons.Filled.List),
                        TaskFilter.ACTIVE to stringResource(R.string.nav_active) to
                            Pair(Icons.Outlined.RadioButtonUnchecked, Icons.Filled.RadioButtonUnchecked),
                        TaskFilter.COMPLETED to stringResource(R.string.nav_completed) to
                            Pair(Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
                    )

                    navigationItems.forEach { (filterData, icons) ->
                        val (filter, label) = filterData
                        val (unselectedIcon, selectedIcon) = icons

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (currentFilter == filter) selectedIcon else unselectedIcon,
                                    contentDescription = label,
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            selected = currentFilter == filter,
                            onClick = {
                                viewModel.setFilter(filter)
                            },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TagChipRow(
                currentTagFilter = currentTagFilter,
                availableTags = availableTags,
                onTagFilterChange = viewModel::setTagFilter,
            )
            FeatureTip(
                text = stringResource(R.string.tip_fab_create_task),
                visible = !userPrefs.tipFabShown && allTasks.isEmpty(),
                onDismiss = viewModel::setTipFabShown,
            )
            FeatureTip(
                text = stringResource(R.string.tip_tag_chips_filter),
                visible = !userPrefs.tipTagChipsShown && availableTags.isNotEmpty(),
                onDismiss = viewModel::setTipTagChipsShown,
            )
            TaskListContent(
                allTasks = allTasks,
                visibleTasks = visibleTasks,
                groupedVisibleTasks = groupedVisibleTasks,
                searchQuery = searchQuery,
                currentFilter = currentFilter,
                currentTagFilter = currentTagFilter,
                availableTags = availableTags,
                selectedIds = selectedIds,
                isSelectionMode = isSelectionMode,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onClearSearch = viewModel::clearSearch,
                onToggleTaskComplete = viewModel::toggleTaskCompletion,
                onEditTask = { task -> navController.navigate(TaskTrackerRoutes.taskEditorEdit(task.id)) },
                onArchiveTask = viewModel::archiveTask,
                onDuplicateTask = viewModel::duplicateTask,
                onSkipOccurrence = viewModel::skipOccurrence,
                onPinTask = viewModel::toggleTaskPin,
                onLongPressTask = viewModel::enterSelection,
                onToggleSelection = viewModel::toggleSelection,
            )
        }
    }

    // Show archive confirmation dialog
    pendingDeleteTask?.let { task ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteTask() },
            title = { Text(stringResource(R.string.dialog_archive_task_title)) },
            text = {
                Text(
                    stringResource(R.string.dialog_archive_task_message, task.title),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmArchiveTask() },
                ) {
                    Text(stringResource(R.string.action_archive))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelDeleteTask() },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Show bulk delete confirmation dialog
    if (pendingBulkDeleteTasks.isNotEmpty()) {
        val taskCount = pendingBulkDeleteTasks.size
        AlertDialog(
            onDismissRequest = { viewModel.cancelBulkDelete() },
            title = { Text(stringResource(R.string.dialog_delete_tasks_title)) },
            text = {
                Text(stringResource(R.string.dialog_delete_tasks_message, taskCount))
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBulkDelete() },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelBulkDelete() },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Show bulk archive confirmation dialog
    if (pendingBulkArchiveTasks.isNotEmpty()) {
        val taskCount = pendingBulkArchiveTasks.size
        AlertDialog(
            onDismissRequest = { viewModel.cancelBulkArchive() },
            title = { Text(stringResource(R.string.dialog_archive_tasks_title)) },
            text = {
                Text(stringResource(R.string.dialog_archive_tasks_message, taskCount))
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBulkArchive() },
                ) {
                    Text(stringResource(R.string.action_archive))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelBulkArchive() },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun DebugRatingDialog(onDismiss: () -> Unit) {
    var selectedStars by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "DEBUG: Google Play Review",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rate Task Tracker",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "This simulates the native Google Play In-App Review dialog. " +
                        "In release builds installed from the Play Store, the real " +
                        "Play Store review sheet appears here instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    for (i in 1..5) {
                        IconButton(onClick = { selectedStars = i }) {
                            Icon(
                                imageVector = if (i <= selectedStars) {
                                    Icons.Filled.Star
                                } else {
                                    Icons.Filled.StarBorder
                                },
                                contentDescription = "$i star",
                                tint = if (i <= selectedStars) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        },
    )
}
