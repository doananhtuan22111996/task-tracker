package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.SubtaskProgress
import dev.tuandoan.tasktracker.domain.model.AgendaItem
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Standalone day agenda content composable (CAL-25.2).
 * Renders the day agenda header (or multi-select action bar), list of tasks/projections,
 * empty state, and floating action button. Reusable across [DayAgendaSheet] (modal)
 * and tablet two-pane split layout.
 */
@Composable
fun DayAgendaContent(
    selectedDay: LocalDate,
    items: List<AgendaItem>,
    subtaskProgress: Map<Long, SubtaskProgress>,
    onItemClick: (AgendaItem) -> Unit,
    onToggleComplete: (AgendaItem) -> Unit,
    onArchive: (AgendaItem) -> Unit,
    onTogglePin: (AgendaItem) -> Unit,
    onAddTaskClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTwoPane: Boolean = false,
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    selectedCount: Int = 0,
    onLongPressTask: (Long) -> Unit = {},
    onToggleSelection: (Long) -> Unit = {},
    onBulkComplete: () -> Unit = {},
    onBulkArchive: () -> Unit = {},
    onBulkDelete: () -> Unit = {},
) {
    val dateFormatter = remember { dayTitleFormatter(Locale.getDefault()) }
    val dateTitle = selectedDay.format(dateFormatter)

    val headerModifier = if (isTwoPane) {
        Modifier
            .fillMaxWidth()
            .height(64.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(bottom = AppSpacing.small)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding),
    ) {
        Box(
            modifier = headerModifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            if (isSelectionMode) {
                AgendaSelectionBar(
                    selectedCount = selectedCount,
                    onComplete = onBulkComplete,
                    onArchive = onBulkArchive,
                    onDelete = onBulkDelete,
                )
            } else {
                Text(
                    text = dateTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (items.isEmpty()) {
            val emptyModifier = if (isTwoPane) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.large)
            }
            Box(
                modifier = emptyModifier,
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(AppSpacing.small))
                    Text(
                        text = stringResource(R.string.calendar_agenda_empty_on_date, dateTitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val listModifier = if (isTwoPane) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier.fillMaxWidth()
            }
            LazyColumn(
                modifier = listModifier,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
            ) {
                items(items = items, key = { agendaItemKey(it) }) { item ->
                    when (item) {
                        is AgendaItem.Concrete -> TaskItem(
                            task = item.task,
                            subtaskProgress = subtaskProgress[item.task.id],
                            onToggleComplete = { onToggleComplete(item) },
                            onEditClick = { onItemClick(item) },
                            onArchiveClick = { onArchive(item) },
                            onPinClick = { onTogglePin(item) },
                            isSelectionMode = isSelectionMode,
                            isSelected = item.task.id in selectedIds,
                            onLongPress = { onLongPressTask(item.task.id) },
                            onToggleSelection = { onToggleSelection(item.task.id) },
                        )
                        is AgendaItem.Projected -> ProjectedAgendaRow(
                            projected = item,
                            onClick = { if (!isSelectionMode) onItemClick(item) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.medium))

        // FAB (CAL-19): hidden in selection mode to avoid conflicting with bulk actions.
        if (!isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacing.medium),
                contentAlignment = Alignment.CenterEnd,
            ) {
                FloatingActionButton(
                    onClick = onAddTaskClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_agenda_add_task),
                    )
                }
            }
        }
    }
}

@Composable
internal fun AgendaSelectionBar(
    selectedCount: Int,
    onComplete: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.agenda_multi_select_count, selectedCount),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onComplete) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.cd_mark_completed),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onArchive) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = stringResource(R.string.action_archive),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// Stable LazyColumn key. Concrete and Projected must never collide even if their underlying
// id and parentTaskId happen to match (they can — Projected.parentTaskId is the chain root
// which may also be a Concrete elsewhere in the same list). Namespace with a prefix.
internal fun agendaItemKey(item: AgendaItem): String = when (item) {
    is AgendaItem.Concrete -> "concrete-${item.task.id}"
    is AgendaItem.Projected -> "projected-${item.parentTaskId}-${item.date}"
}

// Locale-aware "full day of week, full date" formatter — e.g. "Tuesday, May 12, 2026".
internal fun dayTitleFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
