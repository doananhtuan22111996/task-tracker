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
    val onboardingCompleted: Boolean = false,
    val tipFabShown: Boolean = false,
    val tipTagChipsShown: Boolean = false,
    val tipStatsCardsShown: Boolean = false,
    val firstLaunchDate: Long = 0L, // Epoch millis, set once on first app run
    val ratingPromptShown: Boolean = false,
)
