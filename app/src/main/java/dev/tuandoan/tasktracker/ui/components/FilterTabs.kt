package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.ui.theme.AppSpacing

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
                    CustomFilterChip(
                        onClick = { onTagFilterChange(null) },
                        text = stringResource(R.string.filter_clear),
                        isSelected = false,
                        showIcon = true,
                    )
                }
            }

            // Tag filter chips with HTML reference design
            items(availableTags) { tag ->
                CustomFilterChip(
                    onClick = {
                        if (currentTagFilter == tag) {
                            onTagFilterChange(null) // Deselect if already selected
                        } else {
                            onTagFilterChange(tag)
                        }
                    },
                    text = tag,
                    isSelected = currentTagFilter == tag,
                )
            }
        }
    }
}

/**
 * Custom filter chip that matches the HTML reference design
 * - Selected: primary background, white text, subtle shadow
 * - Unselected: surface background, outline border, muted text
 * - Height: 36dp, rounded corners: 12dp
 */
@Composable
private fun CustomFilterChip(
    onClick: () -> Unit,
    text: String,
    isSelected: Boolean,
    showIcon: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    }

    val borderStroke = if (!isSelected) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    } else {
        null
    }

    val shadowElevation = if (isSelected) 2.dp else 0.dp

    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp), // h-9 from HTML
        shape = RoundedCornerShape(12.dp), // rounded-xl from HTML
        color = containerColor,
        border = borderStroke,
        shadowElevation = shadowElevation,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), // px-5 from HTML
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showIcon) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.cd_clear_filter),
                    modifier = Modifier.size(16.dp),
                    tint = contentColor,
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.sp, // text-sm from HTML
                ),
                color = contentColor,
            )
        }
    }
}
