package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.model.TagItem
import dev.tuandoan.tasktracker.domain.service.TagNormalizer
import dev.tuandoan.tasktracker.ui.components.TagColor
import dev.tuandoan.tasktracker.ui.components.TagColors
import dev.tuandoan.tasktracker.ui.viewmodel.TagManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(viewModel: TagManagementViewModel, onNavigateBack: () -> Unit) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingTag by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingTag by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_tag_management)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (tags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.tag_management_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tag_management_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                items(tags, key = { it.name }) { tag ->
                    TagListItem(
                        tag = tag,
                        onEditClick = { editingTag = tag.name },
                        onDeleteClick = { deletingTag = tag.name },
                        onColorClick = { color ->
                            viewModel.updateTagColor(tag.name, color?.key)
                        },
                    )
                }
            }
        }
    }

    // Edit bottom sheet
    editingTag?.let { tagName ->
        val tag = tags.find { it.name == tagName }
        if (tag != null) {
            TagEditorBottomSheet(
                tag = tag,
                existingTagNames = tags.map { it.name }.toSet(),
                onDismiss = { editingTag = null },
                onSave = { newName ->
                    viewModel.renameTag(tagName, newName)
                    editingTag = null
                },
                onColorChange = { color ->
                    viewModel.updateTagColor(tagName, color?.key)
                },
            )
        }
    }

    // Delete confirmation dialog
    deletingTag?.let { tagName ->
        val tag = tags.find { it.name == tagName }
        if (tag != null) {
            DeleteTagDialog(
                tag = tag,
                onConfirm = {
                    viewModel.deleteTag(tagName)
                    deletingTag = null
                },
                onDismiss = { deletingTag = null },
            )
        }
    }
}

@Composable
private fun TagListItem(
    tag: TagItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onColorClick: (TagColor?) -> Unit,
) {
    val tagColor = TagColors.fromKey(tag.color)
    val chipContainerColor = tagColor?.container ?: MaterialTheme.colorScheme.secondaryContainer
    val chipLabelColor = tagColor?.onContainer ?: MaterialTheme.colorScheme.onSecondaryContainer

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = chipContainerColor,
                ) {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = chipLabelColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.tag_task_count, tag.taskCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit_tag),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete_tag),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagEditorBottomSheet(
    tag: TagItem,
    existingTagNames: Set<String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onColorChange: (TagColor?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tagName by rememberSaveable { mutableStateOf(tag.name) }
    var tagError by remember { mutableStateOf<String?>(null) }
    val selectedColorKey = tag.color

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.tag_editor_title),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tagName,
                onValueChange = { newValue ->
                    if (newValue.length <= TAG_MAX_LENGTH) {
                        tagName = newValue
                        tagError = null
                    }
                },
                label = { Text(stringResource(R.string.label_tag)) },
                singleLine = true,
                isError = tagError != null,
                supportingText = tagError?.let { { Text(it) } },
                trailingIcon = {
                    if (tagName.isNotEmpty()) {
                        IconButton(onClick = { tagName = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_clear_search),
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.tag_editor_color),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // "No color" option
                ColorCircle(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    isSelected = selectedColorKey == null,
                    label = stringResource(R.string.tag_color_none),
                    onClick = { onColorChange(null) },
                )
                TagColors.palette.forEach { tagColor ->
                    ColorCircle(
                        color = tagColor.primary,
                        isSelected = selectedColorKey == tagColor.key,
                        label = tagColor.key,
                        onClick = { onColorChange(tagColor) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val normalized = TagNormalizer.normalize(tagName)
                        when {
                            normalized == null -> tagError = "Tag name cannot be empty"
                            normalized != tag.name && existingTagNames.contains(normalized) ->
                                tagError = "Tag \"$normalized\" already exists"
                            else -> onSave(normalized)
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun ColorCircle(
    color: androidx.compose.ui.graphics.Color,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val cd = stringResource(R.string.cd_tag_color, label)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color, CircleShape)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DeleteTagDialog(tag: TagItem, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_delete_title)) },
        text = {
            Text(stringResource(R.string.tag_delete_message, tag.name, tag.taskCount))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private const val TAG_MAX_LENGTH = 30
