package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
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
    streakMap: Map<Long, Int> = emptyMap(),
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onPinTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onDuplicateTask: (Task) -> Unit,
    onSkipOccurrence: (Task) -> Unit = {},
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

        // Task List or Empty State
        TaskListOrEmptyState(
            allTasks = allTasks,
            visibleTasks = visibleTasks,
            groupedVisibleTasks = groupedVisibleTasks,
            searchQuery = searchQuery,
            currentFilter = currentFilter,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            streakMap = streakMap,
            onToggleTaskComplete = onToggleTaskComplete,
            onEditTask = onEditTask,
            onArchiveTask = onArchiveTask,
            onPinTask = onPinTask,
            onLongPressTask = onLongPressTask,
            onDuplicateTask = onDuplicateTask,
            onSkipOccurrence = onSkipOccurrence,
            onToggleSelection = onToggleSelection,
            onClearSearch = onClearSearch,
            onChangeFilter = { /* Filter change now handled by bottom navigation */ },
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
    streakMap: Map<Long, Int>,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onPinTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onDuplicateTask: (Task) -> Unit,
    onSkipOccurrence: (Task) -> Unit = {},
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
                streakMap = streakMap,
                onToggleTaskComplete = onToggleTaskComplete,
                onEditTask = onEditTask,
                onArchiveTask = onArchiveTask,
                onPinTask = onPinTask,
                onLongPressTask = onLongPressTask,
                onDuplicateTask = onDuplicateTask,
                onSkipOccurrence = onSkipOccurrence,
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
    streakMap: Map<Long, Int>,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onPinTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onDuplicateTask: (Task) -> Unit,
    onSkipOccurrence: (Task) -> Unit = {},
    onToggleSelection: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Increased spacing for Material 3 ListItem design
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        contentPadding = PaddingValues(
            bottom = 104.dp, // FAB height (56dp) + bottom nav height (80dp) - overlap (32dp) = proper clearance
        ),
    ) {
        taskSections.forEach { section ->
            // Sticky header for each day section
            stickyHeader(key = section.dateKey) {
                TaskSectionHeader(
                    header = section.header,
                    itemCount = section.tasks.size,
                    isToday = section.isToday,
                )
            }

            // Tasks in this section
            items(
                items = section.tasks,
                key = { task -> task.id },
            ) { task ->
                TaskItem(
                    task = task,
                    streakCount = streakMap[task.parentRecurringTaskId ?: task.id] ?: 0,
                    isSelected = selectedIds.contains(task.id),
                    isSelectionMode = isSelectionMode,
                    onToggleComplete = { onToggleTaskComplete(task) },
                    onEditClick = { onEditTask(task) },
                    onArchiveClick = { onArchiveTask(task) },
                    onDuplicateClick = { onDuplicateTask(task) },
                    onSkipOccurrence = { onSkipOccurrence(task) },
                    onPinClick = { onPinTask(task) },
                    onLongPress = { onLongPressTask(task.id) },
                    onToggleSelection = { onToggleSelection(task.id) },
                )
            }
        }
    }
}

/**
 * Centered pill-style header for task sections matching HTML reference design
 *
 * Visual goals:
 * - Centered pill/capsule label (e.g., "TODAY")
 * - Uppercase, bold, tracking wide
 * - For "Today" use primary with ~10% alpha (light) / ~20% alpha (dark)
 * - For other days use neutral surface variant
 * - Optional count badge on the right
 */
@Composable
fun TaskSectionHeader(modifier: Modifier = Modifier, header: String, itemCount: Int? = null, isToday: Boolean = false) {
    // Colors based on HTML reference design
    val containerColor = if (isToday) {
        // Today: bg-primary/10 (light), bg-primary/20 (dark)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        // Other days: bg-slate-200 (light), bg-slate-800 (dark)
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .semantics { heading() },
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Main section pill
            Surface(
                shape = MaterialTheme.shapes.extraLarge, // rounded-full
                color = containerColor,
            ) {
                Text(
                    text = header.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp, // tracking-widest
                    ),
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), // px-4 py-1.5
                )
            }

            // Optional count badge
            itemCount?.let { count ->
                if (count > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge, // rounded-full
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp),
                    ) {
                        Box(
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
