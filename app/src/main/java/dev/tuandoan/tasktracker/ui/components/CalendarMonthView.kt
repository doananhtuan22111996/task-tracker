package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Thin wrapper around [HorizontalCalendar] (ADR-001). Hides the library's types so the rest of
 * the codebase only sees our domain primitives — if R1 (maintainer abandonment) materializes,
 * swapping to a DIY grid stays bounded to this file.
 *
 * Current scope (CAL-11):
 * - Single-month window: `startMonth = endMonth = visibleMonth`. Paging across months lands
 *   in CAL-15 by expanding the window and syncing `state.firstVisibleMonth` with the
 *   ViewModel's `visibleMonth`.
 * - Default Kizitonwose month-header is used. CAL-13 replaces it with a localized short-name
 *   weekday row.
 * - `firstDayOfWeekFromLocale()` honors device locale (Sun-first `en-US`, Mon-first `de/fr`).
 */
@Composable
fun CalendarMonthView(
    visibleMonth: YearMonth,
    selectedDay: LocalDate,
    decorations: Map<LocalDate, DayDecoration>,
    onDayClick: (LocalDate) -> Unit,
    onJumpToMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberCalendarState(
        startMonth = visibleMonth,
        endMonth = visibleMonth,
        firstVisibleMonth = visibleMonth,
        firstDayOfWeek = firstDayOfWeekFromLocale(),
    )

    // When the ViewModel's visibleMonth changes externally (e.g. restored from SavedStateHandle,
    // or from onTodayClick), re-scroll the library to match. For a single-month window this is a
    // no-op; CAL-15 will rely on this hook for paging.
    LaunchedEffect(visibleMonth) {
        if (state.firstVisibleMonth.yearMonth != visibleMonth) {
            state.scrollToMonth(visibleMonth)
        }
    }

    // Memoized at composition start; midnight rollover won't update until recomposition.
    // Good enough for v1.11.0 MVP — a ticker-based refresh is a future enhancement.
    val today = remember { LocalDate.now() }

    HorizontalCalendar(
        state = state,
        modifier = modifier,
        dayContent = { day ->
            DayCell(
                day = day,
                decoration = decorations[day.date],
                selected = day.date == selectedDay,
                today = day.date == today,
                onClick = { clickedDate ->
                    onDayClick(clickedDate)
                    // If the user tapped a leading/trailing day from an adjacent month,
                    // follow them into that month so the grid doesn't get stuck showing a
                    // selected day that's clearly "elsewhere".
                    if (day.position != DayPosition.MonthDate) {
                        onJumpToMonth(YearMonth.from(clickedDate))
                    }
                },
            )
        },
        monthHeader = { month ->
            // Minimal fallback header until CAL-13 lands. Shows weekday short names using the
            // calendar state's firstDayOfWeek so the order matches the rendered grid.
            val daysOfWeek = month.weekDays.first().map { it.date.dayOfWeek }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                daysOfWeek.forEach { dow ->
                    Text(
                        text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
