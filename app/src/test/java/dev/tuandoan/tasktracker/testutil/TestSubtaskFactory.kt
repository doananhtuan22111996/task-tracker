package dev.tuandoan.tasktracker.testutil

import dev.tuandoan.tasktracker.data.database.Subtask

/**
 * Factory for deterministic [Subtask] test instances. All timestamps default to
 * [TestTaskFactory.BASE_TIMESTAMP] so fixtures compose cleanly with existing task fixtures.
 */
object TestSubtaskFactory {

    fun createSubtask(
        id: Long = 1L,
        taskId: Long = 1L,
        title: String = "Test Subtask",
        isCompleted: Boolean = false,
        sortOrder: Int = 0,
        createdAt: Long = TestTaskFactory.BASE_TIMESTAMP,
    ): Subtask = Subtask(
        id = id,
        taskId = taskId,
        title = title,
        isCompleted = isCompleted,
        sortOrder = sortOrder,
        createdAt = createdAt,
    )

    /**
     * Creates [count] subtasks under [taskId] with sequential ids and increasing sortOrder.
     */
    fun createSubtaskList(count: Int, taskId: Long = 1L, startId: Long = 1L): List<Subtask> = (0 until count).map { i ->
        createSubtask(
            id = startId + i,
            taskId = taskId,
            title = "Subtask ${startId + i}",
            sortOrder = i,
        )
    }
}
