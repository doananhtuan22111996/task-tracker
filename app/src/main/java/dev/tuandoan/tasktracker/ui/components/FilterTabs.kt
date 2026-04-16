package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R

@Composable
fun TagChipRow(
    currentTagFilter: String?,
    availableTags: List<String>,
    onTagFilterChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    tagColorMap: Map<String, String?> = emptyMap(),
) {
    if (availableTags.isEmpty()) return

    val selectedStateDesc = stringResource(R.string.a11y_chip_selected)
    val unselectedStateDesc = stringResource(R.string.a11y_chip_not_selected)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(availableTags) { tag ->
            val isSelected = currentTagFilter == tag
            val tagColor = TagColors.fromKey(tagColorMap[tag])
            FilterChip(
                selected = isSelected,
                onClick = {
                    onTagFilterChange(if (isSelected) null else tag)
                },
                label = {
                    Text(
                        text = tag,
                        color = if (tagColor != null) {
                            tagColor.onContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                colors = if (tagColor != null) {
                    FilterChipDefaults.filterChipColors(
                        containerColor = tagColor.container.copy(alpha = 0.6f),
                        selectedContainerColor = tagColor.container,
                        selectedLabelColor = tagColor.onContainer,
                    )
                } else {
                    FilterChipDefaults.filterChipColors()
                },
                modifier = Modifier.semantics {
                    stateDescription = if (isSelected) selectedStateDesc else unselectedStateDesc
                },
            )
        }
    }
}
