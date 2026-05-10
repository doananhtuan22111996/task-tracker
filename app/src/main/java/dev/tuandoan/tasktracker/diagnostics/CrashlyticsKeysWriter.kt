package dev.tuandoan.tasktracker.diagnostics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.tuandoan.tasktracker.BuildConfig
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes the 6 Crashlytics custom keys that give a crash report enough context
 * to debug user-facing issues without collecting task content (v1.12.0, FB-10).
 *
 * **When this runs:**
 *  - `PrivacyManager.setEnabled(true)` — user flipped the Diagnostics toggle on.
 *  - `PrivacyManager.initCollectionState(true)` — cold-start gate, launched async
 *    on the diagnostics coroutine scope so the main thread isn't blocked.
 *
 * **Why a separate class:**
 *  - Keeps `PrivacyManager`'s SDK-toggle concern separate from the key-assembly
 *    concern. Tests can verify key values without mocking all three Firebase SDKs.
 *  - When FB-11 adds the opt-out cleanup (clear all custom keys), it extends
 *    this class with a `clearAll()` method rather than bloating `PrivacyManager`.
 *
 * **Why the keys are what they are** (per ADR-003 + PRD):
 *  - `locale` — per-app locale affects which strings + formatting reach the user;
 *    a locale-specific rendering bug needs this to be reproducible.
 *  - `theme`, `dynamic_color` — the biggest source of visual bugs; light/dark
 *    and Material You palette shifts change rendering significantly.
 *  - `task_count_bucket` — not the raw count (PII risk at the extremes); a coarse
 *    bucket that distinguishes "empty state" from "power user" crashes.
 *  - `calendar_tab_active` — snapshot at opt-in. See [writeAll] for the caveat;
 *    without a nav observer this is effectively a \"was the user on calendar
 *    when they opted in\" flag.
 *  - `app_version_name` — redundant with Crashlytics' own version metadata but
 *    cheap insurance if the SDK's version field is ever stale vs the APK.
 *
 * **Error handling:** every throwable inside [writeAll] is caught and swallowed.
 * A missing preference or a DataStore IO failure must not take the app down —
 * diagnostics keys are nice-to-have context, not load-bearing.
 */
@Singleton
class CrashlyticsKeysWriter @Inject constructor(
    private val crashlytics: FirebaseCrashlytics,
    private val settingsRepository: SettingsRepository,
    private val taskRepository: ITaskRepository,
) {

    /**
     * Snapshot the current user/app state and write all 6 keys to Crashlytics.
     *
     * `calendar_tab_active` is intentionally `false` here because this snapshot
     * runs at opt-in time, not at tab-change time. Making it reflect live tab
     * state requires a nav observer that's out of scope for FB-10. If that
     * becomes load-bearing for debugging, promote to a follow-up ticket.
     *
     * Safe to call when Crashlytics is disabled — `setCustomKey` is a no-op in
     * that state per the SDK contract. We still skip the work on opt-out paths
     * at the caller level ([PrivacyManager]) to avoid reading DataStore for
     * nothing.
     */
    suspend fun writeAll() {
        try {
            val prefs = settingsRepository.userPreferences.first()
            val activeTaskCount = taskRepository.observeActiveCount().first()

            crashlytics.setCustomKey(LOCALE, Locale.getDefault().toLanguageTag())
            crashlytics.setCustomKey(THEME, prefs.themeMode.name)
            crashlytics.setCustomKey(DYNAMIC_COLOR, prefs.dynamicColor)
            crashlytics.setCustomKey(TASK_COUNT_BUCKET, bucketTaskCount(activeTaskCount))
            crashlytics.setCustomKey(CALENDAR_TAB_ACTIVE, false)
            crashlytics.setCustomKey(APP_VERSION_NAME, BuildConfig.VERSION_NAME)
        } catch (t: Throwable) {
            // Diagnostics-key-writing failing must never take the app down. A
            // corrupt DataStore or unavailable DB should result in crashes that
            // simply don't have these keys attached, not in a second-order
            // crash from the key-writing path itself.
        }
    }

    /**
     * Bucket the raw active-task count so the value we send is coarse enough to
     * not identify individual users at the extremes (e.g. a single user with
     * 10,000 tasks). Ranges chosen to match typical-usage distribution: most
     * users stay in `1-9` or `10-49`, power users in `50-199`, outliers in `200+`.
     */
    internal fun bucketTaskCount(count: Int): String = when {
        count <= 0 -> "0"
        count < 10 -> "1-9"
        count < 50 -> "10-49"
        count < 200 -> "50-199"
        else -> "200+"
    }

    companion object {
        const val LOCALE = "locale"
        const val THEME = "theme"
        const val DYNAMIC_COLOR = "dynamic_color"
        const val TASK_COUNT_BUCKET = "task_count_bucket"
        const val CALENDAR_TAB_ACTIVE = "calendar_tab_active"
        const val APP_VERSION_NAME = "app_version_name"
    }
}
