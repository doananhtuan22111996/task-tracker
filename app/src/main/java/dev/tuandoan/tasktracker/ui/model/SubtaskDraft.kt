package dev.tuandoan.tasktracker.ui.model

import dev.tuandoan.tasktracker.data.database.Subtask

/**
 * Editor-local representation of a subtask. Carries the persisted `id` (positive, from Room) or a
 * negative placeholder id for drafts created in this editing session that haven't been saved yet.
 * The negative-id scheme lets the UI key LazyColumn rows uniquely before the DB assigns real ids.
 */
data class SubtaskDraft(val id: Long, val title: String, val isCompleted: Boolean, val sortOrder: Int) {
    val isPersisted: Boolean get() = id > 0L

    companion object {
        fun fromSubtask(subtask: Subtask): SubtaskDraft = SubtaskDraft(
            id = subtask.id,
            title = subtask.title,
            isCompleted = subtask.isCompleted,
            sortOrder = subtask.sortOrder,
        )
    }
}
