package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter

@Composable
fun FilterTabs(
    currentFilter: TaskFilter,
    onFilterChange: (TaskFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        TaskFilter.ALL to "All",
        TaskFilter.ACTIVE to "Active",
        TaskFilter.COMPLETED to "Completed"
    )

    PrimaryTabRow(
        selectedTabIndex = filters.indexOfFirst { it.first == currentFilter },
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        filters.forEachIndexed { index, (filter, title) ->
            Tab(
                selected = currentFilter == filter,
                onClick = { onFilterChange(filter) },
                text = { Text(title) }
            )
        }
    }
}