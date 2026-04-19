package dev.tuandoan.tasktracker.work

import org.junit.Assert.assertEquals
import org.junit.Test

class SnoozeDelayCalculatorTest {

    @Test
    fun `snooze 15 min constant is 900000 ms`() {
        assertEquals(15 * 60 * 1000L, SnoozeDelayCalculator.SNOOZE_15_MIN_MS)
    }

    @Test
    fun `snooze 1 hour constant is 3600000 ms`() {
        assertEquals(60 * 60 * 1000L, SnoozeDelayCalculator.SNOOZE_1_HOUR_MS)
    }
}
