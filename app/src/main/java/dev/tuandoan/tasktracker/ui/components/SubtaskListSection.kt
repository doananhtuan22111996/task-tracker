package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
 * Approximate height of a single subtask row in dp. Used by the drag gesture to decide when a
 * sustained vertical drag should swap with the neighbor above/below. A fixed constant is cheaper
 * and more predictable than measuring each row; rows have consistent layout.
 */
private const val ROW_HEIGHT_DP = 56

/**
 * Checklist section inside the task editor. Renders the current [subtasks] as rows (checkbox +
 * inline text field + drag handle + delete icon) and an always-present "Add subtask" row at the
 * bottom. Drag handle supports long-press + vertical drag to reorder rows via [onMoveSubtask].
 */
@Composable
fun SubtaskListSection(
    subtasks: List<SubtaskDraft>,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (Long) -> Unit,
    onUpdateTitle: (Long, String) -> Unit,
    onRemoveSubtask: (Long) -> Unit,
    onMoveSubtaskBy: (draftId: Long, direction: Int) -> Boolean,
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
                onMoveBy = { direction -> onMoveSubtaskBy(draft.id, direction) },
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
    onMoveBy: (direction: Int) -> Boolean,
) {
    val checkboxDescriptionResId = if (draft.isCompleted) {
        R.string.cd_subtask_checkbox_checked
    } else {
        R.string.cd_subtask_checkbox_unchecked
    }
    val fallback = stringResource(R.string.label_add_subtask)
    val checkboxDescription = stringResource(
        checkboxDescriptionResId,
        draft.title.ifBlank { fallback },
    )
    val removeDescription = stringResource(R.string.cd_remove_subtask)
    val dragHandleDescription = stringResource(R.string.cd_drag_handle_subtask)

    val density = LocalDensity.current
    val rowHeightPx = remember(density) { with(density) { ROW_HEIGHT_DP.dp.toPx() } }

    // Accumulates vertical drag offset across a single gesture. When it passes ±rowHeightPx the
    // row swaps with its neighbor in that direction, and the accumulator is reset. Keyed only by
    // draft.id — the gesture continues smoothly across swaps because onMoveBy uses the draft id
    // and the ViewModel re-resolves the current index on each call (no stale-index bug).
    var dragAccumulator by remember(draft.id) { mutableFloatStateOf(0f) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = dragHandleDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(48.dp)
                .padding(12.dp)
                .pointerInput(draft.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragAccumulator = 0f },
                        onDragEnd = { dragAccumulator = 0f },
                        onDragCancel = { dragAccumulator = 0f },
                        onDrag = { _, dragAmount ->
                            dragAccumulator += dragAmount.y
                            while (dragAccumulator > rowHeightPx) {
                                if (onMoveBy(1)) {
                                    dragAccumulator -= rowHeightPx
                                } else {
                                    // Hit bottom of list; clamp so the next reverse drag
                                    // responds immediately instead of having to burn off banked
                                    // overshoot first.
                                    dragAccumulator = rowHeightPx
                                    break
                                }
                            }
                            while (dragAccumulator < -rowHeightPx) {
                                if (onMoveBy(-1)) {
                                    dragAccumulator += rowHeightPx
                                } else {
                                    dragAccumulator = -rowHeightPx
                                    break
                                }
                            }
                        },
                    )
                },
        )
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
    // rememberSaveable so an in-progress "Add subtask" draft survives rotation.
    var text by rememberSaveable { mutableStateOf("") }
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
