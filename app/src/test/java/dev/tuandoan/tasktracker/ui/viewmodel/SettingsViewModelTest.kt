package dev.tuandoan.tasktracker.ui.viewmodel

import android.content.Context
import app.cash.turbine.test
import dev.tuandoan.tasktracker.data.preferences.PrivacyRepository
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.data.preferences.ThemeMode
import dev.tuandoan.tasktracker.data.preferences.UserPreferences
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbCategory
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbLogger
import dev.tuandoan.tasktracker.diagnostics.PrivacyManager
import dev.tuandoan.tasktracker.domain.backup.ExportBackupUseCase
import dev.tuandoan.tasktracker.domain.backup.ImportBackupUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var exportBackupUseCase: ExportBackupUseCase
    private lateinit var importBackupUseCase: ImportBackupUseCase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var privacyManager: PrivacyManager
    private lateinit var breadcrumbLogger: BreadcrumbLogger
    private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>
    private lateinit var diagnosticsOptInFlow: MutableStateFlow<Boolean>
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        exportBackupUseCase = mockk(relaxed = true)
        importBackupUseCase = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        privacyRepository = mockk(relaxed = true)
        privacyManager = mockk(relaxed = true)
        breadcrumbLogger = mockk(relaxed = true)
        preferencesFlow = MutableStateFlow(UserPreferences())
        diagnosticsOptInFlow = MutableStateFlow(false)

        every { settingsRepository.userPreferences } returns preferencesFlow
        every { privacyRepository.diagnosticsOptIn } returns diagnosticsOptInFlow

        viewModel = SettingsViewModel(
            context = context,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase,
            settingsRepository = settingsRepository,
            privacyRepository = privacyRepository,
            privacyManager = privacyManager,
            breadcrumbLogger = breadcrumbLogger,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- UserPreferences StateFlow ---

    @Test
    fun `userPreferences initial value is default UserPreferences`() = runTest {
        val prefs = viewModel.userPreferences.value
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        assertEquals(true, prefs.dynamicColor)
        assertEquals("", prefs.languageTag)
    }

    @Test
    fun `userPreferences reflects updates from repository flow`() = runTest {
        viewModel.userPreferences.test {
            // Initial value
            val initial = awaitItem()
            assertEquals(ThemeMode.SYSTEM, initial.themeMode)

            // Simulate repository emitting new preferences
            preferencesFlow.value = UserPreferences(
                themeMode = ThemeMode.DARK,
                dynamicColor = false,
                languageTag = "vi",
            )

            val updated = awaitItem()
            assertEquals(ThemeMode.DARK, updated.themeMode)
            assertEquals(false, updated.dynamicColor)
            assertEquals("vi", updated.languageTag)
        }
    }

    // --- setThemeMode ---

    @Test
    fun `setThemeMode delegates to repository`() = runTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        coVerify { settingsRepository.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `setThemeMode LIGHT delegates to repository`() = runTest {
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        coVerify { settingsRepository.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `setThemeMode SYSTEM delegates to repository`() = runTest {
        viewModel.setThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()

        coVerify { settingsRepository.setThemeMode(ThemeMode.SYSTEM) }
    }

    // --- setDynamicColor ---

    @Test
    fun `setDynamicColor true delegates to repository`() = runTest {
        viewModel.setDynamicColor(true)
        advanceUntilIdle()

        coVerify { settingsRepository.setDynamicColor(true) }
    }

    @Test
    fun `setDynamicColor false delegates to repository`() = runTest {
        viewModel.setDynamicColor(false)
        advanceUntilIdle()

        coVerify { settingsRepository.setDynamicColor(false) }
    }

    // --- setLanguageTag ---

    @Test
    fun `setLanguageTag delegates to repository and applies locale`() = runTest {
        // Mock the static AppCompatDelegate call
        mockkStatic("androidx.appcompat.app.AppCompatDelegate")

        viewModel.setLanguageTag("vi")
        advanceUntilIdle()

        coVerify { settingsRepository.setLanguageTag("vi") }

        unmockkStatic("androidx.appcompat.app.AppCompatDelegate")
    }

    @Test
    fun `setLanguageTag with empty string delegates to repository`() = runTest {
        mockkStatic("androidx.appcompat.app.AppCompatDelegate")

        viewModel.setLanguageTag("")
        advanceUntilIdle()

        coVerify { settingsRepository.setLanguageTag("") }

        unmockkStatic("androidx.appcompat.app.AppCompatDelegate")
    }

    // --- getSupportedLocales ---

    @Test
    fun `getSupportedLocales returns correct locale list`() {
        val locales = viewModel.getSupportedLocales()

        assertEquals(8, locales.size)
        val allTags = locales.map { it.first }.toSet()
        assertEquals(setOf("de", "en", "es", "fr", "hi", "in", "pt", "vi"), allTags)
    }

    @Test
    fun `getSupportedLocales English display name starts with uppercase`() {
        val locales = viewModel.getSupportedLocales()
        val englishDisplayName = locales.first { it.first == "en" }.second

        assertTrue(
            "English display name should start with uppercase: $englishDisplayName",
            englishDisplayName[0].isUpperCase(),
        )
    }

    @Test
    fun `getSupportedLocales Vietnamese display name starts with uppercase`() {
        val locales = viewModel.getSupportedLocales()
        val vietnameseDisplayName = locales.first { it.first == "vi" }.second

        assertTrue(
            "Vietnamese display name should start with uppercase: $vietnameseDisplayName",
            vietnameseDisplayName[0].isUpperCase(),
        )
    }

    // --- Backup & Restore (existing behavior) ---

    @Test
    fun `initial isLoading is false`() {
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `initial showImportConfirmation is false`() {
        assertEquals(false, viewModel.showImportConfirmation.value)
    }

    @Test
    fun `initial showErrorDialog is null`() {
        assertEquals(null, viewModel.showErrorDialog.value)
    }

    @Test
    fun `requestImport sets showImportConfirmation to true`() {
        val uri = mockk<android.net.Uri>()
        viewModel.requestImport(uri)

        assertEquals(true, viewModel.showImportConfirmation.value)
    }

    @Test
    fun `cancelImport resets showImportConfirmation`() {
        val uri = mockk<android.net.Uri>()
        viewModel.requestImport(uri)
        viewModel.cancelImport()

        assertEquals(false, viewModel.showImportConfirmation.value)
    }

    @Test
    fun `dismissError resets showErrorDialog`() {
        // Access private field indirectly by checking the flow
        viewModel.dismissError()
        assertEquals(null, viewModel.showErrorDialog.value)
    }

    // --- Privacy (FB-07) ---

    @Test
    fun `diagnosticsOptIn initial value is false`() = runTest {
        // Structural opt-out guarantee: a user who has never opted in must see the
        // switch as off on first render. Matches PrivacyRepository's DataStore default.
        assertEquals(false, viewModel.diagnosticsOptIn.value)
    }

    @Test
    fun `diagnosticsOptIn reflects repository updates`() = runTest {
        viewModel.diagnosticsOptIn.test {
            assertEquals(false, awaitItem())

            diagnosticsOptInFlow.value = true
            advanceUntilIdle()
            assertEquals(true, awaitItem())

            diagnosticsOptInFlow.value = false
            advanceUntilIdle()
            assertEquals(false, awaitItem())

            cancel()
        }
    }

    @Test
    fun `setDiagnosticsOptIn true delegates to PrivacyManager`() = runTest {
        // Handler routes through PrivacyManager.setEnabled (NOT PrivacyRepository.set...)
        // because setEnabled is the path that persists AND fans out to the three Firebase
        // SDKs atomically per FB-05's contract.
        viewModel.setDiagnosticsOptIn(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { privacyManager.setEnabled(true) }
    }

    @Test
    fun `setDiagnosticsOptIn false delegates to PrivacyManager`() = runTest {
        // Symmetric disable-path check. On disable, PrivacyManager also calls
        // Crashlytics.deleteUnsentReports() per FB-05 — the VM doesn't need to know.
        viewModel.setDiagnosticsOptIn(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { privacyManager.setEnabled(false) }
    }

    // === FB-12: SETTINGS breadcrumbs — enum constants and booleans only, never raw user input ===

    @Test
    fun `setThemeMode logs SETTINGS breadcrumb with enum constant name`() = runTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        verify { breadcrumbLogger.log(BreadcrumbCategory.SETTINGS, "theme=DARK") }
    }

    @Test
    fun `setDynamicColor logs SETTINGS breadcrumb with boolean only`() = runTest {
        viewModel.setDynamicColor(false)
        verify { breadcrumbLogger.log(BreadcrumbCategory.SETTINGS, "dynamic_color=false") }
    }

    @Test
    fun `setLanguageTag logs SETTINGS breadcrumb with BCP-47 tag only`() = runTest {
        mockkStatic("androidx.appcompat.app.AppCompatDelegate")
        viewModel.setLanguageTag("vi")
        verify { breadcrumbLogger.log(BreadcrumbCategory.SETTINGS, "locale=vi") }
        unmockkStatic("androidx.appcompat.app.AppCompatDelegate")
    }

    @Test
    fun `setDiagnosticsOptIn logs SETTINGS breadcrumb with boolean`() = runTest {
        viewModel.setDiagnosticsOptIn(true)
        verify { breadcrumbLogger.log(BreadcrumbCategory.SETTINGS, "diagnostics=true") }
    }
}
