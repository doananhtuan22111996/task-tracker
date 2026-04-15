package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.ui.components.ArchivedTaskListContent
import dev.tuandoan.tasktracker.ui.components.ArchivedTaskListTopBar
import dev.tuandoan.tasktracker.ui.components.TagChipRow
import dev.tuandoan.tasktracker.ui.events.UiEvent
import dev.tuandoan.tasktracker.ui.viewmodel.TaskViewModel

/**
 * Screen for viewing and managing archived tasks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(viewModel: TaskViewModel, modifier: Modifier = Modifier, bottomBarPadding: Dp = 0.dp) {
    // Collect archived tasks state
    val archivedTasks by viewModel.archivedTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val archivedTagFilter by viewModel.archivedTagFilter.collectAsStateWithLifecycle()
    val archivedAvailableTags by viewModel.archivedAvailableTags.collectAsStateWithLifecycle()

    // Selection state
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val pendingBulkDeleteTasks by viewModel.pendingBulkDeleteTasks.collectAsStateWithLifecycle()
    val pendingDeleteTask by viewModel.pendingDeleteTask.collectAsStateWithLifecycle()

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle UI events
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowUndoDelete -> {
                    val taskCount = event.tasks.size
                    val message = event.message ?: if (taskCount == 1) {
                        context.getString(R.string.snackbar_task_permanently_deleted)
                    } else {
                        context.getString(R.string.snackbar_tasks_permanently_deleted, taskCount)
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
                is UiEvent.ShowRatingPrompt -> { /* Not applicable on archive screen */ }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ArchivedTaskListTopBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedCount,
                onBulkRestore = viewModel::bulkRestoreArchived,
                onBulkPermanentDelete = viewModel::requestBulkPermanentDelete,
                onClearSelection = viewModel::clearSelection,
                onSelectAll = { viewModel.selectAll(archivedTasks.map { it.id }) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = bottomBarPadding)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TagChipRow(
                currentTagFilter = archivedTagFilter,
                availableTags = archivedAvailableTags,
                onTagFilterChange = viewModel::setArchivedTagFilter,
            )
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
                bottomBarPadding = bottomBarPadding,
            )
        }
    }

    // Show permanent delete confirmation dialog
    pendingDeleteTask?.let { task ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteTask() },
            title = { Text(stringResource(R.string.dialog_permanent_delete_title)) },
            text = {
                Text(stringResource(R.string.dialog_permanent_delete_message, task.title))
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmPermanentDeleteTask() },
                ) {
                    Text(stringResource(R.string.action_delete_permanently))
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

    // Show bulk permanent delete confirmation dialog
    if (pendingBulkDeleteTasks.isNotEmpty()) {
        val taskCount = pendingBulkDeleteTasks.size
        AlertDialog(
            onDismissRequest = { viewModel.cancelBulkDelete() },
            title = { Text(stringResource(R.string.dialog_permanent_delete_tasks_title)) },
            text = {
                Text(stringResource(R.string.dialog_permanent_delete_tasks_message, taskCount))
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBulkPermanentDelete() },
                ) {
                    Text(stringResource(R.string.action_delete_permanently))
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
}
