package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter

/**
 * Main content area of the task list screen containing search, filter, and task list
 */
@Composable
fun TaskListContent(
    allTasks: List<Task>,
    visibleTasks: List<Task>,
    searchQuery: String,
    currentFilter: TaskFilter,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search Field
        SearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearClick = onClearSearch
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Tabs
        FilterTabs(
            currentFilter = currentFilter,
            onFilterChange = onFilterChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Task List or Empty State
        TaskListOrEmptyState(
            allTasks = allTasks,
            visibleTasks = visibleTasks,
            searchQuery = searchQuery,
            currentFilter = currentFilter,
            onToggleTaskComplete = onToggleTaskComplete,
            onEditTask = onEditTask,
            onDeleteTask = onDeleteTask,
            onClearSearch = onClearSearch,
            onChangeFilter = { onFilterChange(TaskFilter.ALL) }
        )
    }
}

/**
 * Displays task list or appropriate empty state based on current conditions
 */
@Composable
private fun TaskListOrEmptyState(
    allTasks: List<Task>,
    visibleTasks: List<Task>,
    searchQuery: String,
    currentFilter: TaskFilter,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onClearSearch: () -> Unit,
    onChangeFilter: () -> Unit
) {
    when {
        allTasks.isEmpty() -> {
            EmptyTaskList()
        }
        visibleTasks.isEmpty() -> {
            EmptySearchResults(
                hasQuery = searchQuery.isNotEmpty(),
                filter = currentFilter,
                onClearSearch = onClearSearch,
                onChangeFilter = onChangeFilter
            )
        }
        else -> {
            TaskList(
                tasks = visibleTasks,
                onToggleTaskComplete = onToggleTaskComplete,
                onEditTask = onEditTask,
                onDeleteTask = onDeleteTask
            )
        }
    }
}

/**
 * Scrollable list of tasks
 */
@Composable
private fun TaskList(
    tasks: List<Task>,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = tasks,
            key = { task -> task.id }
        ) { task ->
            TaskItem(
                task = task,
                onToggleComplete = { onToggleTaskComplete(task) },
                onEditClick = { onEditTask(task) },
                onDeleteClick = { onDeleteTask(task) }
            )
        }
    }
}