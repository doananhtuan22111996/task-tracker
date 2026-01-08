package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.domain.model.CompletedGrouping
import dev.tuandoan.tasktracker.domain.model.SortDirection
import dev.tuandoan.tasktracker.domain.model.SortKey
import dev.tuandoan.tasktracker.domain.model.TaskSort

/**
 * Sort menu dropdown content with radio group for sort options and toggle for completed grouping
 */
@Composable
fun SortMenu(
    currentSort: TaskSort,
    onSortSelected: (TaskSort) -> Unit
) {
    // Sort section header
    Text(
        text = "Sort",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )

    // Define the 3 sort options (radio group)
    val sortOptions = listOf(
        Triple(SortKey.CREATED_AT, SortDirection.DESC, "Created: Newest first"),
        Triple(SortKey.CREATED_AT, SortDirection.ASC, "Created: Oldest first"),
        Triple(SortKey.TITLE, SortDirection.ASC, "Title: A–Z")
    )

    // Radio group for sort options
    sortOptions.forEach { (key, direction, label) ->
        SortRadioItem(
            label = label,
            isSelected = currentSort.key == key && currentSort.direction == direction,
            onClick = {
                onSortSelected(
                    currentSort.copy(
                        key = key,
                        direction = direction
                    )
                )
            }
        )
    }

    // Divider
    HorizontalDivider()

    // Completed last toggle
    CompletedLastToggleItem(
        isEnabled = currentSort.completedGrouping == CompletedGrouping.COMPLETED_LAST,
        onToggle = { enabled ->
            val newGrouping = if (enabled) CompletedGrouping.COMPLETED_LAST else CompletedGrouping.NONE
            onSortSelected(currentSort.copy(completedGrouping = newGrouping))
        }
    )
}

/**
 * Individual sort radio button menu item
 */
@Composable
private fun SortRadioItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        onClick = onClick
    )
}

/**
 * Completed last toggle menu item with switch
 */
@Composable
private fun CompletedLastToggleItem(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completed last",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = null // Handled by the dropdown item click
                )
            }
        },
        onClick = { onToggle(!isEnabled) }
    )
}