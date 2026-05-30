package dev.tuandoan.tasktracker.widget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.data.preferences.WidgetConfigurationRepository
import dev.tuandoan.tasktracker.domain.model.TagItem
import dev.tuandoan.tasktracker.domain.service.TagNormalizer
import dev.tuandoan.tasktracker.domain.usecase.TagManagementUseCase
import dev.tuandoan.tasktracker.widget.model.WidgetSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WidgetConfigureUiState(val selection: WidgetSource = WidgetSource.Today, val isSaving: Boolean = false)

@HiltViewModel
class WidgetConfigureViewModel @Inject constructor(
    private val configRepository: WidgetConfigurationRepository,
    tagManagementUseCase: TagManagementUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WidgetConfigureUiState())
    val uiState: StateFlow<WidgetConfigureUiState> = _uiState.asStateFlow()

    private val _navigateDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateDone: SharedFlow<Unit> = _navigateDone.asSharedFlow()

    val tags: StateFlow<List<TagItem>> = tagManagementUseCase.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSelection(source: WidgetSource) {
        _uiState.update { it.copy(selection = source) }
    }

    /**
     * Persists the selected source for [appWidgetId] and emits on [navigateDone].
     *
     * Selection is captured before the coroutine starts to avoid a TOCTOU where
     * the user taps a different radio button between `isSaving=true` and the read.
     * `isSaving` is always cleared in `finally` so a DataStore `IOException` never
     * leaves the Confirm button permanently disabled.
     */
    fun confirm(appWidgetId: Int) {
        val source = _uiState.value.selection
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val finalSource = if (source is WidgetSource.Tag) {
                    val normalized = TagNormalizer.normalize(source.name)
                    if (normalized != null) WidgetSource.Tag(normalized) else WidgetSource.Today
                } else {
                    source
                }
                configRepository.setSource(appWidgetId, finalSource)
                _navigateDone.tryEmit(Unit)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
