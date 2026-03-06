package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.model.DueDatePreset
import dev.tuandoan.tasktracker.domain.model.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onSave: (title: String, priority: Priority, dueAt: Long?) -> Unit,
    onExpandToEditor: (title: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var dueAt by remember { mutableStateOf<Long?>(null) }
    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding(),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.hint_quick_add)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (title.isNotBlank()) onSave(title.trim(), priority, dueAt)
                    },
                ),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    val label = when (p) {
                        Priority.LOW -> stringResource(R.string.priority_low)
                        Priority.MEDIUM -> stringResource(R.string.priority_medium)
                        Priority.HIGH -> stringResource(R.string.priority_high)
                    }
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DueDatePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = dueAt == preset.toEpochMillis(),
                        onClick = {
                            dueAt = if (dueAt == preset.toEpochMillis()) null else preset.toEpochMillis()
                        },
                        label = { Text(preset.label) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onExpandToEditor(title) }) {
                    Text(stringResource(R.string.action_add_details))
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                    )
                }
                Button(
                    onClick = { onSave(title.trim(), priority, dueAt) },
                    enabled = title.isNotBlank(),
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
