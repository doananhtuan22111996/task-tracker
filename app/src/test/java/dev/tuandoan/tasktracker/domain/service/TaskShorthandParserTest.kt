package dev.tuandoan.tasktracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TaskShorthandParserTest {

    // Fixed test clock: Wednesday, Oct 14, 2026, 10:00:00 UTC
    private val fixedZone = ZoneId.of("UTC")
    private val fixedInstant = Instant.parse("2026-10-14T10:00:00Z")
    private val testClock = Clock.fixed(fixedInstant, fixedZone)

    @Test
    fun `plain text without tokens returns original title and null attributes`() {
        val input = "Buy milk at the grocery store"
        val parsed = TaskShorthandParser.parse(input, testClock)

        assertEquals("Buy milk at the grocery store", parsed.cleanTitle)
        assertFalse(parsed.hasTokens)
        assertNull(parsed.dueAt)
        assertFalse(parsed.dueAtHasTime)
        assertNull(parsed.priority)
        assertNull(parsed.tag)
    }

    @Test
    fun `priority tokens parse correctly`() {
        val highParsed = TaskShorthandParser.parse("Important task !high", testClock)
        assertEquals("Important task", highParsed.cleanTitle)
        assertEquals(2, highParsed.priority)

        val hParsed = TaskShorthandParser.parse("Urgent fix !h", testClock)
        assertEquals("Urgent fix", hParsed.cleanTitle)
        assertEquals(2, hParsed.priority)

        val threeParsed = TaskShorthandParser.parse("Critical bug !3", testClock)
        assertEquals("Critical bug", threeParsed.cleanTitle)
        assertEquals(2, threeParsed.priority)

        val medParsed = TaskShorthandParser.parse("Regular task !med", testClock)
        assertEquals("Regular task", medParsed.cleanTitle)
        assertEquals(1, medParsed.priority)

        val mParsed = TaskShorthandParser.parse("Medium task !m", testClock)
        assertEquals("Medium task", mParsed.cleanTitle)
        assertEquals(1, mParsed.priority)

        val lowParsed = TaskShorthandParser.parse("Someday task !low", testClock)
        assertEquals("Someday task", lowParsed.cleanTitle)
        assertEquals(0, lowParsed.priority)

        val oneParsed = TaskShorthandParser.parse("Low task !1", testClock)
        assertEquals("Low task", oneParsed.cleanTitle)
        assertEquals(0, oneParsed.priority)
    }

    @Test
    fun `tag tokens parse and normalize to uppercase`() {
        val parsed = TaskShorthandParser.parse("Review pull request #work", testClock)
        assertEquals("Review pull request", parsed.cleanTitle)
        assertEquals("WORK", parsed.tag)

        val multiWordTag = TaskShorthandParser.parse("File returns #personal_tax", testClock)
        assertEquals("File returns", multiWordTag.cleanTitle)
        assertEquals("PERSONAL_TAX", multiWordTag.tag)
    }

    @Test
    fun `relative date today defaults to end of day`() {
        val parsed = TaskShorthandParser.parse("Submit daily report today", testClock)
        assertEquals("Submit daily report", parsed.cleanTitle)
        assertNotNull(parsed.dueAt)
        assertFalse(parsed.dueAtHasTime)

        val dueDateTime = Instant.ofEpochMilli(parsed.dueAt!!).atZone(fixedZone)
        assertEquals(LocalDate.of(2026, 10, 14), dueDateTime.toLocalDate())
        assertEquals(23, dueDateTime.hour)
        assertEquals(59, dueDateTime.minute)
    }

    @Test
    fun `relative date tomorrow defaults to end of day tomorrow`() {
        val parsed = TaskShorthandParser.parse("Call dentist tomorrow", testClock)
        assertEquals("Call dentist", parsed.cleanTitle)
        assertNotNull(parsed.dueAt)
        assertFalse(parsed.dueAtHasTime)

        val dueDateTime = Instant.ofEpochMilli(parsed.dueAt!!).atZone(fixedZone)
        assertEquals(LocalDate.of(2026, 10, 15), dueDateTime.toLocalDate())
        assertEquals(23, dueDateTime.hour)
        assertEquals(59, dueDateTime.minute)
    }

    @Test
    fun `relative date next week calculates 7 days ahead`() {
        val parsed = TaskShorthandParser.parse("Weekly sync next week", testClock)
        assertEquals("Weekly sync", parsed.cleanTitle)
        assertNotNull(parsed.dueAt)

        val dueDateTime = Instant.ofEpochMilli(parsed.dueAt!!).atZone(fixedZone)
        assertEquals(LocalDate.of(2026, 10, 21), dueDateTime.toLocalDate())
    }

    @Test
    fun `weekday keyword finds next occurrence`() {
        // Today is Wednesday Oct 14, 2026. Friday is Oct 16.
        val parsedFriday = TaskShorthandParser.parse("Send invoice Friday", testClock)
        assertEquals("Send invoice", parsedFriday.cleanTitle)
        val fridayDate = Instant.ofEpochMilli(parsedFriday.dueAt!!).atZone(fixedZone).toLocalDate()
        assertEquals(LocalDate.of(2026, 10, 16), fridayDate)
        assertEquals(DayOfWeek.FRIDAY, fridayDate.dayOfWeek)

        // Monday is Oct 19.
        val parsedMonday = TaskShorthandParser.parse("Team standup on Monday", testClock)
        assertEquals("Team standup", parsedMonday.cleanTitle)
        val mondayDate = Instant.ofEpochMilli(parsedMonday.dueAt!!).atZone(fixedZone).toLocalDate()
        assertEquals(LocalDate.of(2026, 10, 19), mondayDate)
        assertEquals(DayOfWeek.MONDAY, mondayDate.dayOfWeek)
    }

    @Test
    fun `date and time combination sets dueAtHasTime true`() {
        val parsed = TaskShorthandParser.parse("Dentist appointment tomorrow at 3pm", testClock)
        assertEquals("Dentist appointment", parsed.cleanTitle)
        assertTrue(parsed.dueAtHasTime)

        val dueDateTime = Instant.ofEpochMilli(parsed.dueAt!!).atZone(fixedZone)
        assertEquals(LocalDate.of(2026, 10, 15), dueDateTime.toLocalDate())
        assertEquals(15, dueDateTime.hour)
        assertEquals(0, dueDateTime.minute)
    }

    @Test
    fun `explicit time with minutes parses correctly`() {
        val parsed = TaskShorthandParser.parse("Flight departure tomorrow at 5:30pm", testClock)
        assertEquals("Flight departure", parsed.cleanTitle)
        assertTrue(parsed.dueAtHasTime)

        val dueDateTime = Instant.ofEpochMilli(parsed.dueAt!!).atZone(fixedZone)
        assertEquals(17, dueDateTime.hour)
        assertEquals(30, dueDateTime.minute)
    }

    @Test
    fun `24-hour time format parses correctly`() {
        val parsed = TaskShorthandParser.parse("Server maintenance tomorrow at 22:00", testClock)
        assertEquals("Server maintenance", parsed.cleanTitle)
        assertTrue(parsed.dueAtHasTime)

        val dueDateTime = Instant.ofEpochMilli(parsed.dueAt!!).atZone(fixedZone)
        assertEquals(22, dueDateTime.hour)
        assertEquals(0, dueDateTime.minute)
    }

    @Test
    fun `time only in future defaults to today`() {
        // Current clock is 10:00 UTC. 3pm (15:00) is in the future.
        val parsed = TaskShorthandParser.parse("Coffee chat at 3pm", testClock)
        assertEquals("Coffee chat", parsed.cleanTitle)
        assertTrue(parsed.dueAtHasTime)

        val dueDateTime = Instant.ofEpochMilli(parsed.dueAt!!).atZone(fixedZone)
        assertEquals(LocalDate.of(2026, 10, 14), dueDateTime.toLocalDate())
        assertEquals(15, dueDateTime.hour)
    }

    @Test
    fun `time only in past rolls over to tomorrow`() {
        // Current clock is 10:00 UTC. 8am is in the past for today, so rolls to tomorrow.
        val parsed = TaskShorthandParser.parse("Morning workout at 8am", testClock)
        assertEquals("Morning workout", parsed.cleanTitle)
        assertTrue(parsed.dueAtHasTime)

        val dueDateTime = Instant.ofEpochMilli(parsed.dueAt!!).atZone(fixedZone)
        assertEquals(LocalDate.of(2026, 10, 15), dueDateTime.toLocalDate())
        assertEquals(8, dueDateTime.hour)
    }

    @Test
    fun `combined full syntax extracts all attributes cleanly`() {
        val input = "Submit quarterly tax return tomorrow at 4pm !high #finance"
        val parsed = TaskShorthandParser.parse(input, testClock)

        assertEquals("Submit quarterly tax return", parsed.cleanTitle)
        assertTrue(parsed.hasTokens)
        assertEquals(2, parsed.priority)
        assertEquals("FINANCE", parsed.tag)
        assertTrue(parsed.dueAtHasTime)

        val dueDateTime = Instant.ofEpochMilli(parsed.dueAt!!).atZone(fixedZone)
        assertEquals(LocalDate.of(2026, 10, 15), dueDateTime.toLocalDate())
        assertEquals(16, dueDateTime.hour)
    }

    @Test
    fun `vietnamese relative date tokens parse correctly`() {
        val parsedToday = TaskShorthandParser.parse("Mua đồ ăn hôm nay", testClock)
        assertEquals("Mua đồ ăn", parsedToday.cleanTitle)
        val todayDate = Instant.ofEpochMilli(parsedToday.dueAt!!).atZone(fixedZone).toLocalDate()
        assertEquals(LocalDate.of(2026, 10, 14), todayDate)

        val parsedTomorrow = TaskShorthandParser.parse("Gặp khách hàng ngày mai", testClock)
        assertEquals("Gặp khách hàng", parsedTomorrow.cleanTitle)
        val tomorrowDate = Instant.ofEpochMilli(parsedTomorrow.dueAt!!).atZone(fixedZone).toLocalDate()
        assertEquals(LocalDate.of(2026, 10, 15), tomorrowDate)
    }

    @Test
    fun `tokens only input does not produce empty cleanTitle`() {
        val input = "!high #work"
        val parsed = TaskShorthandParser.parse(input, testClock)

        assertEquals("!high #work", parsed.cleanTitle)
        assertEquals(2, parsed.priority)
        assertEquals("WORK", parsed.tag)
    }

    @Test
    fun `punctuation connecting tokens is trimmed cleanly`() {
        val input = "Call doctor, tomorrow, !high, #health."
        val parsed = TaskShorthandParser.parse(input, testClock)

        assertEquals("Call doctor", parsed.cleanTitle)
        assertEquals(2, parsed.priority)
        assertEquals("HEALTH", parsed.tag)
        assertNotNull(parsed.dueAt)
    }
}
