package dev.tuandoan.tasktracker.widget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.data.preferences.WidgetConfigurationRepository
import dev.tuandoan.tasktracker.domain.model.TagItem
import dev.tuandoan.tasktracker.domain.service.TagNormalizer
import dev.tuandoan.tasktracker.domain.usecase.TagManagementUseCase
import dev.tuandoan.tasktracker.widget.model.WidgetSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val tags: StateFlow<List<TagItem>> = tagManagementUseCase.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSelection(source: WidgetSource) {
        _uiState.update { it.copy(selection = source) }
    }

    /**
     * Persists the selected source for [appWidgetId] and signals completion via [onDone].
     * [onDone] is called on the main thread inside `viewModelScope`; the caller sets
     * `RESULT_OK` and finishes the activity.
     */
    fun confirm(appWidgetId: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val source = _uiState.value.selection
            // Normalize tag name defensively; blank tags are rejected by the repository too.
            val finalSource = if (source is WidgetSource.Tag) {
                val normalized = TagNormalizer.normalize(source.name)
                if (normalized != null) WidgetSource.Tag(normalized) else WidgetSource.Today
            } else {
                source
            }
            configRepository.setSource(appWidgetId, finalSource)
            _uiState.update { it.copy(isSaving = false) }
            onDone()
        }
    }
}
