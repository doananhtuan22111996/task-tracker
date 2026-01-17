package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.ui.theme.CustomShapes
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter

/**
 * Enhanced filter tabs with improved Material 3 styling and consistent spacing
 */
@Composable
fun FilterTabs(
    currentFilter: TaskFilter,
    onFilterChange: (TaskFilter) -> Unit,
    currentTagFilter: String?,
    availableTags: List<String>,
    onTagFilterChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Status Filter Tabs with improved styling
        val filters = listOf(
            TaskFilter.ALL to "All",
            TaskFilter.ACTIVE to "Active",
            TaskFilter.COMPLETED to "Completed",
        )

        PrimaryTabRow(
            selectedTabIndex = filters.indexOfFirst { it.first == currentFilter },
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp,
                )
            },
        ) {
            filters.forEachIndexed { index, (filter, title) ->
                Tab(
                    selected = currentFilter == filter,
                    onClick = { onFilterChange(filter) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (currentFilter == filter) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Tag Filter Section with improved spacing and styling
        if (availableTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppSpacing.medium))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.chipSpacing),
                contentPadding = PaddingValues(horizontal = AppSpacing.screenPadding),
            ) {
                // Clear tag filter button (only show if a tag is selected)
                if (currentTagFilter != null) {
                    item {
                        FilterChip(
                            onClick = { onTagFilterChange(null) },
                            label = {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            selected = false,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear tag filter",
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            shape = CustomShapes.chip,
                        )
                    }
                }

                // Tag filter chips with enhanced styling
                items(availableTags) { tag ->
                    FilterChip(
                        onClick = {
                            if (currentTagFilter == tag) {
                                onTagFilterChange(null) // Deselect if already selected
                            } else {
                                onTagFilterChange(tag)
                            }
                        },
                        label = {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        selected = currentTagFilter == tag,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        shape = CustomShapes.chip,
                        elevation = FilterChipDefaults.filterChipElevation(
                            elevation = if (currentTagFilter == tag) 2.dp else 0.dp,
                        ),
                    )
                }
            }
        }
    }
}
