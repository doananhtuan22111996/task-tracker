package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R

/**
 * Enhanced top app bar for the task list screen with improved Material 3 styling
 * Uses CenterAlignedTopAppBar for better visual hierarchy and clean actions layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTopBar(
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onBulkMarkCompleted: () -> Unit = {},
    onBulkMarkActive: () -> Unit = {},
    onBulkArchive: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = if (isSelectionMode) {
                    stringResource(R.string.title_selected_count, selectedCount)
                } else {
                    stringResource(R.string.title_task_tracker)
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
                        contentDescription = stringResource(R.string.cd_clear_selection),
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
                        contentDescription = stringResource(R.string.cd_mark_active),
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
                        contentDescription = stringResource(R.string.cd_mark_completed),
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
                        contentDescription = stringResource(R.string.cd_archive_selected),
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
                            contentDescription = stringResource(R.string.cd_more_options),
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
                                    stringResource(R.string.action_select_all),
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
                    onClick = onStatsClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = stringResource(R.string.cd_statistics),
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
                        contentDescription = stringResource(R.string.cd_view_archived),
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
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
