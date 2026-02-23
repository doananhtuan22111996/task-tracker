package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.model.CompletedGrouping
import dev.tuandoan.tasktracker.domain.model.SortDirection
import dev.tuandoan.tasktracker.domain.model.SortKey
import dev.tuandoan.tasktracker.domain.model.TaskSort

/**
 * Sort menu dropdown content with radio group for sort options and toggle for completed grouping
 */
@Composable
fun SortMenu(currentSort: TaskSort, onSortSelected: (TaskSort) -> Unit) {
    // Sort section header
    Text(
        text = stringResource(R.string.sort_header),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )

    // Define the 4 sort options (radio group)
    val sortOptions = listOf(
        Triple(SortKey.CREATED_AT, SortDirection.DESC, stringResource(R.string.sort_created_newest)),
        Triple(SortKey.CREATED_AT, SortDirection.ASC, stringResource(R.string.sort_created_oldest)),
        Triple(SortKey.TITLE, SortDirection.ASC, stringResource(R.string.sort_title_az)),
        Triple(SortKey.PRIORITY, SortDirection.DESC, stringResource(R.string.sort_priority_high_low)),
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
                        direction = direction,
                    ),
                )
            },
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
        },
    )
}

/**
 * Individual sort radio button menu item
 */
@Composable
private fun SortRadioItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.cd_selected),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        onClick = onClick,
    )
}

/**
 * Completed last toggle menu item with switch
 */
@Composable
private fun CompletedLastToggleItem(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sort_completed_last),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = null, // Handled by the dropdown item click
                )
            }
        },
        onClick = { onToggle(!isEnabled) },
    )
}
