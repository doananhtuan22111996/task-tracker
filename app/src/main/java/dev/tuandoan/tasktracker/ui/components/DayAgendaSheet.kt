package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
 * Day agenda bottom sheet (CAL-17 + CAL-18 + CAL-19 + CAL-24). Renders a mixed list of
 * [AgendaItem.Concrete] (full [TaskItem]) and [AgendaItem.Projected] (read-only
 * [ProjectedAgendaRow]). All interaction handlers take the [AgendaItem] so the VM can
 * materialize projections before dispatching to the concrete handler (ADR-002 option c).
 *
 * Scope for the calendar agenda context:
 * - `onClick` / `onToggleComplete` / `onArchive` / `onTogglePin` wired through the VM.
 * - `onDuplicateClick` / `onSkipOccurrence` left at `TaskItem`'s `{}` defaults — users
 *   duplicate/skip from the task list.
 * - Multi-select and swipe-to-archive land in CAL-20 / CAL-21.
 * - Empty-day state gets its dedicated polish in CAL-22.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAgendaSheet(
    selectedDay: LocalDate,
    items: List<AgendaItem>,
    subtaskProgress: Map<Long, SubtaskProgress>,
    onItemClick: (AgendaItem) -> Unit,
    onToggleComplete: (AgendaItem) -> Unit,
    onArchive: (AgendaItem) -> Unit,
    onTogglePin: (AgendaItem) -> Unit,
    onAddTaskClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val dateFormatter = dayTitleFormatter(Locale.getDefault())
    val dateTitle = selectedDay.format(dateFormatter)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding),
        ) {
            Text(
                text = dateTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = AppSpacing.small),
            )

            if (items.isEmpty()) {
                // CAL-22: polished empty-day state. Icon + date-inlined message. FAB below
                // stays in place (CAL-19) so the user can still create a task for this day.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.large),
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
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
                            )
                            is AgendaItem.Projected -> ProjectedAgendaRow(
                                projected = item,
                                onClick = { onItemClick(item) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.medium))

            // FAB (CAL-19): opens task editor with dueDate prefilled to selectedDay.
            // Right-aligned inside the sheet so it behaves like the floating action it is,
            // without colliding with the sheet's own drag-to-dismiss gestures.
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

// Stable LazyColumn key. Concrete and Projected must never collide even if their underlying
// id and parentTaskId happen to match (they can — Projected.parentTaskId is the chain root
// which may also be a Concrete elsewhere in the same list). Namespace with a prefix.
private fun agendaItemKey(item: AgendaItem): String = when (item) {
    is AgendaItem.Concrete -> "concrete-${item.task.id}"
    is AgendaItem.Projected -> "projected-${item.parentTaskId}-${item.date}"
}

// Locale-aware "full day of week, full date" formatter — e.g. "Tuesday, May 12, 2026".
private fun dayTitleFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
