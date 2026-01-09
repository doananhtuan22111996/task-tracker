package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.tuandoan.tasktracker.domain.model.TaskSort

/**
 * Top app bar for the task list screen with sort functionality and selection mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTopBar(
    currentSort: TaskSort,
    onSortChanged: (TaskSort) -> Unit,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onBulkMarkCompleted: () -> Unit = {},
    onBulkMarkActive: () -> Unit = {},
    onBulkDelete: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = if (isSelectionMode) {
                    "$selectedCount selected"
                } else {
                    "Tasks"
                }
            )
        },
        navigationIcon = if (isSelectionMode) {
            {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear selection"
                    )
                }
            }
        } else {
            { } // Empty composable when not in selection mode
        },
        actions = {
            if (isSelectionMode) {
                // Selection mode actions
                IconButton(onClick = onBulkMarkActive) {
                    Icon(
                        imageVector = Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Mark as active"
                    )
                }

                IconButton(onClick = onBulkMarkCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = "Mark as completed"
                    )
                }

                IconButton(onClick = onBulkDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete selected"
                    )
                }

                // More options menu
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select all") },
                            onClick = {
                                onSelectAll()
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            } else {
                // Normal mode actions
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort tasks"
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortMenu(
                            currentSort = currentSort,
                            onSortSelected = { sort ->
                                onSortChanged(sort)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }
    )
}