package dev.tuandoan.tasktracker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.theme.TaskTrackerTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListItem(
    task: Task,
    isSelected: Boolean = false,
    showSelectionMode: Boolean = false,
    onToggleComplete: (Task) -> Unit,
    onTogglePin: (Task) -> Unit,
    onToggleSelection: (Task) -> Unit,
    onMenuClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val completedAlpha = if (task.isCompleted) 0.6f else 1f
    val context = LocalContext.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .alpha(completedAlpha),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (task.isPinned) 2.dp else 1.dp,
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                task.isPinned -> MaterialTheme.colorScheme.surfaceContainerHighest
                else -> MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        ListItem(
            modifier = Modifier.padding(vertical = 4.dp),
            headlineContent = {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Description and due date
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }

                    // Due date display
                    task.dueAt?.let { dueTimestamp ->
                        val dueDate = Date(dueTimestamp)
                        val now = Date()
                        val isOverdue = dueDate.before(now) && !task.isCompleted

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isOverdue) {
                                    MaterialTheme.colorScheme.error
                                } else if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dueDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverdue) {
                                    MaterialTheme.colorScheme.error
                                } else if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }

                    // Chips row
                    TaskChipsRow(
                        task = task,
                        isSelected = isSelected,
                    )
                }
            },
            leadingContent = {
                if (showSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection(task) },
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleComplete(task) },
                        modifier = Modifier.size(24.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            },
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Pin toggle
                    IconButton(
                        onClick = { onTogglePin(task) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (task.isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (task.isPinned) {
                                stringResource(R.string.unpin_task)
                            } else {
                                stringResource(R.string.pin_task)
                            },
                            tint = if (task.isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    // Menu button
                    IconButton(
                        onClick = { onMenuClick(task) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskChipsRow(task: Task, isSelected: Boolean) {
    val chips = buildList {
        // Tag chip
        task.tag?.let { tag ->
            add(ChipData.Tag(tag))
        }

        // Overdue chip
        task.dueAt?.let { dueTimestamp ->
            val dueDate = Date(dueTimestamp)
            val now = Date()
            if (dueDate.before(now) && !task.isCompleted) {
                add(ChipData.Overdue)
            }
        }

        // Priority chip (only show for HIGH priority)
        if (task.priority == 2) { // HIGH priority
            add(ChipData.Priority("High"))
        }

        // Due time chip (if due today)
        task.dueAt?.let { dueTimestamp ->
            val dueDate = Date(dueTimestamp)
            val now = Date()
            val calendar = Calendar.getInstance()
            calendar.time = now
            val today = calendar.get(Calendar.DAY_OF_YEAR)
            calendar.time = dueDate
            val dueDay = calendar.get(Calendar.DAY_OF_YEAR)

            if (today == dueDay && !task.isCompleted) {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                add(ChipData.DueTime(timeFormat.format(dueDate)))
            }
        }
    }

    if (chips.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(chips) { chip ->
                when (chip) {
                    is ChipData.Tag -> {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = chip.text,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                labelColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                            ),
                            modifier = Modifier.height(32.dp),
                        )
                    }
                    is ChipData.Overdue -> {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = "Overdue",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.height(32.dp),
                        )
                    }
                    is ChipData.Priority -> {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = chip.text,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                                labelColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                leadingIconContentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                            ),
                            modifier = Modifier.height(32.dp),
                        )
                    }
                    is ChipData.DueTime -> {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = chip.text,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                labelColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                            modifier = Modifier.height(32.dp),
                        )
                    }
                }
            }
        }
    }
}

private sealed class ChipData {
    data class Tag(val text: String) : ChipData()
    object Overdue : ChipData()
    data class Priority(val text: String) : ChipData()
    data class DueTime(val text: String) : ChipData()
}

@Preview(showBackground = true)
@Composable
fun TaskListItemPreview() {
    TaskTrackerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal task
            TaskListItem(
                task = Task(
                    id = 1,
                    title = "Complete project documentation",
                    description = "Write comprehensive docs for the new feature",
                    isCompleted = false,
                    isPinned = false,
                    tag = "Work",
                    dueAt = System.currentTimeMillis() + 86400000, // Tomorrow
                    priority = 1,
                ),
                onToggleComplete = {},
                onTogglePin = {},
                onToggleSelection = {},
                onMenuClick = {},
            )

            // Overdue task with high priority
            TaskListItem(
                task = Task(
                    id = 2,
                    title = "Fix critical bug in payment system",
                    description = "Users unable to process payments",
                    isCompleted = false,
                    isPinned = true,
                    tag = "Bug",
                    dueAt = System.currentTimeMillis() - 86400000, // Yesterday (overdue)
                    priority = 2, // HIGH
                ),
                onToggleComplete = {},
                onTogglePin = {},
                onToggleSelection = {},
                onMenuClick = {},
            )

            // Completed task
            TaskListItem(
                task = Task(
                    id = 3,
                    title = "Update user interface",
                    description = "Apply new Material 3 design system",
                    isCompleted = true,
                    isPinned = false,
                    tag = "Design",
                    dueAt = System.currentTimeMillis() - 172800000, // 2 days ago
                    priority = 1,
                ),
                onToggleComplete = {},
                onTogglePin = {},
                onToggleSelection = {},
                onMenuClick = {},
            )
        }
    }
}
