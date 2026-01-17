package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.ui.theme.CustomShapes
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter
import dev.tuandoan.tasktracker.utils.TaskSection

/**
 * Main content area of the task list screen containing search, filter, and task list
 */
@Composable
fun TaskListContent(
    allTasks: List<Task>,
    visibleTasks: List<Task>,
    groupedVisibleTasks: List<TaskSection>,
    searchQuery: String,
    currentFilter: TaskFilter,
    currentTagFilter: String?,
    availableTags: List<String>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onPinTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.screenPadding),
    ) {
        // Search Field - placed directly below TopAppBar with minimal spacing
        SearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearClick = onClearSearch,
        )

        Spacer(modifier = Modifier.height(AppSpacing.small))

        // Filter Tabs - compact spacing for cohesive feel
        FilterTabs(
            currentFilter = currentFilter,
            onFilterChange = onFilterChange,
            currentTagFilter = currentTagFilter,
            availableTags = availableTags,
            onTagFilterChange = onTagFilterChange,
        )

        Spacer(modifier = Modifier.height(AppSpacing.small))

        // Task List or Empty State
        TaskListOrEmptyState(
            allTasks = allTasks,
            visibleTasks = visibleTasks,
            groupedVisibleTasks = groupedVisibleTasks,
            searchQuery = searchQuery,
            currentFilter = currentFilter,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            onToggleTaskComplete = onToggleTaskComplete,
            onEditTask = onEditTask,
            onArchiveTask = onArchiveTask,
            onPinTask = onPinTask,
            onLongPressTask = onLongPressTask,
            onToggleSelection = onToggleSelection,
            onClearSearch = onClearSearch,
            onChangeFilter = { onFilterChange(TaskFilter.ALL) },
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
    groupedVisibleTasks: List<TaskSection>,
    searchQuery: String,
    currentFilter: TaskFilter,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onPinTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onClearSearch: () -> Unit,
    onChangeFilter: () -> Unit,
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
                onChangeFilter = onChangeFilter,
            )
        }
        else -> {
            GroupedTaskList(
                taskSections = groupedVisibleTasks,
                selectedIds = selectedIds,
                isSelectionMode = isSelectionMode,
                onToggleTaskComplete = onToggleTaskComplete,
                onEditTask = onEditTask,
                onArchiveTask = onArchiveTask,
                onPinTask = onPinTask,
                onLongPressTask = onLongPressTask,
                onToggleSelection = onToggleSelection,
            )
        }
    }
}

/**
 * Scrollable list of tasks grouped by day with sticky section headers
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedTaskList(
    taskSections: List<TaskSection>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onPinTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
        contentPadding = PaddingValues(
            bottom = 88.dp, // FAB height (56dp) + margin (32dp) for proper clearance
        ),
    ) {
        taskSections.forEach { section ->
            // Sticky header for each day section
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
                TaskItem(
                    task = task,
                    isSelected = selectedIds.contains(task.id),
                    isSelectionMode = isSelectionMode,
                    onToggleComplete = { onToggleTaskComplete(task) },
                    onEditClick = { onEditTask(task) },
                    onArchiveClick = { onArchiveTask(task) },
                    onPinClick = { onPinTask(task) },
                    onLongPress = { onLongPressTask(task.id) },
                    onToggleSelection = { onToggleSelection(task.id) },
                )
            }
        }
    }
}

/**
 * Enhanced header for a task section (Today, Yesterday, etc.) with improved Material 3 styling
 */
@Composable
fun TaskSectionHeader(header: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.98f),
        shape = CustomShapes.sectionHeader,
        tonalElevation = 2.dp,
    ) {
        Column {
            Text(
                text = header,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = AppSpacing.large,
                    vertical = AppSpacing.small, // Compact header padding per layout guidelines
                ),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp,
            )
        }
    }
}
