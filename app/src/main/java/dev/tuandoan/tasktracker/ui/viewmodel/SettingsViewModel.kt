package dev.tuandoan.tasktracker.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.domain.backup.ExportBackupUseCase
import dev.tuandoan.tasktracker.domain.backup.ImportBackupUseCase
import dev.tuandoan.tasktracker.domain.backup.model.BackupFormat
import dev.tuandoan.tasktracker.domain.backup.model.ExportResult
import dev.tuandoan.tasktracker.domain.backup.model.ImportResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen, managing backup and restore operations.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
) : ViewModel() {

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
                    _snackbarMessage.emit("Exported ${result.taskCount} tasks successfully")
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
                        append("Imported ${result.importedCount} tasks successfully")
                        if (result.skippedCount > 0) {
                            append(" (${result.skippedCount} skipped)")
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
}
