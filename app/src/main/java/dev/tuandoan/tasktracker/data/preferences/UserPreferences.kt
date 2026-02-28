package dev.tuandoan.tasktracker.data.preferences

/**
 * Represents the user's theme mode preference.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

/**
 * Data class holding all user preferences.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val languageTag: String = "", // Empty string means "follow system"
)
