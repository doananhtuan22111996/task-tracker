package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter

@Composable
fun FilterTabs(
    currentFilter: TaskFilter,
    onFilterChange: (TaskFilter) -> Unit,
    currentTagFilter: String?,
    availableTags: List<String>,
    onTagFilterChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Status Filter Tabs
        val filters = listOf(
            TaskFilter.ALL to "All",
            TaskFilter.ACTIVE to "Active",
            TaskFilter.COMPLETED to "Completed"
        )

        PrimaryTabRow(
            selectedTabIndex = filters.indexOfFirst { it.first == currentFilter },
            modifier = Modifier.fillMaxWidth(),
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

        // Tag Filter Section
        if (availableTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                // Clear tag filter button (only show if a tag is selected)
                if (currentTagFilter != null) {
                    item {
                        FilterChip(
                            onClick = { onTagFilterChange(null) },
                            label = { Text("Clear") },
                            selected = false,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear tag filter",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Tag filter chips
                items(availableTags) { tag ->
                    FilterChip(
                        onClick = {
                            if (currentTagFilter == tag) {
                                onTagFilterChange(null) // Deselect if already selected
                            } else {
                                onTagFilterChange(tag)
                            }
                        },
                        label = { Text(tag) },
                        selected = currentTagFilter == tag,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}