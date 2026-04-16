package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.domain.model.TagItem
import dev.tuandoan.tasktracker.domain.usecase.TagManagementUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagManagementViewModel @Inject constructor(private val tagManagementUseCase: TagManagementUseCase) :
    ViewModel() {

    val tags: StateFlow<List<TagItem>> = tagManagementUseCase.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    fun renameTag(oldName: String, newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            tagManagementUseCase.renameTag(oldName, newName)
                .onFailure { _snackbarMessage.emit(it.message ?: "Failed to rename tag") }
            _isLoading.value = false
        }
    }

    fun deleteTag(tagName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            tagManagementUseCase.deleteTag(tagName)
                .onSuccess { _snackbarMessage.emit("Tag \"$tagName\" deleted") }
                .onFailure { _snackbarMessage.emit(it.message ?: "Failed to delete tag") }
            _isLoading.value = false
        }
    }

    fun updateTagColor(tagName: String, color: String?) {
        viewModelScope.launch {
            tagManagementUseCase.updateTagColor(tagName, color)
                .onFailure { _snackbarMessage.emit(it.message ?: "Failed to update color") }
        }
    }
}
