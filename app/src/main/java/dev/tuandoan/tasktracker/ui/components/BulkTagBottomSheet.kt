package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LabelOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.ui.theme.AppSpacing

/**
 * Bottom sheet that lets the user pick an existing tag to apply to the current selection, or
 * clear the tag entirely. The list is sourced from [availableTags] (distinct task tags) with
 * colors from [tagColorMap]. Out of scope: inline "create new tag" — the project creates tags
 * via the task editor first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkTagBottomSheet(
    selectedCount: Int,
    availableTags: List<String>,
    tagColorMap: Map<String, String?>,
    onTagSelected: (tag: String?, tagColor: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.title_apply_tag_for_count, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = AppSpacing.medium),
            )

            ClearTagRow(onClick = { onTagSelected(null, null) })

            if (availableTags.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppSpacing.small),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                availableTags.forEach { tagName ->
                    TagRow(
                        name = tagName,
                        colorKey = tagColorMap[tagName],
                        onClick = { onTagSelected(tagName, tagColorMap[tagName]) },
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.label_no_tags_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = AppSpacing.medium),
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.medium))
        }
    }
}

@Composable
private fun ClearTagRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = AppSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        Icon(
            imageVector = Icons.Filled.LabelOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.action_clear_tag),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun TagRow(name: String, colorKey: String?, onClick: () -> Unit) {
    val tagColor = TagColors.fromKey(colorKey)
    val chipContainerColor = tagColor?.container ?: MaterialTheme.colorScheme.secondaryContainer
    val chipLabelColor = tagColor?.onContainer ?: MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = AppSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = chipContainerColor,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = chipLabelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}
