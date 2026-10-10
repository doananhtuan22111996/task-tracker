package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.service.TaskShorthandParser
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.utils.formatDueDate
import kotlinx.coroutines.delay

/**
 * Lightweight bottom sheet for lightning-fast task capture with smart shorthand support (CAP-13).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    onDismiss: () -> Unit,
    onSaveTask: (title: String, dueAt: Long?, dueAtHasTime: Boolean, priority: Int, tag: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    var appliedDueAt by remember { mutableStateOf<Long?>(null) }
    var appliedDueAtHasTime by remember { mutableStateOf(false) }
    var appliedPriority by remember { mutableStateOf<Int?>(null) }
    var appliedTag by remember { mutableStateOf<String?>(null) }

    val parsedTokens = remember(input) { TaskShorthandParser.parse(input) }

    fun commitTask() {
        val rawTrimmed = input.trim()
        val cleanTitle = parsedTokens.cleanTitle.trim()
        val finalTitle = if (cleanTitle.isNotBlank()) cleanTitle else rawTrimmed
        if (finalTitle.isBlank()) return

        val finalDueAt = appliedDueAt ?: parsedTokens.dueAt
        val finalDueAtHasTime = if (appliedDueAt != null) appliedDueAtHasTime else parsedTokens.dueAtHasTime
        val finalPriority = appliedPriority ?: parsedTokens.priority ?: 1
        val finalTag = appliedTag ?: parsedTokens.tag

        onSaveTask(finalTitle, finalDueAt, finalDueAtHasTime, finalPriority, finalTag)
        onDismiss()
    }

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.quick_add_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                    )
                }
            }

            // Input field
            val canSave = input.trim().isNotBlank() || parsedTokens.cleanTitle.trim().isNotBlank()
            val saveCd = stringResource(R.string.quick_add_save)

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = stringResource(R.string.quick_add_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                singleLine = true,
                maxLines = 1,
                trailingIcon = {
                    IconButton(
                        onClick = { commitTask() },
                        enabled = canSave,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = saveCd,
                            tint = if (canSave) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { commitTask() },
                ),
            )

            // Applied attributes row (if user clicked chips)
            val hasAppliedAttrs = appliedDueAt != null || appliedPriority != null || appliedTag != null
            if (hasAppliedAttrs) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppSpacing.small),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    appliedDueAt?.let { dueDate ->
                        val datePattern = if (appliedDueAtHasTime) {
                            stringResource(R.string.date_format_due_time)
                        } else {
                            stringResource(R.string.date_format_due_date_only)
                        }
                        val formattedDate = formatDueDate(dueDate, datePattern)
                        FilterChip(
                            selected = true,
                            onClick = {
                                appliedDueAt = null
                                appliedDueAtHasTime = false
                            },
                            label = { Text(formattedDate) },
                            leadingIcon = {
                                val icon = if (appliedDueAtHasTime) {
                                    Icons.Default.Schedule
                                } else {
                                    Icons.Default.Event
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }

                    appliedPriority?.let { prio ->
                        val prioLabel = when (prio) {
                            2 -> stringResource(R.string.priority_high)
                            1 -> stringResource(R.string.priority_medium)
                            else -> stringResource(R.string.priority_low)
                        }
                        FilterChip(
                            selected = true,
                            onClick = { appliedPriority = null },
                            label = { Text(prioLabel) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }

                    appliedTag?.let { tag ->
                        FilterChip(
                            selected = true,
                            onClick = { appliedTag = null },
                            label = { Text("#$tag") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Label,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                        )
                    }
                }
            }

            // Live shorthand suggestion pills
            ShorthandSuggestionRow(
                parsedTokens = parsedTokens,
                onApplyDueDate = {
                    appliedDueAt = parsedTokens.dueAt
                    appliedDueAtHasTime = parsedTokens.dueAtHasTime
                    input = TaskShorthandParser.stripDueDateToken(input)
                },
                onApplyPriority = {
                    appliedPriority = parsedTokens.priority
                    input = TaskShorthandParser.stripPriorityToken(input)
                },
                onApplyTag = {
                    appliedTag = parsedTokens.tag
                    input = TaskShorthandParser.stripTagToken(input)
                },
                onApplyAll = {
                    appliedDueAt = parsedTokens.dueAt
                    appliedDueAtHasTime = parsedTokens.dueAtHasTime
                    appliedPriority = parsedTokens.priority
                    appliedTag = parsedTokens.tag
                    input = parsedTokens.cleanTitle
                },
                modifier = Modifier.padding(top = AppSpacing.small),
            )

            Spacer(modifier = Modifier.height(AppSpacing.large))
        }
    }
}
