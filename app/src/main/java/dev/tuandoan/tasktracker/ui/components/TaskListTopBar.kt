package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.domain.model.TaskSort

/**
 * Enhanced top app bar for the task list screen with improved Material 3 styling
 * Uses CenterAlignedTopAppBar for better visual hierarchy and clean actions layout
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
    onBulkArchive: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = if (isSelectionMode) {
                    "$selectedCount selected"
                } else {
                    "Task Tracker"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = if (isSelectionMode) {
            {
                IconButton(
                    onClick = onClearSelection,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear selection",
                    )
                }
            }
        } else {
            { } // Empty composable when not in selection mode
        },
        actions = {
            if (isSelectionMode) {
                // Selection mode actions with improved styling
                IconButton(
                    onClick = onBulkMarkActive,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Mark as active",
                    )
                }

                IconButton(
                    onClick = onBulkMarkCompleted,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = "Mark as completed",
                    )
                }

                IconButton(
                    onClick = onBulkArchive,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archive selected",
                    )
                }

                // More options menu with improved styling
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Select all",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            onClick = {
                                onSelectAll()
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                        )
                    }
                }
            } else {
                // Normal mode actions with improved visual hierarchy
                IconButton(
                    onClick = { showSortMenu = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort tasks",
                    )
                }

                IconButton(
                    onClick = onStatsClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Statistics",
                    )
                }

                IconButton(
                    onClick = onArchiveClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "View archived tasks",
                    )
                }

                // Sort menu dropdown
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                ) {
                    SortMenu(
                        currentSort = currentSort,
                        onSortSelected = { sort ->
                            onSortChanged(sort)
                            showSortMenu = false
                        },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
