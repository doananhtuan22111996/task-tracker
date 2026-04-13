package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter

@Composable
fun TaskFilterChipRow(currentFilter: TaskFilter, onFilterChange: (TaskFilter) -> Unit, modifier: Modifier = Modifier) {
    val filters = listOf(
        TaskFilter.ALL to stringResource(R.string.nav_all),
        TaskFilter.ACTIVE to stringResource(R.string.nav_active),
        TaskFilter.COMPLETED to stringResource(R.string.nav_completed),
    )

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (filter, label) ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}
