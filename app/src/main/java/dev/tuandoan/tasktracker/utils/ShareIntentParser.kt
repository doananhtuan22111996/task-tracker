package dev.tuandoan.tasktracker.utils

import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase

/**
 * Parsed payload from an incoming [android.content.Intent.ACTION_SEND] intent (CAP-01).
 *
 * @param title Pre-filled task title (enforces [TaskFormUseCase.MAX_TITLE_LENGTH] limit).
 * @param description Pre-filled task description (enforces [TaskFormUseCase.MAX_DESCRIPTION_LENGTH] limit).
 */
data class SharePayload(val title: String, val description: String = "")

/**
 * Parser for incoming Android share targets (CAP-01 / FR-14 / FR-15).
 * Extracts URL and text payloads into structured title and description fields.
 */
object ShareIntentParser {

    private val URL_REGEX = Regex("""https?://\S+""")

    /**
     * Parses incoming ACTION_SEND intent extras into a [SharePayload].
     *
     * Rules per FR-15:
     * - If text contains a URL: URL is placed in description, subject/title in title.
     *   If subject is blank, any non-URL text around the link becomes the title.
     *   If text is purely a URL with no subject, URL is used for both title and description.
     * - If text does NOT contain a URL:
     *   If subject is non-blank and differs from text, subject is title and text is description.
     *   Otherwise, text (or subject if text is blank) is title.
     * - Titles and descriptions are clamped to [TaskFormUseCase.MAX_TITLE_LENGTH] and
     *   [TaskFormUseCase.MAX_DESCRIPTION_LENGTH].
     * - Returns null if both [subject] and [text] are null or blank.
     */
    fun parse(subject: String?, text: String?): SharePayload? {
        val cleanSubject = subject?.trim().orEmpty()
        val cleanText = text?.trim().orEmpty()

        if (cleanSubject.isEmpty() && cleanText.isEmpty()) {
            return null
        }

        val urlMatch = URL_REGEX.find(cleanText)

        val (rawTitle, rawDesc) = if (urlMatch != null) {
            val url = urlMatch.value
            val nonUrlText = cleanText.replace(url, "").trim()
            val title = when {
                cleanSubject.isNotEmpty() -> cleanSubject
                nonUrlText.isNotEmpty() -> nonUrlText
                else -> url
            }
            val desc = if (cleanSubject.isNotEmpty() && cleanText != cleanSubject) {
                cleanText
            } else {
                url
            }
            title to desc
        } else {
            if (cleanSubject.isNotEmpty() && cleanText.isNotEmpty() && cleanSubject != cleanText) {
                cleanSubject to cleanText
            } else {
                val singleText = cleanText.ifEmpty { cleanSubject }
                singleText to ""
            }
        }

        return SharePayload(
            title = rawTitle.take(TaskFormUseCase.MAX_TITLE_LENGTH),
            description = rawDesc.take(TaskFormUseCase.MAX_DESCRIPTION_LENGTH),
        )
    }
}
