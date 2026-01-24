package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val border: BorderStroke? = if (isSelected) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    } else {
        null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
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
        border = border,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
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
                            MetaRow(
                                icon = Icons.Default.Event,
                                text = "Due: ${formatDueDate(task.dueAt)}",
                            )
                        }

                        val archiveDate = task.archivedAt ?: task.createdAt
                        MetaRow(
                            icon = Icons.Default.Archive,
                            text = "Archived: ${formatDate(archiveDate)}",
                        )

                        MetaRow(
                            icon = Icons.Default.AddCircle,
                            text = "Created: ${formatDate(task.createdAt)}",
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
                        ) {
                            Icon(
                                imageVector = Icons.Default.Unarchive,
                                contentDescription = "Restore task",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        IconButton(
                            onClick = onPermanentDeleteClick,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Permanently delete task",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
