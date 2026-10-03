package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Subtask
import dev.tuandoan.tasktracker.domain.usecase.SubtaskUseCase

/**
 * Renders an inline subtask checklist for a task card.
 *
 * Each row provides a checkbox to toggle completion and strikethrough styling when done.
 * At the bottom, a compact single-line input field allows appending new subtasks immediately.
 */
@Composable
fun InlineSubtaskList(
    subtasks: List<Subtask>,
    onToggleSubtask: (subtaskId: Long, completed: Boolean) -> Unit,
    onAddSubtask: (title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        subtasks.forEach { subtask ->
            InlineSubtaskRow(
                subtask = subtask,
                onToggle = { onToggleSubtask(subtask.id, !subtask.isCompleted) },
            )
        }

        InlineAddSubtaskRow(
            onAddSubtask = onAddSubtask,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun InlineSubtaskRow(subtask: Subtask, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val checkedDesc = stringResource(R.string.a11y_checkbox_checked)
    val uncheckedDesc = stringResource(R.string.a11y_checkbox_unchecked)
    val contentDesc = stringResource(
        if (subtask.isCompleted) R.string.cd_subtask_checkbox_checked else R.string.cd_subtask_checkbox_unchecked,
        subtask.title,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = null, // Handled by row click
            modifier = Modifier
                .size(24.dp)
                .semantics {
                    this.contentDescription = contentDesc
                    this.stateDescription = if (subtask.isCompleted) checkedDesc else uncheckedDesc
                    this.role = Role.Checkbox
                },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )

        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtask.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InlineAddSubtaskRow(onAddSubtask: (title: String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }

    val submit = {
        val sanitized = sanitizeSubtaskTitle(text)
        if (sanitized != null) {
            onAddSubtask(sanitized)
            text = ""
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            if (newText.length <= SubtaskUseCase.MAX_TITLE_LENGTH) {
                text = newText
            }
        },
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = stringResource(R.string.subtask_add_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            IconButton(
                onClick = submit,
                enabled = text.isNotBlank(),
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_subtask),
                    tint = if (text.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        shape = MaterialTheme.shapes.small,
    )
}

/**
 * Sanitizes input for subtask creation:
 * - Strips leading/trailing whitespace.
 * - Clamps length to [SubtaskUseCase.MAX_TITLE_LENGTH].
 * - Returns null if the resulting string is empty or blank, preventing blank subtasks.
 */
internal fun sanitizeSubtaskTitle(title: String): String? {
    val trimmed = title.trim()
    return if (trimmed.isNotEmpty()) trimmed.take(SubtaskUseCase.MAX_TITLE_LENGTH) else null
}
