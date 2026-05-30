package dev.tuandoan.tasktracker.widget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.model.TagItem
import dev.tuandoan.tasktracker.widget.model.WidgetSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigureScreen(
    uiState: WidgetConfigureUiState,
    tags: List<TagItem>,
    onSelectionChange: (WidgetSource) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_configure_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                val isTagSourceWithNoSelection =
                    uiState.selection is WidgetSource.Tag && uiState.selection.name.isBlank()
                Button(
                    onClick = onConfirm,
                    enabled = !uiState.isSaving && !isTagSourceWithNoSelection,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.widget_configure_confirm))
                }
            }
        },
    ) { paddingValues ->
        SourcePickerContent(
            paddingValues = paddingValues,
            selection = uiState.selection,
            tags = tags,
            onSelectionChange = onSelectionChange,
        )
    }
}

@Composable
private fun SourcePickerContent(
    paddingValues: PaddingValues,
    selection: WidgetSource,
    tags: List<TagItem>,
    onSelectionChange: (WidgetSource) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        item {
            SourceOptionRow(
                label = stringResource(R.string.widget_source_today),
                selected = selection is WidgetSource.Today,
                onClick = { onSelectionChange(WidgetSource.Today) },
            )
        }
        item {
            SourceOptionRow(
                label = stringResource(R.string.widget_source_upcoming),
                selected = selection is WidgetSource.Upcoming7d,
                onClick = { onSelectionChange(WidgetSource.Upcoming7d) },
            )
        }
        item {
            SourceOptionRow(
                label = stringResource(R.string.widget_source_pinned),
                selected = selection is WidgetSource.Pinned,
                onClick = { onSelectionChange(WidgetSource.Pinned) },
            )
        }
        item {
            SourceOptionRow(
                label = stringResource(R.string.widget_source_tag),
                selected = selection is WidgetSource.Tag,
                onClick = {
                    // Pre-select first tag if available; otherwise blank (confirm disabled).
                    val firstTag = tags.firstOrNull()
                    onSelectionChange(if (firstTag != null) WidgetSource.Tag(firstTag.name) else WidgetSource.Tag(""))
                },
            )
        }

        if (selection is WidgetSource.Tag) {
            if (tags.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.widget_configure_no_tags),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.widget_configure_tag_picker_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(tags, key = { it.name }) { tag ->
                    TagPickerRow(
                        tag = tag,
                        selected = selection.name == tag.name,
                        onClick = { onSelectionChange(WidgetSource.Tag(tag.name)) },
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun SourceOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val cd = stringResource(R.string.cd_widget_source_option, label)
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            RadioButton(selected = selected, onClick = null)
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = cd
                role = Role.RadioButton
            }
            .clickable(onClick = onClick),
    )
}

@Composable
private fun TagPickerRow(tag: TagItem, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.RadioButton }
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
