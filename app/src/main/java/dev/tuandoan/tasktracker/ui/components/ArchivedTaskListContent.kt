package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.utils.TaskDateGrouper

/**
 * Content area of the archived tasks screen.
 * Tag chips scroll inline with the task list, matching TaskListScreen pattern.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchivedTaskListContent(
    archivedTasks: List<Task>,
    searchQuery: String,
    currentTagFilter: String?,
    availableTags: List<String>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onTagFilterChange: (String?) -> Unit,
    onRestoreTask: (Task) -> Unit,
    onPermanentDeleteTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    bottomBarPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
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

    // If archivedTasks is empty but tags exist, a tag filter is hiding results (not truly empty)
    val isTrulyEmpty = archivedTasks.isEmpty() && availableTags.isEmpty()

    when {
        // No archived tasks at all — tag chips fixed, empty state centered
        isTrulyEmpty -> {
            Column(modifier = modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyArchivedTaskList()
                }
            }
        }

        // Filters/search produced no results — tag chips fixed, empty results centered
        filteredTasks.isEmpty() -> {
            Column(modifier = modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                TagChipRow(
                    currentTagFilter = currentTagFilter,
                    availableTags = availableTags,
                    onTagFilterChange = onTagFilterChange,
                )
                Box(modifier = Modifier.weight(1f)) {
                    EmptyArchivedSearchResults()
                }
            }
        }

        // Has visible tasks — everything scrolls together in one LazyColumn
        else -> {
            val groupedTasks = TaskDateGrouper.groupTasksByDay(filteredTasks, LocalContext.current)

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
                contentPadding = PaddingValues(bottom = AppSpacing.large + bottomBarPadding),
            ) {
                // Tag chips
                if (availableTags.isNotEmpty()) {
                    item(key = "tag_chips") {
                        TagChipRow(
                            currentTagFilter = currentTagFilter,
                            availableTags = availableTags,
                            onTagFilterChange = onTagFilterChange,
                        )
                    }
                }

                // Grouped task sections with sticky headers
                groupedTasks.forEach { section ->
                    stickyHeader(key = section.dateKey) {
                        TaskSectionHeader(
                            header = section.header,
                            modifier = Modifier.padding(horizontal = AppSpacing.screenPadding),
                        )
                    }

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
                            modifier = Modifier.padding(horizontal = AppSpacing.screenPadding),
                        )
                    }
                }
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
            .padding(AppSpacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_no_archived),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(AppSpacing.small))

        Text(
            text = stringResource(R.string.empty_archived_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Empty state for search results in archived tasks
 */
@Composable
private fun EmptyArchivedSearchResults() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_no_archived_found),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
