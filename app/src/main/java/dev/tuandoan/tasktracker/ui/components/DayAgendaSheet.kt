package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Day agenda bottom sheet (CAL-17 + CAL-18 + CAL-19). Opens on day tap from
 * [CalendarMonthView]; shows a localized date title, the day's tasks rendered with the full
 * [TaskItem] composable, and a FAB (CAL-19) that opens the task editor prefilled with the
 * selected day's date.
 *
 * Scope for the calendar agenda context:
 * - `onToggleComplete`, `onEditClick`, `onArchiveClick`, `onPinClick` wired through the VM.
 * - `onDuplicateClick` / `onSkipOccurrence` left at `TaskItem`'s `{}` defaults — users
 *   duplicate/skip from the task list.
 * - Multi-select and swipe-to-archive land in CAL-20 / CAL-21.
 * - Empty-day state gets its dedicated polish in CAL-22.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAgendaSheet(
    selectedDay: LocalDate,
    tasks: List<Task>,
    subtaskProgress: Map<Long, SubtaskProgress>,
    onTaskClick: (Long) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onArchive: (Task) -> Unit,
    onTogglePin: (Task) -> Unit,
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
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
                ) {
                    items(items = tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            subtaskProgress = subtaskProgress[task.id],
                            onToggleComplete = { onToggleComplete(task) },
                            onEditClick = { onTaskClick(task.id) },
                            onArchiveClick = { onArchive(task) },
                            onPinClick = { onTogglePin(task) },
                        )
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

// Locale-aware "full day of week, full date" formatter — e.g. "Tuesday, May 12, 2026".
private fun dayTitleFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
