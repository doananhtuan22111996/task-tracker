package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
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
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R

/**
 * Enhanced top app bar for the task list screen with improved Material 3 styling.
 * Supports three modes: normal, selection, and search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTopBar(
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onBulkMarkCompleted: () -> Unit = {},
    onBulkMarkActive: () -> Unit = {},
    onBulkArchive: () -> Unit = {},
    onBulkChangePriority: () -> Unit = {},
    onBulkApplyTag: () -> Unit = {},
    hasAnyTags: Boolean = false,
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    hasNonDefaultSort: Boolean = false,
    onSortClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchOpen: () -> Unit = {},
    onSearchClose: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            when {
                isSelectionMode -> {
                    Text(
                        text = stringResource(R.string.title_selected_count, selectedCount),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        // Polite live region so TalkBack announces "N selected" on each count change (BO-12).
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                isSearchActive -> {
                    SearchTitle(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.title_task_tracker),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        navigationIcon = when {
            isSelectionMode -> {
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
            }
            isSearchActive -> {
                {
                    IconButton(onClick = onSearchClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_close_search),
                        )
                    }
                }
            }
            else -> {
                { } // Empty composable when not in selection mode
            }
        },
        actions = {
            when {
                isSelectionMode -> {
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
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.action_change_priority),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                                onClick = {
                                    onBulkChangePriority()
                                    showMoreMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                            )
                            if (hasAnyTags) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.action_apply_tag),
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    },
                                    onClick = {
                                        onBulkApplyTag()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Label,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                isSearchActive -> {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.cd_clear_search),
                            )
                        }
                    }
                }
                else -> {
                    // Normal mode: Search + Sort + Settings
                    IconButton(
                        onClick = onSearchOpen,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.cd_search),
                        )
                    }

                    IconButton(
                        onClick = onSortClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (hasNonDefaultSort) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SortByAlpha,
                            contentDescription = stringResource(R.string.cd_sort_tasks),
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
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SearchTitle(query: String, onQueryChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (query.isEmpty()) {
            Text(
                text = stringResource(R.string.search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { keyboardController?.hide() },
            ),
        )
    }
}
