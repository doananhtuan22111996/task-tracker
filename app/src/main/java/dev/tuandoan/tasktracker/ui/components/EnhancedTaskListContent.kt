package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.theme.TaskTrackerTheme
import java.text.SimpleDateFormat
import java.util.*

data class TaskSection(val dateKey: String, val label: String, val tasks: List<Task>)

@Composable
fun EnhancedTaskListContent(
    sections: List<TaskSection>,
    selectedTaskIds: Set<Long> = emptySet(),
    showSelectionMode: Boolean = false,
    onToggleComplete: (Task) -> Unit,
    onTogglePin: (Task) -> Unit,
    onToggleSelection: (Task) -> Unit,
    onMenuClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 88.dp, // Space for FAB and bottom navigation
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sections.forEach { section ->
            // Sticky header for each section
            stickyHeader(key = "header_${section.dateKey}") {
                SectionHeader(
                    dateKey = section.dateKey,
                    label = section.label,
                )
            }

            // Task items in this section
            itemsIndexed(
                items = section.tasks,
                key = { _, task -> task.id },
            ) { index, task ->
                TaskListItem(
                    task = task,
                    isSelected = selectedTaskIds.contains(task.id),
                    showSelectionMode = showSelectionMode,
                    onToggleComplete = onToggleComplete,
                    onTogglePin = onTogglePin,
                    onToggleSelection = onToggleSelection,
                    onMenuClick = onMenuClick,
                )
            }

            // Add space after each section except the last one
            if (section != sections.last()) {
                item(key = "spacer_${section.dateKey}") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Helper function to group tasks by date
fun groupTasksByDate(tasks: List<Task>): List<TaskSection> {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    return tasks
        .groupBy { task ->
            val taskDate = Calendar.getInstance().apply {
                timeInMillis = task.createdAt
            }
            dateFormat.format(taskDate.time)
        }
        .map { (dateKey, tasksInGroup) ->
            val date = dateFormat.parse(dateKey)!!
            val taskDate = Calendar.getInstance().apply { time = date }

            val label = when {
                isSameDay(taskDate, today) -> "Today"
                isSameDay(taskDate, yesterday) -> "Yesterday"
                isSameDay(taskDate, tomorrow) -> "Tomorrow"
                else -> displayDateFormat.format(date)
            }

            TaskSection(
                dateKey = dateKey,
                label = label,
                tasks = tasksInGroup.sortedWith(
                    compareByDescending<Task> { it.isPinned }
                        .thenBy { it.isCompleted }
                        .thenByDescending { it.priority }
                        .thenByDescending { it.createdAt },
                ),
            )
        }
        .sortedByDescending { section ->
            dateFormat.parse(section.dateKey)?.time ?: 0L
        }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

@Preview(showBackground = true)
@Composable
fun EnhancedTaskListContentPreview() {
    TaskTrackerTheme {
        val sampleTasks = listOf(
            Task(
                id = 1,
                title = "Complete project documentation",
                description = "Write comprehensive docs for the new feature",
                isCompleted = false,
                isPinned = true,
                tag = "Work",
                dueAt = System.currentTimeMillis() + 86400000, // Tomorrow
                priority = 2,
                createdAt = System.currentTimeMillis(),
            ),
            Task(
                id = 2,
                title = "Fix critical bug",
                description = "Users unable to process payments",
                isCompleted = false,
                isPinned = false,
                tag = "Bug",
                dueAt = System.currentTimeMillis() - 86400000, // Yesterday (overdue)
                priority = 2,
                createdAt = System.currentTimeMillis(),
            ),
            Task(
                id = 3,
                title = "Update user interface",
                description = "Apply new Material 3 design system",
                isCompleted = true,
                isPinned = false,
                tag = "Design",
                dueAt = System.currentTimeMillis() - 172800000, // 2 days ago
                priority = 1,
                createdAt = System.currentTimeMillis() - 86400000, // Yesterday
            ),
            Task(
                id = 4,
                title = "Review pull requests",
                description = "",
                isCompleted = false,
                isPinned = false,
                tag = "Code Review",
                dueAt = null,
                priority = 1,
                createdAt = System.currentTimeMillis() - 86400000, // Yesterday
            ),
        )

        val sections = groupTasksByDate(sampleTasks)

        EnhancedTaskListContent(
            sections = sections,
            selectedTaskIds = setOf(2L),
            showSelectionMode = false,
            onToggleComplete = {},
            onTogglePin = {},
            onToggleSelection = {},
            onMenuClick = {},
        )
    }
}
