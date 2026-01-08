package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.tuandoan.tasktracker.domain.model.TaskSort

/**
 * Top app bar for the task list screen with sort functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTopBar(
    currentSort: TaskSort,
    onSortChanged: (TaskSort) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Tasks") },
        actions = {
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
    )
}