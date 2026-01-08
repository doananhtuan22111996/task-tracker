package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.ui.components.EmptySearchResults
import dev.tuandoan.tasktracker.ui.components.EmptyTaskList
import dev.tuandoan.tasktracker.ui.components.FilterTabs
import dev.tuandoan.tasktracker.ui.components.SearchField
import dev.tuandoan.tasktracker.ui.components.TaskItem
import dev.tuandoan.tasktracker.ui.viewmodel.TaskViewModel
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val visibleTasks by viewModel.visibleTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()
    val showAddTaskDialog by viewModel.showAddTaskDialog.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Field
            SearchField(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                onClearClick = viewModel::clearSearch
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Tabs
            FilterTabs(
                currentFilter = currentFilter,
                onFilterChange = viewModel::setFilter
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Task List or Empty State
            if (allTasks.isEmpty()) {
                EmptyTaskList()
            } else if (visibleTasks.isEmpty()) {
                EmptySearchResults(
                    hasQuery = searchQuery.isNotEmpty(),
                    filter = currentFilter,
                    onClearSearch = viewModel::clearSearch,
                    onChangeFilter = { viewModel.setFilter(TaskFilter.ALL) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = visibleTasks,
                        key = { task -> task.id }
                    ) { task ->
                        TaskItem(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onEditClick = { viewModel.showEditTaskDialog(task) },
                            onDeleteClick = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddEditTaskDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideAddTaskDialog() }
        )
    }
}