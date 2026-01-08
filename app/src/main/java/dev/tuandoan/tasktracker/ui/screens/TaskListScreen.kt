package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.ui.events.UiEvent
import dev.tuandoan.tasktracker.ui.components.TaskListContent
import dev.tuandoan.tasktracker.ui.components.TaskListTopBar
import dev.tuandoan.tasktracker.ui.viewmodel.TaskViewModel
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter

/**
 * Main task list screen that coordinates all task-related UI components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    // Collect all required state
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val visibleTasks by viewModel.visibleTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()
    val currentSort by viewModel.taskSort.collectAsStateWithLifecycle()
    val showAddTaskDialog by viewModel.showAddTaskDialog.collectAsStateWithLifecycle()
    val pendingDeleteTask by viewModel.pendingDeleteTask.collectAsStateWithLifecycle()

    // Selection state
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val pendingBulkDeleteTasks by viewModel.pendingBulkDeleteTasks.collectAsStateWithLifecycle()

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle UI events
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowUndoDelete -> {
                    val taskCount = event.tasks.size
                    val message = if (taskCount == 1) {
                        "Task deleted"
                    } else {
                        "$taskCount tasks deleted"
                    }

                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "UNDO",
                        duration = SnackbarDuration.Short
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
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            event.onActionClick()
                        }
                    } else {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
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
                onBulkDelete = viewModel::requestBulkDelete,
                onClearSelection = viewModel::clearSelection,
                onSelectAll = { viewModel.selectAll(visibleTasks.map { it.id }) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddTaskDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task"
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        TaskListContent(
            allTasks = allTasks,
            visibleTasks = visibleTasks,
            searchQuery = searchQuery,
            currentFilter = currentFilter,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onClearSearch = viewModel::clearSearch,
            onFilterChange = viewModel::setFilter,
            onToggleTaskComplete = viewModel::toggleTaskCompletion,
            onEditTask = viewModel::showEditTaskDialog,
            onDeleteTask = viewModel::deleteTask,
            onLongPressTask = viewModel::enterSelection,
            onToggleSelection = viewModel::toggleSelection,
            modifier = Modifier.padding(paddingValues)
        )
    }

    // Show dialog when requested
    if (showAddTaskDialog) {
        AddEditTaskDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideAddTaskDialog() }
        )
    }

    // Show delete confirmation dialog
    pendingDeleteTask?.let { task ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteTask() },
            title = { Text("Delete Task") },
            text = {
                Text("Are you sure you want to delete \"${task.title}\"? This action can be undone for a few seconds after deletion.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteTask() }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelDeleteTask() }
                ) {
                    Text("Cancel")
                }
            }
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
                    onClick = { viewModel.confirmBulkDelete() }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelBulkDelete() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}