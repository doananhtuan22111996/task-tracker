package dev.tuandoan.tasktracker.diagnostics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.tuandoan.tasktracker.BuildConfig
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.data.preferences.ThemeMode
import dev.tuandoan.tasktracker.data.preferences.UserPreferences
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Contract test for [CrashlyticsKeysWriter] (FB-10).
 *
 * Verifies the 6 Crashlytics custom keys are written correctly on [writeAll],
 * the bucketing math is right, and that a failure in any collaborator is
 * swallowed rather than propagating to the caller.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CrashlyticsKeysWriterTest {

    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var taskRepository: ITaskRepository
    private lateinit var writer: CrashlyticsKeysWriter

    private lateinit var prefsFlow: MutableStateFlow<UserPreferences>
    private lateinit var activeCountFlow: MutableStateFlow<Int>

    @Before
    fun setUp() {
        crashlytics = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)

        prefsFlow = MutableStateFlow(UserPreferences())
        activeCountFlow = MutableStateFlow(0)
        every { settingsRepository.userPreferences } returns prefsFlow
        every { taskRepository.observeActiveCount() } returns activeCountFlow

        writer = CrashlyticsKeysWriter(crashlytics, settingsRepository, taskRepository)
    }

    // ── 6 keys written on writeAll ───────────────────────────────────────────

    @Test
    fun `writeAll writes locale from Locale getDefault`() = runTest(UnconfinedTestDispatcher()) {
        writer.writeAll()
        val expected = Locale.getDefault().toLanguageTag()
        verify { crashlytics.setCustomKey(CrashlyticsKeysWriter.LOCALE, expected) }
    }

    @Test
    fun `writeAll writes theme from SettingsRepository`() = runTest(UnconfinedTestDispatcher()) {
        prefsFlow.value = UserPreferences(themeMode = ThemeMode.DARK)
        writer.writeAll()
        verify { crashlytics.setCustomKey(CrashlyticsKeysWriter.THEME, "DARK") }
    }

    @Test
    fun `writeAll writes dynamic_color from SettingsRepository`() = runTest(UnconfinedTestDispatcher()) {
        prefsFlow.value = UserPreferences(dynamicColor = false)
        writer.writeAll()
        verify { crashlytics.setCustomKey(CrashlyticsKeysWriter.DYNAMIC_COLOR, false) }
    }

    @Test
    fun `writeAll writes task_count_bucket from TaskRepository`() = runTest(UnconfinedTestDispatcher()) {
        activeCountFlow.value = 42
        writer.writeAll()
        verify { crashlytics.setCustomKey(CrashlyticsKeysWriter.TASK_COUNT_BUCKET, "10-49") }
    }

    @Test
    fun `writeAll writes calendar_tab_active as false snapshot`() = runTest(UnconfinedTestDispatcher()) {
        // Snapshot semantics: at opt-in the tab state isn't tracked dynamically.
        // See `CrashlyticsKeysWriter.writeAll` KDoc for the follow-up-ticket note.
        writer.writeAll()
        verify { crashlytics.setCustomKey(CrashlyticsKeysWriter.CALENDAR_TAB_ACTIVE, false) }
    }

    @Test
    fun `writeAll writes app_version_name from BuildConfig`() = runTest(UnconfinedTestDispatcher()) {
        writer.writeAll()
        verify { crashlytics.setCustomKey(CrashlyticsKeysWriter.APP_VERSION_NAME, BuildConfig.VERSION_NAME) }
    }

    // ── bucketTaskCount ──────────────────────────────────────────────────────

    @Test
    fun `bucketTaskCount zero or negative returns 0`() {
        assertEquals("0", writer.bucketTaskCount(0))
        assertEquals("0", writer.bucketTaskCount(-1))
    }

    @Test
    fun `bucketTaskCount small single digit returns 1-9`() {
        assertEquals("1-9", writer.bucketTaskCount(1))
        assertEquals("1-9", writer.bucketTaskCount(9))
    }

    @Test
    fun `bucketTaskCount ten to forty-nine returns 10-49`() {
        assertEquals("10-49", writer.bucketTaskCount(10))
        assertEquals("10-49", writer.bucketTaskCount(49))
    }

    @Test
    fun `bucketTaskCount fifty to one-ninety-nine returns 50-199`() {
        assertEquals("50-199", writer.bucketTaskCount(50))
        assertEquals("50-199", writer.bucketTaskCount(199))
    }

    @Test
    fun `bucketTaskCount two hundred or more returns 200plus`() {
        assertEquals("200+", writer.bucketTaskCount(200))
        assertEquals("200+", writer.bucketTaskCount(100_000))
    }

    // ── error swallowing ─────────────────────────────────────────────────────

    @Test
    fun `writeAll swallows throwables from DataStore and does not propagate`() = runTest(UnconfinedTestDispatcher()) {
        // If SettingsRepository.userPreferences itself throws, writeAll must not
        // take the app down. Diagnostics failing to annotate a crash is
        // strictly worse than a silent failure — but a crash from our key
        // path would be catastrophic for a user who just opted in.
        every { settingsRepository.userPreferences } returns flow {
            throw RuntimeException("simulated DataStore bug")
        }
        writer.writeAll()
        // Reaching this point = no exception escaped. Also verify we didn't
        // write any partial key set after the throw.
        coVerify(exactly = 0) {
            crashlytics.setCustomKey(any<String>(), any<String>())
        }
    }
}
