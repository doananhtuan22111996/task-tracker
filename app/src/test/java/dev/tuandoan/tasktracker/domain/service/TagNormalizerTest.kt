package dev.tuandoan.tasktracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagNormalizerTest {

    @Test
    fun `null input returns null`() {
        assertNull(TagNormalizer.normalize(null))
    }

    @Test
    fun `empty string returns null`() {
        assertNull(TagNormalizer.normalize(""))
    }

    @Test
    fun `blank whitespace returns null`() {
        assertNull(TagNormalizer.normalize("   "))
        assertNull(TagNormalizer.normalize("\t\n "))
    }

    @Test
    fun `lowercase is uppercased`() {
        assertEquals("WORK", TagNormalizer.normalize("work"))
    }

    @Test
    fun `mixed case is uppercased`() {
        assertEquals("WORK", TagNormalizer.normalize("Work"))
        assertEquals("WORK", TagNormalizer.normalize("WoRk"))
    }

    @Test
    fun `already uppercase passes through`() {
        assertEquals("WORK", TagNormalizer.normalize("WORK"))
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        assertEquals("WORK", TagNormalizer.normalize("  work  "))
        assertEquals("WORK", TagNormalizer.normalize("\twork\n"))
    }

    @Test
    fun `internal whitespace is preserved`() {
        assertEquals("TODO LIST", TagNormalizer.normalize("todo list"))
    }

    @Test
    fun `vietnamese diacritics are preserved`() {
        // "việc" is Vietnamese for "work/task" — must stay intact after normalization
        assertEquals("VIỆC", TagNormalizer.normalize("việc"))
    }

    @Test
    fun `hindi devanagari characters pass through unchanged`() {
        // Devanagari script has no case — should round-trip
        assertEquals("काम", TagNormalizer.normalize("काम"))
    }

    @Test
    fun `turkish dotless i uses Locale ROOT not device locale`() {
        // Locale.ROOT: "i" → "I" (deterministic)
        // Turkish default: "i" → "İ" (with dot above)
        // This test would fail with .uppercase(Locale.getDefault()) on a Turkish device.
        assertEquals("I", TagNormalizer.normalize("i"))
        assertEquals("IDEA", TagNormalizer.normalize("idea"))
    }

    @Test
    fun `numeric and symbolic characters are preserved`() {
        assertEquals("P1", TagNormalizer.normalize("p1"))
        assertEquals("WORK-URGENT", TagNormalizer.normalize("work-urgent"))
        assertEquals("@HOME", TagNormalizer.normalize("@home"))
    }
}
