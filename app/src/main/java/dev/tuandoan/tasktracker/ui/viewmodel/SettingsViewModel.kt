package dev.tuandoan.tasktracker.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.preferences.PrivacyRepository
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.data.preferences.ThemeMode
import dev.tuandoan.tasktracker.data.preferences.UserPreferences
import dev.tuandoan.tasktracker.diagnostics.PrivacyManager
import dev.tuandoan.tasktracker.domain.backup.ExportBackupUseCase
import dev.tuandoan.tasktracker.domain.backup.ImportBackupUseCase
import dev.tuandoan.tasktracker.domain.backup.model.BackupFormat
import dev.tuandoan.tasktracker.domain.backup.model.ExportResult
import dev.tuandoan.tasktracker.domain.backup.model.ImportResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the Settings screen, managing backup/restore operations,
 * theme preferences, dynamic color, and per-app language selection.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val settingsRepository: SettingsRepository,
    private val privacyRepository: PrivacyRepository,
    private val privacyManager: PrivacyManager,
) : ViewModel() {

    // --- User Preferences ---

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    /**
     * Updates the theme mode preference.
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /**
     * Updates the dynamic color preference.
     */
    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    /**
     * Updates the language tag preference and applies the locale change.
     * An empty tag means "follow system".
     */
    fun setLanguageTag(tag: String) {
        viewModelScope.launch {
            settingsRepository.setLanguageTag(tag)
            applyLocale(tag)
        }
    }

    /**
     * Applies the given locale tag using AppCompatDelegate.
     * This works on all API levels via the AndroidX compatibility layer.
     */
    fun applyLocale(tag: String) {
        val locales = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /**
     * Returns the list of supported locales as (tag, displayName) pairs.
     * The display name is shown in the locale's own language for clarity.
     */
    fun getSupportedLocales(): List<Pair<String, String>> {
        val supportedTags = listOf("de", "en", "es", "fr", "hi", "in", "pt", "vi")
        return supportedTags.map { tag ->
            val locale = Locale.forLanguageTag(tag)
            val displayName = locale.getDisplayName(locale)
                .replaceFirstChar { it.titlecase(locale) }
            tag to displayName
        }
    }

    // --- Backup & Restore ---

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showImportConfirmation = MutableStateFlow(false)
    val showImportConfirmation: StateFlow<Boolean> = _showImportConfirmation.asStateFlow()

    private val _showErrorDialog = MutableStateFlow<String?>(null)
    val showErrorDialog: StateFlow<String?> = _showErrorDialog.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var pendingImportUri: Uri? = null

    /**
     * Exports all tasks to the given URI in the specified format.
     */
    fun exportBackup(uri: Uri, format: BackupFormat, appVersion: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = exportBackupUseCase.execute(uri, format, appVersion)
            _isLoading.value = false

            when (result) {
                is ExportResult.Success -> {
                    _snackbarMessage.emit(context.getString(R.string.snackbar_exported_tasks, result.taskCount))
                }
                is ExportResult.Error -> {
                    _showErrorDialog.value = result.message
                }
            }
        }
    }

    /**
     * Stores the import URI and shows the confirmation dialog.
     */
    fun requestImport(uri: Uri) {
        pendingImportUri = uri
        _showImportConfirmation.value = true
    }

    /**
     * Confirms the import and replaces all existing tasks with the backup data.
     */
    fun confirmImport() {
        val uri = pendingImportUri ?: return
        _showImportConfirmation.value = false

        viewModelScope.launch {
            _isLoading.value = true
            val result = importBackupUseCase.execute(uri)
            _isLoading.value = false
            pendingImportUri = null

            when (result) {
                is ImportResult.Success -> {
                    val message = buildString {
                        append(context.getString(R.string.snackbar_imported_tasks, result.importedCount))
                        if (result.skippedCount > 0) {
                            append(context.getString(R.string.snackbar_imported_skipped, result.skippedCount))
                        }
                    }
                    _snackbarMessage.emit(message)
                }
                is ImportResult.Error -> {
                    _showErrorDialog.value = result.message
                }
            }
        }
    }

    /**
     * Cancels the pending import operation.
     */
    fun cancelImport() {
        _showImportConfirmation.value = false
        pendingImportUri = null
    }

    /**
     * Dismisses the error dialog.
     */
    fun dismissError() {
        _showErrorDialog.value = null
    }

    // --- Onboarding ---

    fun setOnboardingCompleted() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted() }
    }

    // --- Feature Tips ---

    fun setTipFabShown() {
        viewModelScope.launch { settingsRepository.setTipShown(SettingsRepository.TipKeys.FAB) }
    }

    fun setTipTagChipsShown() {
        viewModelScope.launch { settingsRepository.setTipShown(SettingsRepository.TipKeys.TAG_CHIPS) }
    }

    fun setTipStatsCardsShown() {
        viewModelScope.launch { settingsRepository.setTipShown(SettingsRepository.TipKeys.STATS_CARDS) }
    }

    // --- Privacy (FB-07) ---

    /**
     * Reactive view of the diagnostics opt-in flag for the Settings → Privacy switch.
     * Initial value is `false` — the structural opt-out default from ADR-003 / FB-04.
     */
    val diagnosticsOptIn: StateFlow<Boolean> = privacyRepository.diagnosticsOptIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * User flipped the diagnostics toggle. Routes through [PrivacyManager.setEnabled] so
     * the DataStore write AND the three Firebase SDK collection flags flip atomically.
     * On disable, [PrivacyManager] also calls `Crashlytics.deleteUnsentReports()` to
     * discard queued payloads (best-effort per the SDK contract).
     */
    fun setDiagnosticsOptIn(optIn: Boolean) {
        viewModelScope.launch { privacyManager.setEnabled(optIn) }
    }
}
