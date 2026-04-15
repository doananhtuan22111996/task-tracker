package dev.tuandoan.tasktracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.Priority
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.utils.formatDueDate
import dev.tuandoan.tasktracker.utils.isOverdue

@Composable
fun TaskItem(
    modifier: Modifier = Modifier,
    task: Task,
    streakCount: Int = 0,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onPinClick: () -> Unit,
    onDuplicateClick: () -> Unit = {},
    onSkipOccurrence: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onToggleSelection: () -> Unit = {},
) {
    val completedAlpha = if (task.isCompleted) 0.65f else 1f
    val overdue = task.dueAt?.let { !task.isCompleted && isOverdue(it) } ?: false

    val titleTextDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
    val supportingTextDecoration = if (task.isCompleted) TextDecoration.LineThrough else null

    // Accessibility state descriptions
    val checkedDesc = stringResource(R.string.a11y_checkbox_checked)
    val uncheckedDesc = stringResource(R.string.a11y_checkbox_unchecked)

    // Localized priority label
    val priorityLabel = when (Priority.fromValue(task.priority)) {
        Priority.LOW -> stringResource(R.string.priority_low)
        Priority.MEDIUM -> stringResource(R.string.priority_medium)
        Priority.HIGH -> stringResource(R.string.priority_high)
    }

    // Chips: Tag (if any), Priority (if non-medium)
    val chips = buildList {
        if (!task.tag.isNullOrBlank()) add(task.tag.uppercase() to ChipType.Tag)
        if (task.priority != Priority.MEDIUM.value) {
            add(priorityLabel.uppercase() to ChipType.Priority)
        }
    }

    // Animated container color based on state
    val baseContainerColor = when {
        task.isPinned -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val selectedOverlayColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)

    val animatedContainerColor by animateColorAsState(
        targetValue = when {
            isSelected -> selectedOverlayColor.compositeOver(baseContainerColor)
            else -> baseContainerColor
        },
        animationSpec = tween(durationMillis = 120),
        label = "containerColor",
    )

    // Animated elevation
    val animatedElevation by animateDpAsState(
        targetValue = when {
            isSelected -> 2.dp
            task.isPinned -> 2.dp
            else -> 1.dp
        },
        animationSpec = tween(durationMillis = 120),
        label = "elevation",
    )

    // Selected border
    val selectedBorder = if (isSelected) {
        BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        )
    } else {
        null
    }

    Card(
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
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = animatedElevation,
        ),
        shape = MaterialTheme.shapes.large,
        border = selectedBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // LEFT: Checkbox (selection mode uses selection state; normal uses completed state)
            val checkboxChecked = if (isSelectionMode) isSelected else task.isCompleted

            // Clickable checkbox area to handle completion without conflicting with card
            Box(
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            if (!isSelectionMode) onToggleComplete()
                        },
                    )
                    .semantics {
                        contentDescription = task.title
                        stateDescription = if (checkboxChecked) checkedDesc else uncheckedDesc
                        role = Role.Checkbox
                    },
                contentAlignment = Alignment.Center,
            ) {
                Checkbox(
                    checked = checkboxChecked,
                    onCheckedChange = null, // Handled by Box click
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }

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
                    val datePattern = if (task.dueAtHasTime) {
                        stringResource(R.string.date_format_due_time)
                    } else {
                        stringResource(R.string.date_format_due_date_only)
                    }
                    val dueDateText = if (overdue) {
                        stringResource(R.string.task_overdue_date, formatDueDate(dueDate, datePattern))
                    } else {
                        stringResource(R.string.task_due_date, formatDueDate(dueDate, datePattern))
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
                        if (RecurrenceType.fromValue(task.recurrenceType) != RecurrenceType.NONE) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = stringResource(R.string.cd_recurrence),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (streakCount >= 2) {
                            StreakBadge(count = streakCount)
                        }
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
                            TaskLabelChip(
                                label = label,
                                type = type,
                                priority = Priority.fromValue(task.priority),
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
                            contentDescription = stringResource(
                                if (task.isPinned) R.string.unpin_task else R.string.pin_task,
                            ),
                            tint = if (task.isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_options_for_task, task.title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            if (RecurrenceType.fromValue(task.recurrenceType) != RecurrenceType.NONE) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_skip_occurrence)) },
                                    onClick = {
                                        showMenu = false
                                        onSkipOccurrence()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            contentDescription = null,
                                        )
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_archive)) },
                                onClick = {
                                    showMenu = false
                                    onArchiveClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Archive,
                                        contentDescription = null,
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_duplicate)) },
                                onClick = {
                                    showMenu = false
                                    onDuplicateClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Non-interactive label chip for tag/priority display.
 * Uses Surface instead of AssistChip to avoid announcing as a button to TalkBack.
 */
@Composable
private fun TaskLabelChip(label: String, type: ChipType, priority: Priority, modifier: Modifier = Modifier) {
    val containerColor = when (type) {
        ChipType.Overdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
        ChipType.Tag -> MaterialTheme.colorScheme.secondaryContainer
        ChipType.Priority -> when (priority) {
            Priority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
            Priority.LOW -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    }

    val labelColor = when (type) {
        ChipType.Overdue -> MaterialTheme.colorScheme.onErrorContainer
        ChipType.Tag -> MaterialTheme.colorScheme.onSecondaryContainer
        ChipType.Priority -> when (priority) {
            Priority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
            Priority.LOW -> MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Surface(
        modifier = modifier.height(24.dp),
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (type == ChipType.Priority) {
                Icon(
                    imageVector = when (priority) {
                        Priority.HIGH -> Icons.Default.KeyboardArrowUp
                        Priority.LOW -> Icons.Default.KeyboardArrowDown
                        else -> Icons.Default.Remove
                    },
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = labelColor,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StreakBadge(count: Int, modifier: Modifier = Modifier) {
    val badgeCd = stringResource(R.string.streak_badge_cd, count)
    Row(
        modifier = modifier
            .semantics { contentDescription = badgeCd },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "\uD83D\uDD25",
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private enum class ChipType {
    Overdue,
    Tag,
    Priority,
}
