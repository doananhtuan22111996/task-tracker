package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Weekday-label row rendered above the calendar month grid (CAL-13).
 *
 * Honors the device locale two ways:
 * - [firstDayOfWeek] determines the column order (e.g. Mon-first for most of Europe,
 *   Sun-first for US/CA/JP, Sat-first for Arabic locales). Callers pass the same value
 *   the calendar grid uses so the header and the grid stay aligned.
 * - Short weekday names come from `java.time`'s localized `TextStyle.SHORT` catalog.
 */
@Composable
fun WeekdayHeader(firstDayOfWeek: DayOfWeek, modifier: Modifier = Modifier, locale: Locale = Locale.getDefault()) {
    val names = weekdayShortNames(firstDayOfWeek, locale)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        names.forEach { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Returns the 7 locale-short weekday names starting from [firstDayOfWeek]. Kept pure so JVM
 * tests can verify locale handling without composing a UI tree.
 */
internal fun weekdayShortNames(firstDayOfWeek: DayOfWeek, locale: Locale): List<String> = (0 until 7).map { offset ->
    firstDayOfWeek.plus(offset.toLong()).getDisplayName(TextStyle.SHORT, locale)
}
