package dev.tuandoan.tasktracker.utils

import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ShareIntentParserTest {

    @Test
    fun `parse returns null when subject and text are both null`() {
        val result = ShareIntentParser.parse(subject = null, text = null)
        assertNull(result)
    }

    @Test
    fun `parse returns null when subject and text are blank`() {
        val result = ShareIntentParser.parse(subject = "   ", text = "\n\t")
        assertNull(result)
    }

    @Test
    fun `parse extracts subject as title and text as description when text contains url and subject is present`() {
        val subject = "Google DeepMind - Research"
        val text = "https://deepmind.google/research"

        val result = ShareIntentParser.parse(subject = subject, text = text)

        assertNotNull(result)
        assertEquals("Google DeepMind - Research", result?.title)
        assertEquals("https://deepmind.google/research", result?.description)
    }

    @Test
    fun `parse extracts url as title and description when text is only a url and subject is null`() {
        val text = "https://kotlinlang.org"

        val result = ShareIntentParser.parse(subject = null, text = text)

        assertNotNull(result)
        assertEquals("https://kotlinlang.org", result?.title)
        assertEquals("https://kotlinlang.org", result?.description)
    }

    @Test
    fun `parse extracts url as title and description when text is only a url and subject is blank`() {
        val text = "https://kotlinlang.org"

        val result = ShareIntentParser.parse(subject = "   ", text = text)

        assertNotNull(result)
        assertEquals("https://kotlinlang.org", result?.title)
        assertEquals("https://kotlinlang.org", result?.description)
    }

    @Test
    fun `parse extracts non-url text as title and url as description when text has both and subject is null`() {
        val text = "Check this out https://developer.android.com/compose"

        val result = ShareIntentParser.parse(subject = null, text = text)

        assertNotNull(result)
        assertEquals("Check this out", result?.title)
        assertEquals("https://developer.android.com/compose", result?.description)
    }

    @Test
    fun `parse extracts text as title and leaves description empty when no url and no subject`() {
        val text = "Buy milk and groceries"

        val result = ShareIntentParser.parse(subject = null, text = text)

        assertNotNull(result)
        assertEquals("Buy milk and groceries", result?.title)
        assertEquals("", result?.description)
    }

    @Test
    fun `parse extracts subject as title and text as description when both present without url`() {
        val subject = "Meeting Notes"
        val text = "Discuss Q4 objectives with engineering team"

        val result = ShareIntentParser.parse(subject = subject, text = text)

        assertNotNull(result)
        assertEquals("Meeting Notes", result?.title)
        assertEquals("Discuss Q4 objectives with engineering team", result?.description)
    }

    @Test
    fun `parse extracts text as title when subject equals text without url`() {
        val text = "Single line idea"

        val result = ShareIntentParser.parse(subject = text, text = text)

        assertNotNull(result)
        assertEquals("Single line idea", result?.title)
        assertEquals("", result?.description)
    }

    @Test
    fun `parse extracts subject as title when text is null or blank`() {
        val subject = "Only subject provided"

        val result = ShareIntentParser.parse(subject = subject, text = null)

        assertNotNull(result)
        assertEquals("Only subject provided", result?.title)
        assertEquals("", result?.description)
    }

    @Test
    fun `parse truncates title to MAX_TITLE_LENGTH and description to MAX_DESCRIPTION_LENGTH`() {
        val longSubject = "A".repeat(150)
        val longText = "https://example.com/" + "B".repeat(600)

        val result = ShareIntentParser.parse(subject = longSubject, text = longText)

        assertNotNull(result)
        assertEquals(TaskFormUseCase.MAX_TITLE_LENGTH, result?.title?.length)
        assertEquals(TaskFormUseCase.MAX_DESCRIPTION_LENGTH, result?.description?.length)
        assertEquals("A".repeat(100), result?.title)
        assertEquals(("https://example.com/" + "B".repeat(600)).take(500), result?.description)
    }
}
