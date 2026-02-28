package dev.tuandoan.tasktracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.utils.formatDate
import dev.tuandoan.tasktracker.utils.formatDueDate

@Composable
fun ArchivedTaskItem(
    modifier: Modifier = Modifier,
    task: Task,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onRestoreClick: () -> Unit,
    onPermanentDeleteClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onToggleSelection: () -> Unit = {},
) {
    // Unified interaction source for consistent ripple
    val interactionSource = remember { MutableInteractionSource() }

    // Animated container color based on state
    val baseContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                interactionSource = interactionSource,
                indication = null, // Use Material3 default indication
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    }
                    // Archived screen: no navigation in normal mode
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        onLongPress()
                    }
                },
            ),
        shape = RoundedCornerShape(20.dp),
        border = selectedBorder,
        colors = CardDefaults.cardColors(containerColor = animatedContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (isSelectionMode) {
                // Clickable checkbox area to handle selection without conflicting with card
                Box(
                    modifier = Modifier
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // No ripple for checkbox area
                            onClick = onToggleSelection,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null, // Handled by Box click
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        interactionSource = remember { MutableInteractionSource() }, // Non-rippling
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    // Title
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    // Description
                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tag chip (left)
                    if (!task.tag.isNullOrEmpty()) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = task.tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            border = null,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Metadata rows (Due / Archived / Created)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (task.dueAt != null) {
                            val dueTimePattern = stringResource(R.string.date_format_due_time)
                            MetaRow(
                                icon = Icons.Default.Event,
                                text = stringResource(
                                    R.string.task_due_date,
                                    formatDueDate(task.dueAt, dueTimePattern),
                                ),
                            )
                        }

                        val shortDatePattern = stringResource(R.string.date_format_short)
                        val archiveDate = task.archivedAt ?: task.createdAt
                        MetaRow(
                            icon = Icons.Default.Archive,
                            text = stringResource(
                                R.string.task_archived_date,
                                formatDate(archiveDate, shortDatePattern),
                            ),
                        )

                        MetaRow(
                            icon = Icons.Default.AddCircle,
                            text = stringResource(
                                R.string.task_created_date,
                                formatDate(task.createdAt, shortDatePattern),
                            ),
                        )
                    }
                }

                if (!isSelectionMode) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        IconButton(
                            onClick = onRestoreClick,
                            modifier = Modifier.size(40.dp),
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Unarchive,
                                contentDescription = stringResource(R.string.cd_restore_task),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        IconButton(
                            onClick = onPermanentDeleteClick,
                            modifier = Modifier.size(40.dp),
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = stringResource(R.string.cd_permanently_delete_task),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), // Use error color for delete
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
