package dev.tuandoan.tasktracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.model.ParsedTaskTokens
import dev.tuandoan.tasktracker.utils.formatDueDate

/**
 * Animated row of interactive suggestion chips displayed under title input when shorthand tokens are recognized (CAP-11).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShorthandSuggestionRow(
    parsedTokens: ParsedTaskTokens,
    onApplyDueDate: () -> Unit,
    onApplyPriority: () -> Unit,
    onApplyTag: () -> Unit,
    onApplyAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = parsedTokens.hasTokens,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val tokenCount = (if (parsedTokens.dueAt != null) 1 else 0) +
                (if (parsedTokens.priority != null) 1 else 0) +
                (if (parsedTokens.tag != null) 1 else 0)

            // Multi-token bulk apply chip
            if (tokenCount > 1) {
                val applyAllCd = stringResource(R.string.cd_shorthand_apply_all)
                AssistChip(
                    onClick = onApplyAll,
                    label = { Text(stringResource(R.string.shorthand_apply_all)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = applyAllCd
                    },
                )
            }

            // Due date chip
            parsedTokens.dueAt?.let { dueDate ->
                val datePattern = if (parsedTokens.dueAtHasTime) {
                    stringResource(R.string.date_format_due_time)
                } else {
                    stringResource(R.string.date_format_due_date_only)
                }
                val formattedDate = formatDueDate(dueDate, datePattern)
                val applyDateCd = stringResource(R.string.cd_shorthand_apply_date, formattedDate)

                AssistChip(
                    onClick = onApplyDueDate,
                    label = { Text(stringResource(R.string.shorthand_date_pill, formattedDate)) },
                    leadingIcon = {
                        val icon = if (parsedTokens.dueAtHasTime) Icons.Default.Schedule else Icons.Default.Event
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = applyDateCd
                    },
                )
            }

            // Priority chip
            parsedTokens.priority?.let { prio ->
                val prioLabel = when (prio) {
                    2 -> stringResource(R.string.priority_high)
                    1 -> stringResource(R.string.priority_medium)
                    else -> stringResource(R.string.priority_low)
                }
                val applyPrioCd = stringResource(R.string.cd_shorthand_apply_priority, prioLabel)

                AssistChip(
                    onClick = onApplyPriority,
                    label = { Text(stringResource(R.string.shorthand_priority_pill, prioLabel)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = applyPrioCd
                    },
                )
            }

            // Tag chip
            parsedTokens.tag?.let { tagName ->
                val applyTagCd = stringResource(R.string.cd_shorthand_apply_tag, tagName)

                AssistChip(
                    onClick = onApplyTag,
                    label = { Text(stringResource(R.string.shorthand_tag_pill, tagName)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = applyTagCd
                    },
                )
            }
        }
    }
}
