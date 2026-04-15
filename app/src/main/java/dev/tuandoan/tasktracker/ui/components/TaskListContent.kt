package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter
import dev.tuandoan.tasktracker.utils.TaskSection

/** Bottom content padding to clear the FAB (56dp height + 32dp spacing). */
private val FAB_CLEARANCE = 88.dp

/**
 * Main content area of the task list screen.
 * Filter chips, tag chips, and feature tips scroll inline with the task list.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    showFabTip: Boolean = false,
    showTagTip: Boolean = false,
    fabTipText: String = "",
    tagTipText: String = "",
    onFilterChange: (TaskFilter) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onDismissFabTip: () -> Unit = {},
    onDismissTagTip: () -> Unit = {},
    onClearSearch: () -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onPinTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onDuplicateTask: (Task) -> Unit,
    onSkipOccurrence: (Task) -> Unit = {},
    onToggleSelection: (Long) -> Unit,
    bottomBarPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    when {
        // No tasks at all — chips fixed at top, empty state centered below
        allTasks.isEmpty() -> {
            Column(modifier = modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                TaskFilterChipRow(currentFilter = currentFilter, onFilterChange = onFilterChange)
                TagChipRow(
                    currentTagFilter = currentTagFilter,
                    availableTags = availableTags,
                    onTagFilterChange = onTagFilterChange,
                )
                FeatureTip(text = fabTipText, visible = showFabTip, onDismiss = onDismissFabTip)
                Box(modifier = Modifier.weight(1f)) {
                    EmptyTaskList()
                }
            }
        }

        // Tasks exist but filters/search produced zero results — chips fixed, empty results centered
        visibleTasks.isEmpty() -> {
            Column(modifier = modifier.fillMaxSize().padding(bottom = bottomBarPadding)) {
                TaskFilterChipRow(currentFilter = currentFilter, onFilterChange = onFilterChange)
                TagChipRow(
                    currentTagFilter = currentTagFilter,
                    availableTags = availableTags,
                    onTagFilterChange = onTagFilterChange,
                )
                Box(modifier = Modifier.weight(1f)) {
                    EmptySearchResults(
                        hasQuery = searchQuery.isNotEmpty(),
                        filter = currentFilter,
                        onClearSearch = onClearSearch,
                        onChangeFilter = { onFilterChange(TaskFilter.ALL) },
                    )
                }
            }
        }

        // Has visible tasks — everything scrolls together in one LazyColumn
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
                contentPadding = PaddingValues(bottom = FAB_CLEARANCE + bottomBarPadding),
            ) {
                // Filter chips (All / Active / Completed)
                item(key = "filter_chips") {
                    TaskFilterChipRow(
                        currentFilter = currentFilter,
                        onFilterChange = onFilterChange,
                    )
                }

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

                // Feature tips
                if (showFabTip) {
                    item(key = "tip_fab") {
                        FeatureTip(
                            text = fabTipText,
                            visible = true,
                            onDismiss = onDismissFabTip,
                        )
                    }
                }
                if (showTagTip) {
                    item(key = "tip_tag") {
                        FeatureTip(
                            text = tagTipText,
                            visible = true,
                            onDismiss = onDismissTagTip,
                        )
                    }
                }

                // Grouped task sections with sticky headers
                groupedVisibleTasks.forEach { section ->
                    stickyHeader(key = section.dateKey) {
                        TaskSectionHeader(
                            header = section.header,
                            itemCount = section.tasks.size,
                            isToday = section.isToday,
                            modifier = Modifier.padding(horizontal = AppSpacing.screenPadding),
                        )
                    }

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
                            modifier = Modifier.padding(horizontal = AppSpacing.screenPadding),
                        )
                    }
                }
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
            .padding(vertical = 12.dp)
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
