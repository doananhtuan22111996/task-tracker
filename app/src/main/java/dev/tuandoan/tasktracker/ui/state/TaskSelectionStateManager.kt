package dev.tuandoan.tasktracker.ui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages task selection state for multi-select functionality in the Task Tracker application.
 *
 * ## Responsibilities
 * - Track selected task IDs using reactive StateFlow
 * - Provide derived selection state (count, mode, validation)
 * - Handle selection operations (enter, toggle, clear, select all)
 * - Validate selection operations with comprehensive error handling
 * - Enforce reasonable limits for UI performance and usability
 *
 * ## Usage
 * This manager is designed to be used as a singleton dependency that coordinates
 * selection state across the application. It provides both imperative methods
 * for state changes and reactive StateFlow properties for UI observation.
 *
 * ## Thread Safety
 * This manager is thread-safe and can be called from any coroutine context.
 * All StateFlow operations are atomic and properly synchronized.
 *
 * ## Performance Considerations
 * - Maximum selection size is limited to [MAX_SELECTION_SIZE] for UI performance
 * - StateFlow emissions are optimized to prevent unnecessary recompositions
 * - Validation is performed eagerly to catch errors early
 *
 * @since 2.2.0
 * @see SelectionState
 * @see SelectionValidationResult
 */
@Singleton
class TaskSelectionStateManager @Inject constructor() {

    // === Constants ===

    companion object {
        private const val MAX_SELECTION_SIZE = 1000 // Reasonable limit for UI performance
    }

    // === Private State ===

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    // === Public State Flows ===

    /**
     * Set of currently selected task IDs
     */
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    /**
     * Initialize derived state flows for a given coroutine scope.
     *
     * This method creates derived StateFlow properties that automatically update
     * based on the selection state. The StateFlows are optimized with proper
     * sharing strategy to prevent memory leaks and unnecessary computations.
     *
     * @param scope The coroutine scope to use for StateFlow lifecycle management.
     *              Should typically be the ViewModel's viewModelScope.
     * @return A [SelectionState] containing reactive properties for UI observation
     *
     * ## StateFlow Properties
     * - **isSelectionMode**: True when any tasks are selected
     * - **selectedCount**: Number of currently selected tasks
     * - **selectedIds**: Set of selected task IDs (source of truth)
     *
     * @see SelectionState
     */
    fun initializeStateFlows(scope: CoroutineScope): SelectionState {
        val isSelectionMode = selectedIds.map { it.isNotEmpty() }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

        val selectedCount = selectedIds.map { it.size }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

        return SelectionState(
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            selectedCount = selectedCount
        )
    }

    // === Selection Operations ===

    /**
     * Enters selection mode by selecting the specified task
     * @param taskId ID of the task to initially select
     * @throws IllegalArgumentException if taskId is invalid
     */
    fun enterSelection(taskId: Long) {
        require(taskId > 0) { "Task ID must be positive, received: $taskId" }

        try {
            _selectedIds.value = setOf(taskId)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to enter selection mode for task $taskId", e)
        }
    }

    /**
     * Toggles selection state of a task
     * @param taskId ID of the task to toggle
     * @throws IllegalArgumentException if taskId is invalid
     */
    fun toggleSelection(taskId: Long) {
        require(taskId > 0) { "Task ID must be positive, received: $taskId" }

        try {
            val currentSelection = _selectedIds.value
            _selectedIds.value = if (currentSelection.contains(taskId)) {
                currentSelection - taskId
            } else {
                currentSelection + taskId
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to toggle selection for task $taskId", e)
        }
    }

    /**
     * Clears all selection and exits selection mode
     */
    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    /**
     * Selects all tasks from the provided list
     * @param taskIds List of task IDs to select
     * @throws IllegalArgumentException if any taskId is invalid or list constraints are violated
     */
    fun selectAll(taskIds: List<Long>) {
        require(taskIds.isNotEmpty()) { "Task IDs list cannot be empty" }
        require(taskIds.size <= MAX_SELECTION_SIZE) {
            "Cannot select more than $MAX_SELECTION_SIZE tasks at once, received: ${taskIds.size}"
        }
        require(taskIds.all { it > 0 }) {
            "All task IDs must be positive, invalid IDs: ${taskIds.filter { it <= 0 }}"
        }
        require(taskIds.distinct().size == taskIds.size) {
            "Task IDs list cannot contain duplicates"
        }

        try {
            _selectedIds.value = taskIds.toSet()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to select all ${taskIds.size} tasks", e)
        }
    }

    /**
     * Checks if any tasks are currently selected
     */
    fun hasSelection(): Boolean = _selectedIds.value.isNotEmpty()

    /**
     * Gets the current selection as a list for easier consumption
     */
    fun getSelectedIds(): List<Long> = _selectedIds.value.toList()

    /**
     * Gets the count of selected items
     */
    fun getSelectionCount(): Int = _selectedIds.value.size

    /**
     * Checks if a specific task is selected
     */
    fun isSelected(taskId: Long): Boolean = _selectedIds.value.contains(taskId)

    // === Validation Helpers ===

    /**
     * Validates the current selection state for bulk operations.
     *
     * This method examines the current selection and returns a sealed class result
     * that categorizes the selection type. This is essential for bulk operations
     * to handle different selection scenarios appropriately.
     *
     * @return [SelectionValidationResult] indicating the validation status:
     * - [SelectionValidationResult.Empty]: No tasks selected
     * - [SelectionValidationResult.SingleItem]: Exactly one task selected
     * - [SelectionValidationResult.MultipleItems]: Multiple tasks selected
     *
     * ## Usage Example
     * ```kotlin
     * when (val validation = selectionStateManager.validateSelection()) {
     *     is SelectionValidationResult.Empty -> {
     *         // Show "no selection" message
     *     }
     *     is SelectionValidationResult.SingleItem -> {
     *         // Handle single task operation
     *         val taskId = validation.taskId
     *     }
     *     is SelectionValidationResult.MultipleItems -> {
     *         // Handle bulk operation
     *         val taskIds = validation.taskIds
     *     }
     * }
     * ```
     *
     * @see SelectionValidationResult
     */
    fun validateSelection(): SelectionValidationResult {
        return when {
            _selectedIds.value.isEmpty() -> SelectionValidationResult.Empty
            _selectedIds.value.size == 1 -> SelectionValidationResult.SingleItem(_selectedIds.value.first())
            else -> SelectionValidationResult.MultipleItems(_selectedIds.value.toList())
        }
    }
}

/**
 * Data class containing all selection-related state flows
 */
data class SelectionState(
    val selectedIds: StateFlow<Set<Long>>,
    val isSelectionMode: StateFlow<Boolean>,
    val selectedCount: StateFlow<Int>
)

/**
 * Sealed class representing selection validation results
 */
sealed class SelectionValidationResult {
    object Empty : SelectionValidationResult()
    data class SingleItem(val taskId: Long) : SelectionValidationResult()
    data class MultipleItems(val taskIds: List<Long>) : SelectionValidationResult()
}