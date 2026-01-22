package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.Priority
import dev.tuandoan.tasktracker.utils.formatDueDate
import dev.tuandoan.tasktracker.utils.isOverdue

/**
 * Modern Material 3 task item using ElevatedCard + ListItem pattern for improved scannability
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onPinClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onToggleSelection: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val completedAlpha = if (task.isCompleted) 0.65f else 1f
    val overdue = task.dueAt?.let { !task.isCompleted && isOverdue(it) } ?: false

    // Build status chips list for overline
    val statusChips = buildList {
        if (overdue) add("Overdue")
        if (!task.tag.isNullOrEmpty()) add(task.tag)
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    } else {
                        onEditClick()
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        onLongPress()
                    }
                },
                role = Role.Button,
            )
            .alpha(completedAlpha),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                task.isPinned -> MaterialTheme.colorScheme.surfaceContainerHighest
                else -> MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = when {
                isSelected -> 6.dp
                task.isPinned -> 3.dp
                else -> 1.dp
            },
        ),
    ) {
        ListItem(
            modifier = Modifier.padding(
                horizontal = 4.dp,
                vertical = 2.dp,
            ),
            headlineContent = {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (task.isPinned) FontWeight.SemiBold else FontWeight.Normal,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Description (if present)
                    if (task.description.isNotEmpty()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Due date information
                    task.dueAt?.let { dueDate ->
                        val dueDateText = if (overdue) {
                            "Overdue: ${formatDueDate(dueDate)}"
                        } else {
                            "Due: ${formatDueDate(dueDate)}"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (overdue) Icons.Default.Error else Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (overdue) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dueDateText,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (overdue) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (overdue) FontWeight.Medium else FontWeight.Normal,
                            )
                        }
                    }

                    // Bottom chips row: Tag and Priority chips
                    if (statusChips.isNotEmpty() || task.priority != Priority.MEDIUM.value) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            // Render status chips (Overdue, Tag)
                            statusChips.forEach { chip ->
                                val isOverdueChip = chip == "Overdue"
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = chip,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isOverdueChip) {
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        labelColor = if (isOverdueChip) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        },
                                    ),
                                    border = null,
                                    modifier = Modifier.height(32.dp),
                                )
                            }

                            // Priority indicator (only for non-Medium priority)
                            if (task.priority != Priority.MEDIUM.value) {
                                val priority = Priority.fromValue(task.priority)
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = priority.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (priority) {
                                                Priority.HIGH -> Icons.Default.KeyboardArrowUp
                                                Priority.LOW -> Icons.Default.KeyboardArrowDown
                                                else -> Icons.Default.Remove
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = when (priority) {
                                            Priority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(
                                                alpha = 0.6f,
                                            )
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        labelColor = when (priority) {
                                            Priority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    ),
                                    border = null,
                                    modifier = Modifier.height(32.dp),
                                )
                            }
                        }
                    }
                }
            },
            leadingContent = {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                } else {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleComplete() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
            },
            trailingContent = {
                if (!isSelectionMode) {
                    Row {
                        // Pin indicator/toggle
                        IconButton(
                            onClick = onPinClick,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = if (task.isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (task.isPinned) "Unpin task" else "Pin task",
                                tint = if (task.isPinned) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // More options menu
                        IconButton(
                            onClick = onArchiveClick,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
        )
    }
}
