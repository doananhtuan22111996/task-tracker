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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Thin wrapper around [HorizontalCalendar] (ADR-001). Hides the library's types so the rest of
 * the codebase only sees our domain primitives — if R1 (maintainer abandonment) materializes,
 * swapping to a DIY grid stays bounded to this file.
 *
 * Current scope (CAL-11, CAL-15):
 * - Wide paging window: `visibleMonth ± WINDOW_MONTHS_RADIUS`. The library lazily composes
 *   only the visible month, so a wide window is cheap and preserves library state (scroll
 *   offset, pending animations) across month changes instead of recreating it every time.
 * - User-driven swipes emit `firstVisibleMonth` changes. A `snapshotFlow` observer forwards
 *   each change to [onJumpToMonth] so the ViewModel stays in sync. `.drop(1)` skips the
 *   initial emission so we don't echo the seeded `visibleMonth` back.
 * - When the ViewModel drives a change (today-click, saved-state restore, chevron tap),
 *   [LaunchedEffect] re-scrolls the library. The resulting `firstVisibleMonth` change is
 *   re-forwarded to `onJumpToMonth` which is idempotent against identical input — no loop.
 * - Default Kizitonwose month-header is used. CAL-13 extracts the inline version into a
 *   reusable, screenshot-testable component.
 * - `firstDayOfWeekFromLocale()` honors device locale (Sun-first `en-US`, Mon-first `de/fr`).
 */
// ~20 years each direction — realistic user lifetime, still cheap because Kizitonwose is lazy.
private const val WINDOW_MONTHS_RADIUS = 240L

@Composable
fun CalendarMonthView(
    visibleMonth: YearMonth,
    selectedDay: LocalDate,
    decorations: Map<LocalDate, DayDecoration>,
    onDayClick: (LocalDate) -> Unit,
    onJumpToMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Compute the paging window once from the initial visibleMonth. Kizitonwose's internal
    // rememberSaveable uses these as keys; changing them on every VM tick would recreate state
    // and cancel any in-flight swipe animation. Instead, we scroll the stable state below.
    val initialMonth = remember { visibleMonth }
    val state = rememberCalendarState(
        startMonth = initialMonth.minusMonths(WINDOW_MONTHS_RADIUS),
        endMonth = initialMonth.plusMonths(WINDOW_MONTHS_RADIUS),
        firstVisibleMonth = initialMonth,
        firstDayOfWeek = firstDayOfWeekFromLocale(),
    )

    // VM → library: re-scroll when the VM drives a change (today-click, saved-state restore,
    // chevron tap, out-of-month tap). No-op when the library is already there.
    LaunchedEffect(visibleMonth) {
        if (state.firstVisibleMonth.yearMonth != visibleMonth) {
            state.scrollToMonth(visibleMonth)
        }
    }

    // Library → VM: forward user-driven swipes. `drop(1)` skips the seeded initial emission so
    // we don't echo the starting month back. Subsequent values include both user swipes and
    // programmatic scrollToMonth results — both are safe because onJumpToMonth is idempotent
    // against identical input (StateFlow.value assignment with equal value is a no-op).
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleMonth.yearMonth }
            .drop(1)
            .distinctUntilChanged()
            .collect { month -> onJumpToMonth(month) }
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
