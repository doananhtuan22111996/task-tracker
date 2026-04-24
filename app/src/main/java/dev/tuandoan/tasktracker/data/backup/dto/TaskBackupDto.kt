package dev.tuandoan.tasktracker.data.backup.dto

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.service.TagNormalizer
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
    val tagColor: String? = null,
    val isPinned: Boolean = false,
    val priority: Int = 1,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val recurrenceType: Int = 0,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: Int = 0,
    val recurrenceEndDate: Long? = null,
    val parentRecurringTaskId: Long? = null,
    // v3 schema: subtasks per task. Defaults to empty for backward compatibility with v1/v2.
    val subtasks: List<SubtaskBackupDto> = emptyList(),
) {

    fun toTask(): Task {
        val normalizedTag = TagNormalizer.normalize(tag)
        // Orphan tagColor without a tag is meaningless — drop it on import to match the
        // invariant enforced by the v10→v11 migration.
        val resolvedColor = if (normalizedTag == null) null else tagColor
        return Task(
            id = id,
            title = title,
            description = description,
            isCompleted = isCompleted,
            createdAt = createdAt,
            completedAt = completedAt,
            dueAt = dueAt,
            dueAtHasTime = dueAtHasTime,
            reminderOffsetMinutes = reminderOffsetMinutes,
            tag = normalizedTag,
            tagColor = resolvedColor,
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
    }

    companion object {
        fun fromTask(
            task: Task,
            subtasks: List<dev.tuandoan.tasktracker.data.database.Subtask> = emptyList(),
        ): TaskBackupDto = TaskBackupDto(
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
            tagColor = task.tagColor,
            isPinned = task.isPinned,
            priority = task.priority,
            isArchived = task.isArchived,
            archivedAt = task.archivedAt,
            recurrenceType = task.recurrenceType,
            recurrenceInterval = task.recurrenceInterval,
            recurrenceDaysOfWeek = task.recurrenceDaysOfWeek,
            recurrenceEndDate = task.recurrenceEndDate,
            parentRecurringTaskId = task.parentRecurringTaskId,
            subtasks = subtasks
                .sortedWith(compareBy({ it.sortOrder }, { it.id }))
                .map { SubtaskBackupDto.fromSubtask(it) },
        )
    }
}
