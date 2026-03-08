package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter

@Composable
fun FilterChipRow(
    currentFilter: TaskFilter = TaskFilter.ALL,
    currentTagFilter: String?,
    availableTags: List<String>,
    onFilterChange: (TaskFilter) -> Unit = {},
    onTagFilterChange: (String?) -> Unit,
    showStatusChips: Boolean = true,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (showStatusChips) {
            item {
                FilterChip(
                    selected = currentFilter == TaskFilter.ALL,
                    onClick = { onFilterChange(TaskFilter.ALL) },
                    label = { Text("All") },
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == TaskFilter.ACTIVE,
                    onClick = { onFilterChange(TaskFilter.ACTIVE) },
                    label = { Text("Active") },
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == TaskFilter.COMPLETED,
                    onClick = { onFilterChange(TaskFilter.COMPLETED) },
                    label = { Text("Done") },
                )
            }
        }
        if (availableTags.isNotEmpty() && showStatusChips) {
            item {
                HorizontalDivider(
                    modifier = Modifier
                        .height(28.dp)
                        .padding(horizontal = 4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        items(availableTags) { tag ->
            FilterChip(
                selected = currentTagFilter == tag,
                onClick = {
                    onTagFilterChange(if (currentTagFilter == tag) null else tag)
                },
                label = { Text(tag) },
            )
        }
    }
}
