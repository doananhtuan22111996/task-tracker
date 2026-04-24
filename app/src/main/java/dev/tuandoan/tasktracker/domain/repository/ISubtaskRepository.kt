package dev.tuandoan.tasktracker.domain.repository

import dev.tuandoan.tasktracker.data.database.Subtask
import kotlinx.coroutines.flow.Flow

interface ISubtaskRepository {

    fun observeSubtasks(taskId: Long): Flow<List<Subtask>>

    suspend fun getSubtasks(taskId: Long): List<Subtask>

    suspend fun getSubtaskById(id: Long): Subtask?

    suspend fun insertSubtask(subtask: Subtask): Long

    suspend fun updateSubtask(subtask: Subtask)

    suspend fun deleteById(id: Long)

    suspend fun deleteByTaskId(taskId: Long)

    suspend fun resetCompletionForTask(taskId: Long)

    suspend fun countForTask(taskId: Long): Int

    // Bulk operations
    suspend fun upsertAll(subtasks: List<Subtask>)

    /**
     * Atomically rewrites `sortOrder` for every subtask under [taskId] based on its position in
     * [orderedIds]. Fails if [orderedIds] does not exactly match the current subtask id set
     * for the task — partial writes are avoided by wrapping the update in a DB transaction.
     */
    suspend fun reorderSubtasks(taskId: Long, orderedIds: List<Long>)

    /**
     * Atomically copies every subtask under [fromTaskId] to [toTaskId] with `isCompleted = false`,
     * preserving titles and `sortOrder`. No-op when the source has no subtasks. Used by recurrence
     * regeneration so a new task instance inherits a fresh checklist.
     */
    suspend fun copySubtasksResetCompletion(fromTaskId: Long, toTaskId: Long)

    /**
     * Returns every subtask across all tasks, ordered by `taskId`, `sortOrder`, `id`. Used by
     * backup export to serialize the full subtask corpus in one query.
     */
    suspend fun getAllSubtasks(): List<dev.tuandoan.tasktracker.data.database.Subtask>
}
