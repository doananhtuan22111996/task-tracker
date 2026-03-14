package dev.tuandoan.tasktracker.ui.events

import dev.tuandoan.tasktracker.data.database.Task

/**
 * Sealed class representing UI events that should be handled by the UI layer.
 * These events are typically one-time events that should be consumed after handling.
 */
sealed class UiEvent {
    /**
     * Event to show a snackbar with an optional action
     */
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val onActionClick: () -> Unit = {}) :
        UiEvent()

    /**
     * Event to show undo action with custom message (supports single or bulk operations)
     */
    data class ShowUndoDelete(
        val tasks: List<Task>,
        val onUndo: () -> Unit,
        val message: String? = null, // Custom message, if null will generate default
    ) : UiEvent()

    data object ShowRatingPrompt : UiEvent()
}
