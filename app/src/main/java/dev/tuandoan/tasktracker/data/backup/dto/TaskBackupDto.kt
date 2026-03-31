package dev.tuandoan.tasktracker.data.backup.dto

import dev.tuandoan.tasktracker.data.database.Task
import kotlinx.serialization.Serializable

/**
 * Data transfer object for serializing/deserializing a single task in backup files.
 * All fields have defaults for backward compatibility with older backup schemas.
 */
@Serializable
data class TaskBackupDto(
    val id: Long,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val completedAt: Long? = null,
    val dueAt: Long? = null,
    val dueAtHasTime: Boolean = false,
    val reminderOffsetMinutes: Int? = null,
    val tag: String? = null,
    val isPinned: Boolean = false,
    val priority: Int = 1,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val recurrenceType: Int = 0,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: Int = 0,
    val recurrenceEndDate: Long? = null,
    val parentRecurringTaskId: Long? = null,
) {

    fun toTask(): Task = Task(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        createdAt = createdAt,
        completedAt = completedAt,
        dueAt = dueAt,
        dueAtHasTime = dueAtHasTime,
        reminderOffsetMinutes = reminderOffsetMinutes,
        tag = tag,
        isPinned = isPinned,
        priority = priority,
        isArchived = isArchived,
        archivedAt = archivedAt,
        recurrenceType = recurrenceType,
        recurrenceInterval = recurrenceInterval,
        recurrenceDaysOfWeek = recurrenceDaysOfWeek,
        recurrenceEndDate = recurrenceEndDate,
        parentRecurringTaskId = parentRecurringTaskId,
    )

    companion object {
        fun fromTask(task: Task): TaskBackupDto = TaskBackupDto(
            id = task.id,
            title = task.title,
            description = task.description,
            isCompleted = task.isCompleted,
            createdAt = task.createdAt,
            completedAt = task.completedAt,
            dueAt = task.dueAt,
            dueAtHasTime = task.dueAtHasTime,
            reminderOffsetMinutes = task.reminderOffsetMinutes,
            tag = task.tag,
            isPinned = task.isPinned,
            priority = task.priority,
            isArchived = task.isArchived,
            archivedAt = task.archivedAt,
            recurrenceType = task.recurrenceType,
            recurrenceInterval = task.recurrenceInterval,
            recurrenceDaysOfWeek = task.recurrenceDaysOfWeek,
            recurrenceEndDate = task.recurrenceEndDate,
            parentRecurringTaskId = task.parentRecurringTaskId,
        )
    }
}
