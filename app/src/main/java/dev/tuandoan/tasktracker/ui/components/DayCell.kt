package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import java.time.LocalDate

/**
 * Minimum touch-target height for a day cell (PRD NFR a11y).
 * The library sizes cells to fill the 7-column grid width; height is our responsibility.
 */
private val CellHeight = 48.dp
private val DotSize = 6.dp
private val MaxDotsShown = 3

/**
 * One cell in the calendar month grid (CAL-12). Renders the date number plus decoration for
 * tasks on this day: up to [MaxDotsShown] distinct priority-color dots + an optional `+N`
 * overflow. Handles four visual states:
 *
 * - **Out-of-month** (leading/trailing days from adjacent months): dimmed date number, no dots.
 * - **Today**: filled `primary` circle with `onPrimary` date number.
 * - **Selected** (and not today): `primaryContainer` background with `onPrimaryContainer` text.
 * - **Today and selected**: selected border wins (today's fill stays; selected adds a border).
 *
 * Completed-only days (decoration has `completedCount == taskCount` and `taskCount > 0`) get a
 * muted dot alpha so the grid still shows rhythm without overloading the eye.
 */
@Composable
fun DayCell(
    day: CalendarDay,
    decoration: DayDecoration?,
    selected: Boolean,
    today: Boolean,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isInMonth = day.position == DayPosition.MonthDate
    val shape = RoundedCornerShape(12.dp)

    val background: Color = when {
        today && isInMonth -> MaterialTheme.colorScheme.primary
        selected && isInMonth -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val dateColor: Color = when {
        today && isInMonth -> MaterialTheme.colorScheme.onPrimary
        selected && isInMonth -> MaterialTheme.colorScheme.onPrimaryContainer
        isInMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val outOfMonthAlpha = if (isInMonth) 1f else 0.38f

    Box(
        modifier = modifier
            .size(CellHeight)
            .clip(shape)
            .clickable { onClick(day.date) }
            .background(background, shape)
            .let {
                // Selected-but-not-today: add an outline ring on top of primaryContainer.
                if (selected && !today && isInMonth) {
                    it.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    it
                }
            }
            .alpha(outOfMonthAlpha),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = dateColor,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            if (isInMonth && decoration != null) {
                DotRow(decoration)
            } else {
                Spacer(Modifier.height(DotSize))
            }
        }
    }
}

@Composable
private fun DotRow(decoration: DayDecoration) {
    val dots = dotsFor(decoration)
    val showOverflow = hasDotOverflow(decoration)
    // Muted alpha when every task on this day is already completed.
    val dotAlpha = if (decoration.taskCount > 0 && decoration.completedCount >= decoration.taskCount) {
        0.38f
    } else {
        1f
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dots.forEach { priority ->
            Box(
                modifier = Modifier
                    .size(DotSize)
                    .clip(CircleShape)
                    .background(priorityColor(priority))
                    .alpha(dotAlpha),
            )
        }
        if (showOverflow) {
            Text(
                text = "+",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun priorityColor(priority: Int): Color = when (priority) {
    2 -> MaterialTheme.colorScheme.error // HIGH
    1 -> MaterialTheme.colorScheme.tertiary // MEDIUM
    0 -> MaterialTheme.colorScheme.primary // LOW
    else -> MaterialTheme.colorScheme.outline
}

/**
 * Pure function: returns the sorted list of priority buckets to render as dots, capped at
 * [MaxDotsShown]. Sorted descending (HIGH first) so the most urgent dot is left-most.
 *
 * Exposed `internal` for JVM unit tests.
 */
internal fun dotsFor(decoration: DayDecoration): List<Int> = decoration.priorityBuckets
    .sortedDescending()
    .take(MaxDotsShown)

/**
 * Pure function: true when the day has more distinct priority buckets than we can render as
 * dots. Practically impossible with only 3 priority levels (LOW/MEDIUM/HIGH) but guarded so
 * a future priority-model change surfaces the overflow gracefully instead of silently
 * truncating.
 *
 * Exposed `internal` for JVM unit tests.
 */
internal fun hasDotOverflow(decoration: DayDecoration): Boolean = decoration.priorityBuckets.size > MaxDotsShown
