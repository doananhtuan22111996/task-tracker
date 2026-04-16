package dev.tuandoan.tasktracker.work

import java.time.LocalTime
import java.time.ZoneId

object SnoozeDelayCalculator {

    const val SNOOZE_15_MIN_MS = 15 * 60 * 1000L
    const val SNOOZE_1_HOUR_MS = 60 * 60 * 1000L

    fun calculateTomorrow9AmDelay(nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val nineAm = LocalTime.of(9, 0)
        val target = if (now.toLocalTime().isBefore(nineAm)) {
            now.toLocalDate().atTime(nineAm).atZone(zoneId)
        } else {
            now.toLocalDate().plusDays(1).atTime(nineAm).atZone(zoneId)
        }
        return target.toInstant().toEpochMilli() - nowMillis
    }
}
