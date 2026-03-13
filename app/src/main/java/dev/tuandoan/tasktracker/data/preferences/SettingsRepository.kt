package dev.tuandoan.tasktracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for managing user preferences via DataStore.
 * Single source of truth for theme, dynamic color, and language settings.
 */
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val TIP_FAB_SHOWN = booleanPreferencesKey("tip_fab_shown")
        val TIP_TAG_CHIPS_SHOWN = booleanPreferencesKey("tip_tag_chips_shown")
        val TIP_STATS_CARDS_SHOWN = booleanPreferencesKey("tip_stats_cards_shown")
    }

    /**
     * Flow of user preferences, emitting on every change.
     */
    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            languageTag = prefs[Keys.LANGUAGE_TAG] ?: "",
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            tipFabShown = prefs[Keys.TIP_FAB_SHOWN] ?: false,
            tipTagChipsShown = prefs[Keys.TIP_TAG_CHIPS_SHOWN] ?: false,
            tipStatsCardsShown = prefs[Keys.TIP_STATS_CARDS_SHOWN] ?: false,
        )
    }

    /**
     * Updates the theme mode preference.
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    /**
     * Updates the dynamic color preference.
     */
    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_COLOR] = enabled
        }
    }

    /**
     * Updates the language tag preference.
     * Empty string means "follow system".
     */
    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_TAG] = tag
        }
    }

    /**
     * Marks onboarding as completed so the carousel is not shown again.
     */
    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    /**
     * Marks a feature tip as shown so it won't appear again.
     */
    suspend fun setTipShown(key: Preferences.Key<Boolean>) {
        context.dataStore.edit { prefs ->
            prefs[key] = true
        }
    }

    /** Expose tip keys for ViewModel usage. */
    object TipKeys {
        val FAB = Keys.TIP_FAB_SHOWN
        val TAG_CHIPS = Keys.TIP_TAG_CHIPS_SHOWN
        val STATS_CARDS = Keys.TIP_STATS_CARDS_SHOWN
    }
}
