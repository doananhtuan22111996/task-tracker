package dev.tuandoan.tasktracker.data.repository

import androidx.room.withTransaction
import dev.tuandoan.tasktracker.data.database.Subtask
import dev.tuandoan.tasktracker.data.database.SubtaskDao
import dev.tuandoan.tasktracker.data.database.SubtaskProgress
import dev.tuandoan.tasktracker.data.database.TaskDatabase
import dev.tuandoan.tasktracker.domain.repository.ISubtaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of [ISubtaskRepository] backed by Room.
 * Multi-DAO operations (reorder) are wrapped in [TaskDatabase.withTransaction] for atomicity.
 */
class SubtaskRepository @Inject constructor(
    private val subtaskDao: SubtaskDao,
    private val taskDatabase: TaskDatabase,
) : ISubtaskRepository {

    override fun observeSubtasks(taskId: Long): Flow<List<Subtask>> = subtaskDao.observeSubtasks(taskId)

    override fun observeSubtaskProgress(): Flow<List<SubtaskProgress>> = subtaskDao.observeSubtaskProgress()

    override suspend fun getSubtasks(taskId: Long): List<Subtask> = subtaskDao.getSubtasks(taskId)

    override suspend fun getSubtaskById(id: Long): Subtask? = subtaskDao.getSubtaskById(id)

    override suspend fun insertSubtask(subtask: Subtask): Long = subtaskDao.insertSubtask(subtask)

    override suspend fun updateSubtask(subtask: Subtask) = subtaskDao.updateSubtask(subtask)

    override suspend fun deleteById(id: Long) = subtaskDao.deleteById(id)

    override suspend fun deleteByTaskId(taskId: Long) = subtaskDao.deleteByTaskId(taskId)

    override suspend fun resetCompletionForTask(taskId: Long) = subtaskDao.resetCompletionForTask(taskId)

    override suspend fun countForTask(taskId: Long): Int = subtaskDao.countForTask(taskId)

    override suspend fun upsertAll(subtasks: List<Subtask>) = subtaskDao.upsertAll(subtasks)

    override suspend fun reorderSubtasks(taskId: Long, orderedIds: List<Long>) {
        taskDatabase.withTransaction {
            val current = subtaskDao.getSubtasks(taskId)
            val currentIds = current.map { it.id }.toSet()
            val requestedIds = orderedIds.toSet()
            require(currentIds == requestedIds && orderedIds.size == currentIds.size) {
                "reorderSubtasks: orderedIds must match the current subtask id set for task $taskId"
            }
            val byId = current.associateBy { it.id }
            val reordered = orderedIds.mapIndexed { index, id ->
                byId.getValue(id).copy(sortOrder = index)
            }
            subtaskDao.upsertAll(reordered)
        }
    }

    override suspend fun getAllSubtasks(): List<Subtask> = subtaskDao.getAllSubtasks()

    override suspend fun copySubtasksResetCompletion(fromTaskId: Long, toTaskId: Long) {
        taskDatabase.withTransaction {
            val source = subtaskDao.getSubtasks(fromTaskId)
            if (source.isEmpty()) return@withTransaction
            val copies = source.map { original ->
                original.copy(
                    id = 0,
                    taskId = toTaskId,
                    isCompleted = false,
                )
            }
            subtaskDao.upsertAll(copies)
        }
    }
}
