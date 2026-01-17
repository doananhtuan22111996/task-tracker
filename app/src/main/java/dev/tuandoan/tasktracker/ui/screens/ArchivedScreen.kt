package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.ui.events.UiEvent
import dev.tuandoan.tasktracker.ui.components.ArchivedTaskListContent
import dev.tuandoan.tasktracker.ui.components.ArchivedTaskListTopBar
import dev.tuandoan.tasktracker.ui.viewmodel.TaskViewModel

/**
 * Screen for viewing and managing archived tasks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect archived tasks state
    val archivedTasks by viewModel.archivedTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentSort by viewModel.taskSort.collectAsStateWithLifecycle()

    // Selection state
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val pendingBulkDeleteTasks by viewModel.pendingBulkDeleteTasks.collectAsStateWithLifecycle()
    val pendingDeleteTask by viewModel.pendingDeleteTask.collectAsStateWithLifecycle()

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle UI events
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowUndoDelete -> {
                    val taskCount = event.tasks.size
                    val message = event.message ?: if (taskCount == 1) {
                        "Task permanently deleted"
                    } else {
                        "$taskCount tasks permanently deleted"
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
            ArchivedTaskListTopBar(
                currentSort = currentSort,
                onSortChanged = viewModel::setSort,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedCount,
                onBulkRestore = viewModel::bulkRestoreArchived,
                onBulkPermanentDelete = viewModel::requestBulkPermanentDelete,
                onClearSelection = viewModel::clearSelection,
                onSelectAll = { viewModel.selectAll(archivedTasks.map { it.id }) },
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        ArchivedTaskListContent(
            archivedTasks = archivedTasks,
            searchQuery = searchQuery,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onClearSearch = viewModel::clearSearch,
            onRestoreTask = viewModel::restoreArchivedTask,
            onPermanentDeleteTask = viewModel::requestPermanentDeleteTask,
            onLongPressTask = viewModel::enterSelection,
            onToggleSelection = viewModel::toggleSelection,
            modifier = Modifier.padding(paddingValues)
        )
    }

    // Show permanent delete confirmation dialog
    pendingDeleteTask?.let { task ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteTask() },
            title = { Text("Permanently Delete Task") },
            text = {
                Text("Are you sure you want to permanently delete \"${task.title}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmPermanentDeleteTask() }
                ) {
                    Text("Delete Permanently")
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

    // Show bulk permanent delete confirmation dialog
    if (pendingBulkDeleteTasks.isNotEmpty()) {
        val taskCount = pendingBulkDeleteTasks.size
        AlertDialog(
            onDismissRequest = { viewModel.cancelBulkDelete() },
            title = { Text("Permanently Delete Tasks") },
            text = {
                Text("Permanently delete $taskCount selected tasks? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBulkPermanentDelete() }
                ) {
                    Text("Delete Permanently")
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