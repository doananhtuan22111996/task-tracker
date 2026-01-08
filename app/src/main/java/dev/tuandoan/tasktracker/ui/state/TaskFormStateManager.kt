package dev.tuandoan.tasktracker.ui.state

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * State manager for task form operations including dialog state, validation, and form fields.
 * Encapsulates form-related business logic and state management.
 */
class TaskFormStateManager @Inject constructor(
    private val formUseCase: TaskFormUseCase
) {

    /**
     * Initialize form state flows for a given coroutine scope
     */
    fun initializeStateFlows(scope: CoroutineScope): TaskFormState {
        val isFormValid = formUseCase.isFormValid()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

        val isEditMode = formUseCase.isEditMode()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

        return TaskFormState(
            showAddTaskDialog = formUseCase.showAddTaskDialog,
            selectedTask = formUseCase.selectedTask,
            taskTitle = formUseCase.taskTitle,
            taskDescription = formUseCase.taskDescription,
            isFormValid = isFormValid,
            isEditMode = isEditMode,
            errorMessage = formUseCase.errorMessage
        )
    }

    // === Form Dialog Management ===
    fun showAddTaskDialog() = formUseCase.showAddTaskDialog()
    fun showEditTaskDialog(task: Task) = formUseCase.showEditTaskDialog(task)
    fun hideAddTaskDialog() = formUseCase.hideAddTaskDialog()

    // === Form Field Management ===
    fun updateTaskTitle(title: String) = formUseCase.updateTaskTitle(title)
    fun updateTaskDescription(description: String) = formUseCase.updateTaskDescription(description)

    // === Form Validation ===
    suspend fun validateForm(): FormValidationResult {
        val formData = formUseCase.getFormData().first()

        return when {
            formData.title.isBlank() -> FormValidationResult.Error("Title cannot be empty")
            formData.selectedTaskId != null && formData.selectedTaskId <= 0 -> {
                FormValidationResult.Error("Invalid task selected for update")
            }
            else -> FormValidationResult.Success(formData)
        }
    }

    // === Error Management ===
    fun setError(message: String) = formUseCase.setError(message)
    fun clearError() = formUseCase.clearError()

    /**
     * Get current form data
     */
    suspend fun getFormData() = formUseCase.getFormData().first()

    /**
     * Reset form to initial state
     */
    fun resetForm() {
        formUseCase.hideAddTaskDialog()
        formUseCase.clearError()
    }
}

/**
 * Data class containing all form related state
 */
data class TaskFormState(
    val showAddTaskDialog: StateFlow<Boolean>,
    val selectedTask: StateFlow<Task?>,
    val taskTitle: StateFlow<String>,
    val taskDescription: StateFlow<String>,
    val isFormValid: StateFlow<Boolean>,
    val isEditMode: StateFlow<Boolean>,
    val errorMessage: StateFlow<String?>
)

/**
 * Sealed class representing form validation results
 */
sealed class FormValidationResult {
    data class Success(val formData: TaskFormUseCase.FormData) : FormValidationResult()
    data class Error(val message: String) : FormValidationResult()
}