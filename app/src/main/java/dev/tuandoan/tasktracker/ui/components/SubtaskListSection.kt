package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.ui.model.SubtaskDraft

private const val SOFT_CAP = 50

/**
 * Checklist section inside the task editor. Renders the current [subtasks] as rows (checkbox +
 * inline text field + delete icon) and an always-present "Add subtask" row at the bottom.
 *
 * Sort order is maintained by the caller; this component never mutates the passed list.
 */
@Composable
fun SubtaskListSection(
    subtasks: List<SubtaskDraft>,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (Long) -> Unit,
    onUpdateTitle: (Long, String) -> Unit,
    onRemoveSubtask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.section_subtasks),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        subtasks.forEach { draft ->
            SubtaskRow(
                draft = draft,
                onToggle = { onToggleSubtask(draft.id) },
                onUpdateTitle = { onUpdateTitle(draft.id, it) },
                onRemove = { onRemoveSubtask(draft.id) },
            )
        }

        AddSubtaskRow(onAdd = onAddSubtask)

        if (subtasks.size >= SOFT_CAP) {
            Text(
                text = stringResource(R.string.hint_subtask_soft_cap),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubtaskRow(
    draft: SubtaskDraft,
    onToggle: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val checkboxDescriptionResId = if (draft.isCompleted) {
        R.string.cd_subtask_checkbox_checked
    } else {
        R.string.cd_subtask_checkbox_unchecked
    }
    val checkboxDescription = stringResource(checkboxDescriptionResId, draft.title.ifBlank { " " })
    val removeDescription = stringResource(R.string.cd_remove_subtask)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(
            checked = draft.isCompleted,
            onCheckedChange = { onToggle() },
            modifier = Modifier.semantics { contentDescription = checkboxDescription },
        )
        OutlinedTextField(
            value = draft.title,
            onValueChange = onUpdateTitle,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = if (draft.isCompleted) {
                MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
            } else {
                MaterialTheme.typography.bodyLarge
            },
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = removeDescription,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AddSubtaskRow(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val label = stringResource(R.string.label_add_subtask)

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                val trimmed = text.trim()
                if (trimmed.isNotEmpty()) {
                    onAdd(trimmed)
                    text = ""
                }
            },
        ),
    )
}
