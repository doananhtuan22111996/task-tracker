package dev.tuandoan.tasktracker.testutil

import dev.tuandoan.tasktracker.data.database.Task

/**
 * Factory for creating deterministic test [Task] instances.
 * All timestamps use fixed values to ensure test determinism.
 */
object TestTaskFactory {

    /** Base timestamp: 2024-01-15 12:00:00 UTC */
    const val BASE_TIMESTAMP = 1705320000000L

    /** One hour in millis */
    const val ONE_HOUR_MS = 3_600_000L

    /** One day in millis */
    const val ONE_DAY_MS = 86_400_000L

    fun createTask(
        id: Long = 1L,
        title: String = "Test Task",
        description: String = "",
        isCompleted: Boolean = false,
        createdAt: Long = BASE_TIMESTAMP,
        completedAt: Long? = null,
        dueAt: Long? = null,
        reminderOffsetMinutes: Int? = null,
        tag: String? = null,
        isPinned: Boolean = false,
        priority: Int = 1,
        isArchived: Boolean = false,
        archivedAt: Long? = null,
    ): Task = Task(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        createdAt = createdAt,
        completedAt = completedAt,
        dueAt = dueAt,
        reminderOffsetMinutes = reminderOffsetMinutes,
        tag = tag,
        isPinned = isPinned,
        priority = priority,
        isArchived = isArchived,
        archivedAt = archivedAt,
    )

    /**
     * Creates a list of tasks with sequential IDs and creation timestamps.
     * Each task is created 1 hour after the previous.
     */
    fun createTaskList(count: Int, startId: Long = 1L): List<Task> = (0 until count).map { i ->
        createTask(
            id = startId + i,
            title = "Task ${startId + i}",
            createdAt = BASE_TIMESTAMP + (i * ONE_HOUR_MS),
        )
    }

    fun completedTask(
        id: Long = 1L,
        title: String = "Completed Task",
        createdAt: Long = BASE_TIMESTAMP,
        completedAt: Long = BASE_TIMESTAMP + ONE_HOUR_MS,
    ): Task = createTask(
        id = id,
        title = title,
        isCompleted = true,
        createdAt = createdAt,
        completedAt = completedAt,
    )

    fun archivedTask(
        id: Long = 1L,
        title: String = "Archived Task",
        createdAt: Long = BASE_TIMESTAMP,
        archivedAt: Long = BASE_TIMESTAMP + ONE_HOUR_MS,
    ): Task = createTask(
        id = id,
        title = title,
        isArchived = true,
        createdAt = createdAt,
        archivedAt = archivedAt,
    )

    fun pinnedTask(id: Long = 1L, title: String = "Pinned Task", createdAt: Long = BASE_TIMESTAMP): Task = createTask(
        id = id,
        title = title,
        isPinned = true,
        createdAt = createdAt,
    )

    fun taskWithDueDate(
        id: Long = 1L,
        title: String = "Due Task",
        createdAt: Long = BASE_TIMESTAMP,
        dueAt: Long = BASE_TIMESTAMP + ONE_DAY_MS,
        reminderOffsetMinutes: Int? = 60,
    ): Task = createTask(
        id = id,
        title = title,
        createdAt = createdAt,
        dueAt = dueAt,
        reminderOffsetMinutes = reminderOffsetMinutes,
    )
}
