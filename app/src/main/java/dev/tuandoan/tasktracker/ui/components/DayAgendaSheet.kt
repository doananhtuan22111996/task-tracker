package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Day agenda bottom sheet (CAL-17 scaffold). Opens on day tap from [CalendarMonthView];
 * shows a localized date title and a simple list of the selected day's tasks.
 *
 * Scope is intentionally minimal for this ticket:
 * - Rows are plain title + optional time/tag line; tap routes to the task editor.
 * - CAL-18 replaces these rows with the full `TaskItem` composable (priority stripe,
 *   subtask progress, overflow menu).
 * - CAL-19 adds the FAB quick-add with prefilled date.
 * - CAL-20/21 add multi-select + swipe actions.
 * - CAL-22 refines the empty-day state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAgendaSheet(selectedDay: LocalDate, tasks: List<Task>, onTaskClick: (Long) -> Unit, onDismiss: () -> Unit) {
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

            if (tasks.isEmpty()) {
                Spacer(Modifier.height(AppSpacing.medium))
                Text(
                    text = stringResource(R.string.calendar_agenda_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = AppSpacing.medium),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(items = tasks, key = { it.id }) { task ->
                        AgendaRow(task = task, onClick = { onTaskClick(task.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.medium))
        }
    }
}

@Composable
private fun AgendaRow(task: Task, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AppSpacing.medium),
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (task.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        val secondary = secondaryLine(task)
        if (secondary != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Builds a short "tag · time" line for the agenda row when either is present. */
private fun secondaryLine(task: Task): String? {
    val tag = task.tag?.takeIf { it.isNotBlank() }
    val time = if (task.dueAtHasTime && task.dueAt != null) {
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
        Instant.ofEpochMilli(task.dueAt)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(formatter)
    } else {
        null
    }
    return listOfNotNull(tag, time).ifEmpty { null }?.joinToString(" · ")
}

// Locale-aware "full day of week, full date" formatter — e.g. "Tuesday, May 12, 2026".
private fun dayTitleFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
