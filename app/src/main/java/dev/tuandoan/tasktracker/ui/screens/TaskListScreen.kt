package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.tuandoan.tasktracker.navigation.TaskTrackerRoutes
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
) {
    // Collect all required state
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val visibleTasks by viewModel.visibleTasks.collectAsStateWithLifecycle()
    val groupedVisibleTasks by viewModel.groupedVisibleTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()
    val currentTagFilter by viewModel.tagFilter.collectAsStateWithLifecycle()
    val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()
    val currentSort by viewModel.taskSort.collectAsStateWithLifecycle()
    val pendingDeleteTask by viewModel.pendingDeleteTask.collectAsStateWithLifecycle()

    // Selection state
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val pendingBulkDeleteTasks by viewModel.pendingBulkDeleteTasks.collectAsStateWithLifecycle()
    val pendingBulkArchiveTasks by viewModel.pendingBulkArchiveTasks.collectAsStateWithLifecycle()

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle UI events
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowUndoDelete -> {
                    val taskCount = event.tasks.size
                    val message = event.message ?: if (taskCount == 1) {
                        "Task deleted"
                    } else {
                        "$taskCount tasks deleted"
                    }

                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "UNDO",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onUndo()
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TaskListTopBar(
                currentSort = currentSort,
                onSortChanged = viewModel::setSort,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedCount,
                onBulkMarkCompleted = viewModel::bulkMarkCompleted,
                onBulkMarkActive = viewModel::bulkMarkActive,
                onBulkArchive = viewModel::requestBulkArchive,
                onClearSelection = viewModel::clearSelection,
                onSelectAll = { viewModel.selectAll(visibleTasks.map { it.id }) },
                onStatsClick = onStatsClick,
                onArchiveClick = onArchiveClick,
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
                    contentDescription = "Add Task",
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
                        TaskFilter.ALL to "All" to Pair(Icons.Outlined.List, Icons.Filled.List),
                        TaskFilter.ACTIVE to "Active" to
                            Pair(Icons.Outlined.RadioButtonUnchecked, Icons.Filled.RadioButtonUnchecked),
                        TaskFilter.COMPLETED to "Completed" to
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
            onTagFilterChange = viewModel::setTagFilter,
            onToggleTaskComplete = viewModel::toggleTaskCompletion,
            onEditTask = { task -> navController.navigate(TaskTrackerRoutes.taskEditorEdit(task.id)) },
            onArchiveTask = viewModel::archiveTask,
            onPinTask = viewModel::toggleTaskPin,
            onLongPressTask = viewModel::enterSelection,
            onToggleSelection = viewModel::toggleSelection,
            modifier = Modifier.padding(paddingValues),
        )
    }

    // Show archive confirmation dialog
    pendingDeleteTask?.let { task ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteTask() },
            title = { Text("Archive Task") },
            text = {
                Text(
                    "Are you sure you want to archive \"${task.title}\"? This action can be undone for a few seconds after archiving.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmArchiveTask() },
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelDeleteTask() },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    // Show bulk delete confirmation dialog
    if (pendingBulkDeleteTasks.isNotEmpty()) {
        val taskCount = pendingBulkDeleteTasks.size
        AlertDialog(
            onDismissRequest = { viewModel.cancelBulkDelete() },
            title = { Text("Delete Tasks") },
            text = {
                Text("Delete $taskCount selected tasks? This action can be undone for a short time.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBulkDelete() },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelBulkDelete() },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    // Show bulk archive confirmation dialog
    if (pendingBulkArchiveTasks.isNotEmpty()) {
        val taskCount = pendingBulkArchiveTasks.size
        AlertDialog(
            onDismissRequest = { viewModel.cancelBulkArchive() },
            title = { Text("Archive Tasks") },
            text = {
                Text("Archive $taskCount selected tasks? This action can be undone for a short time.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBulkArchive() },
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelBulkArchive() },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
