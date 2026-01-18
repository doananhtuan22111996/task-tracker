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

/**
 * Tag filter chips section for filtering tasks by tags
 */
@Composable
fun TagFilterChips(
    currentTagFilter: String?,
    availableTags: List<String>,
    onTagFilterChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Tag Filter Section with improved spacing and styling
    if (availableTags.isNotEmpty()) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
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
