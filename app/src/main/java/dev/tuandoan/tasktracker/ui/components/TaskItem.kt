package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import dev.tuandoan.tasktracker.utils.formatDueDate
import dev.tuandoan.tasktracker.utils.isOverdue

@Composable
fun TaskItem(
    modifier: Modifier = Modifier,
    task: Task,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onPinClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onToggleSelection: () -> Unit = {},
) {
    val completedAlpha = if (task.isCompleted) 0.65f else 1f
    val overdue = task.dueAt?.let { !task.isCompleted && isOverdue(it) } ?: false

    val titleTextDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
    val supportingTextDecoration = if (task.isCompleted) TextDecoration.LineThrough else null

    // Chips: Overdue (if any), Tag (if any), Priority (if non-medium)
    val chips = buildList {
        if (overdue) add("OVERDUE" to ChipType.Overdue)
        if (!task.tag.isNullOrBlank()) add(task.tag.uppercase() to ChipType.Tag)
        if (task.priority != Priority.MEDIUM.value) {
            add(Priority.fromValue(task.priority).displayName.uppercase() to ChipType.Priority)
        }
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelection() else onEditClick()
                },
                onLongClick = {
                    if (!isSelectionMode) onLongPress()
                },
                role = Role.Button,
            )
            .alpha(completedAlpha),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
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
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // LEFT: Checkbox (selection mode uses selection state; normal uses completed state)
            val checkboxChecked = if (isSelectionMode) isSelected else task.isCompleted
            val checkboxOnChange: (Boolean) -> Unit = {
                if (isSelectionMode) onToggleSelection() else onToggleComplete()
            }

            Checkbox(
                checked = checkboxChecked,
                onCheckedChange = checkboxOnChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // CENTER: Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = titleTextDecoration,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Description (optional)
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = supportingTextDecoration,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(0.9f),
                    )
                }

                // Due date row (optional)
                task.dueAt?.let { dueDate ->
                    val dueDateText = if (overdue) {
                        "Overdue: ${formatDueDate(dueDate)}"
                    } else {
                        "Due: ${formatDueDate(dueDate)}"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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

                // Chips row (optional) – placed BELOW content (never above title)
                if (chips.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        chips.forEach { (label, type) ->
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingIcon = {
                                    if (type == ChipType.Priority) {
                                        val priority = Priority.fromValue(task.priority)
                                        Icon(
                                            imageVector = when (priority) {
                                                Priority.HIGH -> Icons.Default.KeyboardArrowUp
                                                Priority.LOW -> Icons.Default.KeyboardArrowDown
                                                else -> Icons.Default.Remove
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = when (type) {
                                        ChipType.Overdue -> MaterialTheme.colorScheme.errorContainer.copy(
                                            alpha = 0.75f,
                                        )

                                        ChipType.Tag -> MaterialTheme.colorScheme.secondaryContainer
                                        ChipType.Priority -> {
                                            val p = Priority.fromValue(task.priority)
                                            when (p) {
                                                Priority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(
                                                    alpha = 0.65f,
                                                )

                                                Priority.LOW -> MaterialTheme.colorScheme.tertiaryContainer.copy(
                                                    alpha = 0.8f,
                                                )

                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        }
                                    },
                                    labelColor = when (type) {
                                        ChipType.Overdue -> MaterialTheme.colorScheme.onErrorContainer
                                        ChipType.Tag -> MaterialTheme.colorScheme.onSecondaryContainer
                                        ChipType.Priority -> {
                                            val p = Priority.fromValue(task.priority)
                                            when (p) {
                                                Priority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                                                Priority.LOW -> MaterialTheme.colorScheme.onTertiaryContainer
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        }
                                    },
                                ),
                                border = null,
                                modifier = Modifier.height(32.dp),
                            )
                        }
                    }
                }
            }

            // RIGHT: Pin + overflow (only when not selection mode)
            if (!isSelectionMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Top,
                ) {
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
        }
    }
}

private enum class ChipType {
    Overdue,
    Tag,
    Priority,
}
