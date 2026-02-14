package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    isDragEnabled: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onPinTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onReorderTasks: (sectionDateKey: String, reorderedTaskIds: List<Long>) -> Unit,
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

        // Tag Filter Chips - compact spacing for cohesive feel
        TagFilterChips(
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
            isDragEnabled = isDragEnabled,
            onToggleTaskComplete = onToggleTaskComplete,
            onEditTask = onEditTask,
            onArchiveTask = onArchiveTask,
            onPinTask = onPinTask,
            onLongPressTask = onLongPressTask,
            onToggleSelection = onToggleSelection,
            onReorderTasks = onReorderTasks,
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
    isDragEnabled: Boolean,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onPinTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onReorderTasks: (sectionDateKey: String, reorderedTaskIds: List<Long>) -> Unit,
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
                isDragEnabled = isDragEnabled,
                onToggleTaskComplete = onToggleTaskComplete,
                onEditTask = onEditTask,
                onArchiveTask = onArchiveTask,
                onPinTask = onPinTask,
                onLongPressTask = onLongPressTask,
                onToggleSelection = onToggleSelection,
                onReorderTasks = onReorderTasks,
            )
        }
    }
}

/**
 * Scrollable list of tasks grouped by day with sticky section headers.
 * Supports drag-and-drop reorder within sections when isDragEnabled is true.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedTaskList(
    taskSections: List<TaskSection>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    isDragEnabled: Boolean,
    onToggleTaskComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onPinTask: (Task) -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onReorderTasks: (sectionDateKey: String, reorderedTaskIds: List<Long>) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val currentOnReorderTasks by rememberUpdatedState(onReorderTasks)
    val currentTaskSections by rememberUpdatedState(taskSections)

    // Mutable state for sections during drag reorder.
    // Re-initialized when taskSections changes from outside (e.g., new data from DB).
    var mutableSections by remember(taskSections) { mutableStateOf(taskSections) }

    // Drag state
    var draggedTaskId by remember { mutableStateOf<Long?>(null) }
    var draggedSectionKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // Estimated item height for swap threshold (approximate; covers typical task cards)
    val density = LocalDensity.current
    val estimatedItemHeightPx = with(density) { 88.dp.toPx() }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        contentPadding = PaddingValues(
            bottom = 104.dp,
        ),
    ) {
        mutableSections.forEach { section ->
            // Sticky header for each day section
            stickyHeader(key = section.dateKey) {
                TaskSectionHeader(
                    header = section.header,
                    itemCount = section.tasks.size,
                )
            }

            // Tasks in this section
            items(
                items = section.tasks,
                key = { task -> task.id },
            ) { task ->
                val isDragging = draggedTaskId == task.id

                // Visual transform: apply elevation + scale to the dragged item,
                // animate displacement for other items.
                val itemModifier = if (isDragging) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer {
                            translationY = dragOffset
                            scaleX = 1.02f
                            scaleY = 1.02f
                            shadowElevation = 8f
                            alpha = 0.95f
                        }
                } else {
                    Modifier.animateItem()
                }

                // Single unified pointerInput for drag gesture.
                // Uses a stable key so the coroutine is NOT restarted on recomposition.
                val dragModifier = if (isDragEnabled) {
                    Modifier.pointerInput(task.id) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                draggedTaskId = task.id
                                draggedSectionKey = section.dateKey
                                dragOffset = 0f
                            },
                            onDragEnd = {
                                val sectionKey = draggedSectionKey
                                if (sectionKey != null) {
                                    val reorderedSection = mutableSections
                                        .find { it.dateKey == sectionKey }
                                    if (reorderedSection != null) {
                                        currentOnReorderTasks(
                                            sectionKey,
                                            reorderedSection.tasks.map { it.id },
                                        )
                                    }
                                }
                                draggedTaskId = null
                                draggedSectionKey = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggedTaskId = null
                                draggedSectionKey = null
                                dragOffset = 0f
                                mutableSections = currentTaskSections
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount

                                val sectionKey =
                                    draggedSectionKey ?: return@detectVerticalDragGestures
                                val currentSectionIndex = mutableSections
                                    .indexOfFirst { it.dateKey == sectionKey }
                                if (currentSectionIndex == -1) return@detectVerticalDragGestures

                                val currentSection = mutableSections[currentSectionIndex]
                                val currentIndex = currentSection.tasks
                                    .indexOfFirst { it.id == task.id }
                                if (currentIndex == -1) return@detectVerticalDragGestures

                                val indexDelta =
                                    (dragOffset / estimatedItemHeightPx).toInt()
                                val targetIndex = (currentIndex + indexDelta)
                                    .coerceIn(0, currentSection.tasks.size - 1)

                                if (targetIndex != currentIndex) {
                                    val newTasks = currentSection.tasks.toMutableList()
                                    val movedItem = newTasks.removeAt(currentIndex)
                                    newTasks.add(targetIndex, movedItem)

                                    mutableSections = mutableSections.toMutableList()
                                        .apply {
                                            this[currentSectionIndex] =
                                                currentSection.copy(tasks = newTasks)
                                        }

                                    dragOffset -= (targetIndex - currentIndex) *
                                        estimatedItemHeightPx
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                }

                TaskItem(
                    task = task,
                    isSelected = selectedIds.contains(task.id),
                    isSelectionMode = isSelectionMode,
                    showDragHandle = isDragEnabled,
                    onToggleComplete = { onToggleTaskComplete(task) },
                    onEditClick = { onEditTask(task) },
                    onArchiveClick = { onArchiveTask(task) },
                    onPinClick = { onPinTask(task) },
                    onLongPress = { onLongPressTask(task.id) },
                    onToggleSelection = { onToggleSelection(task.id) },
                    modifier = itemModifier.then(dragModifier),
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
fun TaskSectionHeader(modifier: Modifier = Modifier, header: String, itemCount: Int? = null) {
    // Determine if this is "Today" for special styling
    val isToday = header.equals("Today", ignoreCase = true)

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
            .padding(vertical = 16.dp), // Add vertical spacing
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
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
                        fontSize = 12.sp, // text-xs
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
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
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
