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
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onActionClick: () -> Unit = {}
    ) : UiEvent()

    /**
     * Event to show delete confirmation with undo functionality
     */
    data class ShowDeleteUndo(
        val task: Task,
        val onUndo: () -> Unit
    ) : UiEvent()
}