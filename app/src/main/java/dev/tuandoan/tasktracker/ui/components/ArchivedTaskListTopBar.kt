package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.domain.model.TaskSort

/**
 * Enhanced top app bar for the archived tasks screen with improved Material 3 styling
 * Mirrors the main screen improvements with CenterAlignedTopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedTaskListTopBar(
    currentSort: TaskSort,
    onSortChanged: (TaskSort) -> Unit,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onBulkRestore: () -> Unit = {},
    onBulkPermanentDelete: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onNavigateBack: () -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = if (isSelectionMode) {
                    "$selectedCount selected"
                } else {
                    "Archived Tasks"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            if (isSelectionMode) {
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
            } else {
                IconButton(
                    onClick = onNavigateBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back",
                    )
                }
            }
        },
        actions = {
            if (isSelectionMode) {
                // Selection mode actions for archived tasks with enhanced styling
                IconButton(
                    onClick = onBulkRestore,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Unarchive,
                        contentDescription = "Restore selected",
                    )
                }

                IconButton(
                    onClick = onBulkPermanentDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Permanently delete selected",
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

                // Sort menu dropdown with enhanced styling
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
