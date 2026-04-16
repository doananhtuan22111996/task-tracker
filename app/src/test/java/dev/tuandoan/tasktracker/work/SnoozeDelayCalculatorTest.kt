package dev.tuandoan.tasktracker.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SnoozeDelayCalculatorTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `snooze 15 min constant is 900000 ms`() {
        assertEquals(15 * 60 * 1000L, SnoozeDelayCalculator.SNOOZE_15_MIN_MS)
    }

    @Test
    fun `snooze 1 hour constant is 3600000 ms`() {
        assertEquals(60 * 60 * 1000L, SnoozeDelayCalculator.SNOOZE_1_HOUR_MS)
    }

    @Test
    fun `tomorrow 9 AM when current time is afternoon`() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 17),
            LocalTime.of(14, 30),
            zone,
        ).toInstant().toEpochMilli()

        val delay = SnoozeDelayCalculator.calculateTomorrow9AmDelay(now, zone)

        val expected = ZonedDateTime.of(
            LocalDate.of(2026, 4, 18),
            LocalTime.of(9, 0),
            zone,
        ).toInstant().toEpochMilli() - now

        assertEquals(expected, delay)
    }

    @Test
    fun `tomorrow 9 AM when current time is before 9 AM snoozes to today 9 AM`() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 17),
            LocalTime.of(7, 30),
            zone,
        ).toInstant().toEpochMilli()

        val delay = SnoozeDelayCalculator.calculateTomorrow9AmDelay(now, zone)

        val expectedTarget = ZonedDateTime.of(
            LocalDate.of(2026, 4, 17),
            LocalTime.of(9, 0),
            zone,
        ).toInstant().toEpochMilli()

        assertEquals(expectedTarget - now, delay)
    }

    @Test
    fun `tomorrow 9 AM at exactly 9 AM snoozes to next day`() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 17),
            LocalTime.of(9, 0),
            zone,
        ).toInstant().toEpochMilli()

        val delay = SnoozeDelayCalculator.calculateTomorrow9AmDelay(now, zone)

        val expectedTarget = ZonedDateTime.of(
            LocalDate.of(2026, 4, 18),
            LocalTime.of(9, 0),
            zone,
        ).toInstant().toEpochMilli()

        assertEquals(expectedTarget - now, delay)
    }

    @Test
    fun `tomorrow 9 AM at 8_59 AM snoozes to today 9 AM`() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 17),
            LocalTime.of(8, 59),
            zone,
        ).toInstant().toEpochMilli()

        val delay = SnoozeDelayCalculator.calculateTomorrow9AmDelay(now, zone)

        val expectedTarget = ZonedDateTime.of(
            LocalDate.of(2026, 4, 17),
            LocalTime.of(9, 0),
            zone,
        ).toInstant().toEpochMilli()

        assertEquals(expectedTarget - now, delay)
    }

    @Test
    fun `tomorrow 9 AM at midnight snoozes to same day 9 AM`() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 17),
            LocalTime.of(0, 0),
            zone,
        ).toInstant().toEpochMilli()

        val delay = SnoozeDelayCalculator.calculateTomorrow9AmDelay(now, zone)

        val expectedTarget = ZonedDateTime.of(
            LocalDate.of(2026, 4, 17),
            LocalTime.of(9, 0),
            zone,
        ).toInstant().toEpochMilli()

        assertEquals(expectedTarget - now, delay)
    }

    @Test
    fun `tomorrow 9 AM at 9_01 PM snoozes to next day 9 AM`() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 17),
            LocalTime.of(21, 1),
            zone,
        ).toInstant().toEpochMilli()

        val delay = SnoozeDelayCalculator.calculateTomorrow9AmDelay(now, zone)

        assertTrue(delay > 0)
        val targetMs = now + delay
        val target = java.time.Instant.ofEpochMilli(targetMs).atZone(zone)
        assertEquals(LocalTime.of(9, 0), target.toLocalTime())
        assertEquals(LocalDate.of(2026, 4, 18), target.toLocalDate())
    }

    @Test
    fun `delay is always positive`() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 17),
            LocalTime.of(8, 0),
            zone,
        ).toInstant().toEpochMilli()

        val delay = SnoozeDelayCalculator.calculateTomorrow9AmDelay(now, zone)
        assertTrue(delay > 0)
    }
}
