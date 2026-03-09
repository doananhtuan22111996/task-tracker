package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Always-visible horizontally-scrollable row of tag filter chips.
 * Hidden entirely when [availableTags] is empty.
 */
@Composable
fun TagChipRow(
    currentTagFilter: String?,
    availableTags: List<String>,
    onTagFilterChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availableTags.isEmpty()) return

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
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
