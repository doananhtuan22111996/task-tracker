package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.utils.formatDate
import dev.tuandoan.tasktracker.utils.formatDueDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchivedTaskItem(
    task: Task,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onRestoreClick: () -> Unit,
    onPermanentDeleteClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onToggleSelection: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    }
                    // In archived screen, we don't have edit functionality, so no click action in normal mode
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        onLongPress()
                    }
                },
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) // Always show archived appearance
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                // Selection indicator in selection mode
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            } else {
                // Archive indicator in normal mode - just spacing
                Spacer(modifier = Modifier.width(40.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(0.8f), // Slightly faded to indicate archived status
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (task.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Tag Display
                if (!task.tag.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = task.tag,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        border = null,
                        modifier = Modifier.alpha(0.8f),
                    )
                }

                // Due Date Display (if it existed)
                if (task.dueAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Due: ${formatDueDate(task.dueAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }

                // Archive info
                Spacer(modifier = Modifier.height(4.dp))
                val archiveDate = task.archivedAt ?: task.createdAt
                Text(
                    text = "Archived: ${formatDate(archiveDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )

                // Original creation date
                Text(
                    text = "Created: ${formatDate(task.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }

            // Action buttons only in normal mode
            if (!isSelectionMode) {
                Row {
                    IconButton(onClick = onRestoreClick) {
                        Icon(
                            imageVector = Icons.Default.Unarchive,
                            contentDescription = "Restore task",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        )
                    }

                    IconButton(onClick = onPermanentDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Permanently delete task",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}
