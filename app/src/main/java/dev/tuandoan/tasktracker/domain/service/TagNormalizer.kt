package dev.tuandoan.tasktracker.domain.service

import java.util.Locale

/**
 * Canonicalizes tag values to a single form: trimmed + UPPERCASE via [Locale.ROOT].
 *
 * Locale.ROOT is intentional: user devices in Turkish locale would otherwise produce
 * non-deterministic output (`i` → `İ`), splitting the canonical form across devices.
 */
object TagNormalizer {

    fun normalize(input: String?): String? {
        if (input == null) return null
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.uppercase(Locale.ROOT)
    }
}
