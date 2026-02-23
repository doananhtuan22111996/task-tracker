package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.utils.TaskDateGrouper
import dev.tuandoan.tasktracker.utils.TaskSection

/**
 * Content area of the archived tasks screen containing search and archived task list
 */
@Composable
fun ArchivedTaskListContent(
    archivedTasks: List<Task>,
    searchQuery: String,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onRestoreTask: (Task) -> Unit,
    onPermanentDeleteTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
    ) {
        // Search Field - optimized spacing
        SearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearClick = onClearSearch,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // Filter archived tasks by search query
        val filteredTasks = if (searchQuery.isBlank()) {
            archivedTasks
        } else {
            archivedTasks.filter { task ->
                val lowercaseQuery = searchQuery.lowercase()
                task.title.lowercase().contains(lowercaseQuery) ||
                    task.description.lowercase().contains(lowercaseQuery)
            }
        }

        // Task List or Empty State
        if (archivedTasks.isEmpty()) {
            EmptyArchivedTaskList()
        } else if (filteredTasks.isEmpty() && searchQuery.isNotBlank()) {
            EmptySearchResults(
                hasQuery = true,
                onClearSearch = onClearSearch,
            )
        } else {
            val groupedTasks = TaskDateGrouper.groupTasksByDay(filteredTasks, LocalContext.current)

            ArchivedTaskList(
                taskSections = groupedTasks,
                selectedIds = selectedIds,
                isSelectionMode = isSelectionMode,
                onRestoreTask = onRestoreTask,
                onPermanentDeleteTask = onPermanentDeleteTask,
                onLongPressTask = onLongPressTask,
                onToggleSelection = onToggleSelection,
            )
        }
    }
}

/**
 * Scrollable list of archived tasks grouped by archive date
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArchivedTaskList(
    taskSections: List<TaskSection>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onRestoreTask: (Task) -> Unit,
    onPermanentDeleteTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(
            bottom = 16.dp, // Proper clearance for navigation bars
        ),
    ) {
        taskSections.forEach { section ->
            // Sticky header for each archive date section
            stickyHeader(key = section.dateKey) {
                TaskSectionHeader(
                    header = section.header,
                )
            }

            // Tasks in this section
            items(
                items = section.tasks,
                key = { task -> task.id },
            ) { task ->
                ArchivedTaskItem(
                    task = task,
                    isSelected = selectedIds.contains(task.id),
                    isSelectionMode = isSelectionMode,
                    onRestoreClick = { onRestoreTask(task) },
                    onPermanentDeleteClick = { onPermanentDeleteTask(task) },
                    onLongPress = { onLongPressTask(task.id) },
                    onToggleSelection = { onToggleSelection(task.id) },
                )
            }
        }
    }
}

/**
 * Empty state when no tasks are archived
 */
@Composable
private fun EmptyArchivedTaskList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_no_archived),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.empty_archived_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Empty state for search results in archived tasks
 */
@Composable
private fun EmptySearchResults(hasQuery: Boolean, onClearSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_no_archived_found),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )

        if (hasQuery) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClearSearch,
                colors = ButtonDefaults.textButtonColors(),
            ) {
                Text(stringResource(R.string.action_clear_search))
            }
        }
    }
}
