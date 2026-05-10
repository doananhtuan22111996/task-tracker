package dev.tuandoan.tasktracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated DataStore for the Firebase consent gate (v1.12.0, FB-04).
 *
 * Why separate from [SettingsRepository]:
 *  - **Audit clarity.** The `privacy.preferences_pb` file on disk is self-documenting —
 *    a GDPR inquiry can point at one file to show where consent is stored, rather than
 *    grep a mixed-purpose settings file.
 *  - **Pre-Hilt read.** [readOptInOnce] must work in `TaskTrackerApplication.onCreate`
 *    before the Hilt graph is ready. An independent `preferencesDataStore` delegate keeps
 *    that path simple and testable.
 *  - **Future granularity.** If v1.13.0 adds per-product toggles (Crashlytics-only, Analytics-only),
 *    new keys land here without touching the existing settings schema.
 *
 * Default value is `false` — the structural opt-out guarantee from ADR-003.
 */
private val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy")

@Singleton
class PrivacyRepository internal constructor(private val dataStore: DataStore<Preferences>) {

    /**
     * Hilt-injected constructor used in production. The `internal` primary constructor
     * above accepts any `DataStore<Preferences>`, which lets JVM tests inject an
     * in-memory or temp-file-backed store without requiring Robolectric.
     */
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.privacyDataStore)

    /**
     * Observes the diagnostics opt-in flag reactively. Defaults to `false` when the key
     * hasn't been written yet (fresh install). `distinctUntilChanged` so downstream
     * consumers (e.g. `PrivacyManager` fan-out to the three Firebase SDKs) don't re-run
     * on unrelated DataStore writes.
     */
    val diagnosticsOptIn: Flow<Boolean> = dataStore.data
        .catch { cause ->
            // DataStore's official recovery pattern: IOException on read (corrupt file,
            // disk full, filesystem permission drift) must degrade gracefully, not crash
            // the collector. Falling back to emptyPreferences() makes the downstream
            // `map` return `false` — the safe default per ADR-003's structural opt-out.
            // Non-IO throwables (e.g. a key-type mismatch) are real bugs; rethrow so we
            // surface them rather than silently masking.
            if (cause is IOException) {
                emit(emptyPreferences())
            } else {
                throw cause
            }
        }
        .map { prefs -> prefs[DIAGNOSTICS_OPT_IN] ?: false }
        .distinctUntilChanged()

    /**
     * Persists the opt-in flag. Idempotent: writing the same value is cheap.
     * Callers are expected to also call `PrivacyManager.applyToFirebase(optIn)` so the
     * SDK runtime state stays in sync with disk — that wiring lives in FB-05.
     */
    suspend fun setDiagnosticsOptIn(optIn: Boolean) {
        dataStore.edit { prefs ->
            prefs[DIAGNOSTICS_OPT_IN] = optIn
        }
    }

    /**
     * One-shot read of the current opt-in value. Primarily for tests; production runtime
     * callers should collect [diagnosticsOptIn] to stay reactive. Pre-Hilt startup path
     * uses [Companion.readOptInOnce] instead since it doesn't need the Hilt-bound Context.
     */
    suspend fun getDiagnosticsOptInOnce(): Boolean = diagnosticsOptIn.first()

    companion object {
        internal val DIAGNOSTICS_OPT_IN = booleanPreferencesKey("diagnostics_opt_in")

        /**
         * Pre-Hilt synchronous read for `TaskTrackerApplication.onCreate` (FB-06).
         *
         * `runBlocking` on the main thread is accepted per ADR-003: reading a single boolean
         * from a dedicated DataStore file costs ~5–15 ms on P50 hardware, well inside the
         * +100 ms opted-out cold-start budget. The read must complete before Firebase's
         * auto-init ContentProvider fires any collection call, so the blocking cost is the
         * price of the structural opt-out guarantee.
         *
         * If cold-start regression ever exceeds the budget, Option B in ADR-003
         * (`@EntryPoint`-based async init on `Dispatchers.Default`) is the escape hatch.
         */
        fun readOptInOnce(context: Context): Boolean = runBlocking {
            context.privacyDataStore.data
                .catch { cause ->
                    // Same recovery contract as the [diagnosticsOptIn] Flow — an IOException
                    // here would otherwise propagate up the `runBlocking` and crash
                    // `Application.onCreate` before any other startup code runs. Fall back
                    // to the opt-out default (safe per ADR-003); let non-IO bugs surface.
                    if (cause is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw cause
                    }
                }
                .map { it[DIAGNOSTICS_OPT_IN] ?: false }
                .first()
        }
    }
}
