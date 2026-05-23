package dev.tuandoan.tasktracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.widget.model.WidgetSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated DataStore for per-`appWidgetId` widget configuration (v1.13.0, V13-07,
 * ADR-004 Decision 2 Option 2A).
 *
 * Why separate from [SettingsRepository] / [PrivacyRepository]:
 *  - **Lifecycle**: configs come and go with widget placements (`onDeleted` cleanup).
 *    Mixing this churn into the user-facing settings file would cause spurious
 *    re-emissions for unrelated UI Flow collectors.
 *  - **Audit**: one file = one concern. Three files (`settings`, `privacy`, `widget`)
 *    is the pattern ADR-003 set; ADR-004 continues it.
 *  - **Cleanup contract**: V13-10 wires `TaskTrackerWidgetReceiver.onDeleted` to
 *    [removeConfigs]. Centralizing the keys here keeps that hook honest.
 *
 * Per-widget keys: `widget_{id}_source` (one of [SOURCE_TODAY], [SOURCE_UPCOMING],
 * [SOURCE_PINNED], [SOURCE_TAG]) + `widget_{id}_tag` (only when source == tag).
 *
 * Default (no entry written) is `null`, **not** `Today` — callers substitute the
 * default explicitly so it's visible at the call site rather than buried here.
 */
private val Context.widgetConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget")

@Singleton
class WidgetConfigurationRepository internal constructor(private val dataStore: DataStore<Preferences>) {

    /**
     * Hilt-injected constructor used in production. The `internal` primary constructor
     * accepts any `DataStore<Preferences>` so JVM tests can inject a temp-file-backed
     * store via `PreferenceDataStoreFactory.create` without Robolectric (FB-04 pattern).
     */
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.widgetConfigDataStore)

    /**
     * Observes the configured source for [appWidgetId]. Emits `null` until the user
     * configures the widget; caller substitutes [WidgetSource.Today] (or whatever the
     * launcher's default placement experience should show).
     *
     * IOException recovery degrades to `null`-as-unconfigured rather than crashing
     * `provideGlance`; non-IO throwables propagate so real bugs surface (FB-06 pattern).
     */
    fun observeSource(appWidgetId: Int): Flow<WidgetSource?> = dataStore.data
        .catch { cause ->
            if (cause is IOException) {
                emit(emptyPreferences())
            } else {
                throw cause
            }
        }
        .map { prefs -> readSource(prefs, appWidgetId) }
        .distinctUntilChanged()

    /** One-shot read; primarily for `provideGlance` cold-start which doesn't want a Flow collector. */
    suspend fun getSourceOnce(appWidgetId: Int): WidgetSource? = observeSource(appWidgetId).first()

    /**
     * Persist [source] for [appWidgetId]. Idempotent: writing the same value is cheap.
     * `Tag` with a blank name is rejected — the configure activity (V13-08) is
     * responsible for normalization + validation; this is a defensive guard.
     */
    suspend fun setSource(appWidgetId: Int, source: WidgetSource) {
        if (source is WidgetSource.Tag && source.name.isBlank()) return
        dataStore.edit { prefs ->
            prefs[sourceKey(appWidgetId)] = source.toStored()
            if (source is WidgetSource.Tag) {
                prefs[tagKey(appWidgetId)] = source.name
            } else {
                prefs.remove(tagKey(appWidgetId))
            }
        }
    }

    /**
     * Remove configuration for the given widget ids. Called from
     * `TaskTrackerWidgetReceiver.onDeleted` (V13-10) so removed widgets don't leave
     * orphan entries that a fresh widget placement at the same id would inherit.
     * Empty input is a no-op without a DataStore write.
     */
    suspend fun removeConfigs(appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        dataStore.edit { prefs ->
            appWidgetIds.forEach { id ->
                prefs.remove(sourceKey(id))
                prefs.remove(tagKey(id))
            }
        }
    }

    private fun readSource(prefs: Preferences, appWidgetId: Int): WidgetSource? {
        val stored = prefs[sourceKey(appWidgetId)] ?: return null
        return when (stored) {
            SOURCE_TODAY -> WidgetSource.Today
            SOURCE_UPCOMING -> WidgetSource.Upcoming7d
            SOURCE_PINNED -> WidgetSource.Pinned
            SOURCE_TAG -> {
                val tag = prefs[tagKey(appWidgetId)]
                if (tag.isNullOrBlank()) null else WidgetSource.Tag(tag)
            }
            else -> null // forward-compat: unknown source from a future version
        }
    }

    private fun WidgetSource.toStored(): String = when (this) {
        is WidgetSource.Today -> SOURCE_TODAY
        is WidgetSource.Upcoming7d -> SOURCE_UPCOMING
        is WidgetSource.Pinned -> SOURCE_PINNED
        is WidgetSource.Tag -> SOURCE_TAG
    }

    private fun sourceKey(appWidgetId: Int) = stringPreferencesKey("widget_${appWidgetId}_source")

    private fun tagKey(appWidgetId: Int) = stringPreferencesKey("widget_${appWidgetId}_tag")

    companion object {
        internal const val SOURCE_TODAY = "today"
        internal const val SOURCE_UPCOMING = "upcoming"
        internal const val SOURCE_PINNED = "pinned"
        internal const val SOURCE_TAG = "tag"
    }
}
