package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.ui.theme.CustomShapes
import dev.tuandoan.tasktracker.utils.formatDueDate
import dev.tuandoan.tasktracker.utils.isOverdue

/**
 * Enhanced task item with Material 3 ListItem hybrid design and improved visual differentiation
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
    val completedAlpha = if (task.isCompleted) 0.6f else 1f
    val overdue = task.dueAt?.let { !task.isCompleted && isOverdue(it) } ?: false

    Card(
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
            ),
        shape = CustomShapes.taskItem,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                task.isCompleted -> MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f)
                overdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                task.isPinned -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when {
                isSelected -> 4.dp
                task.isPinned -> 2.dp
                else -> 1.dp
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacing.taskItemHorizontalPadding,
                    vertical = AppSpacing.taskItemVerticalPadding,
                )
                .alpha(completedAlpha),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Leading: Checkbox with minimum touch target
            Box(
                modifier = Modifier.size(AppSpacing.minTouchTarget),
                contentAlignment = Alignment.Center,
            ) {
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
            }

            Spacer(modifier = Modifier.width(AppSpacing.medium))

            // Main content area
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // Headline: Task title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (task.isPinned) FontWeight.SemiBold else FontWeight.Normal,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Supporting text: Description (if present)
                if (task.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Metadata row: Due date, tag, priority
                val hasMetadata =
                    task.dueAt != null || !task.tag.isNullOrEmpty() || task.priority != Priority.MEDIUM.value
                if (hasMetadata) {
                    Spacer(modifier = Modifier.height(AppSpacing.small))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.chipSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Due date with overdue indicator
                        task.dueAt?.let { dueDate ->
                            item {
                                Surface(
                                    color = if (overdue) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    },
                                    shape = CustomShapes.chip,
                                    tonalElevation = if (overdue) 2.dp else 0.dp,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = AppSpacing.small,
                                            vertical = AppSpacing.extraSmall,
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = if (overdue) Icons.Default.Error else Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (overdue) {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            } else {
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            },
                                        )
                                        Spacer(modifier = Modifier.width(AppSpacing.extraSmall))
                                        Text(
                                            text = if (overdue) "Overdue" else formatDueDate(dueDate),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (overdue) {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            } else {
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            },
                                            fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }

                        // Tag chip
                        if (!task.tag.isNullOrEmpty()) {
                            item {
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = task.tag,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                    border = null,
                                    shape = CustomShapes.chip,
                                )
                            }
                        }

                        // Priority indicator (only for High and Low priority)
                        if (task.priority != Priority.MEDIUM.value) {
                            item {
                                val priority = Priority.fromValue(task.priority)
                                Surface(
                                    color = when (priority) {
                                        Priority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                        Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = CustomShapes.chip,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = AppSpacing.small,
                                            vertical = AppSpacing.extraSmall,
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = when (priority) {
                                                Priority.HIGH -> Icons.Default.KeyboardArrowUp
                                                Priority.LOW -> Icons.Default.KeyboardArrowDown
                                                else -> Icons.Default.Remove
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = when (priority) {
                                                Priority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                                                Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                        Spacer(modifier = Modifier.width(AppSpacing.extraSmall))
                                        Text(
                                            text = priority.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (priority) {
                                                Priority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                                                Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Trailing: Action buttons or pin indicator
            if (!isSelectionMode) {
                Row {
                    // Pin indicator/toggle
                    IconButton(
                        onClick = onPinClick,
                        modifier = Modifier.size(AppSpacing.minTouchTarget),
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

                    // Overflow menu trigger
                    IconButton(
                        onClick = onArchiveClick,
                        modifier = Modifier.size(AppSpacing.minTouchTarget),
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
        }
    }
}
