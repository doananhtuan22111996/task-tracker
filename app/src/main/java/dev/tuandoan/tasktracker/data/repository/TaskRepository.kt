package dev.tuandoan.tasktracker.data.repository

import androidx.room.withTransaction
import dev.tuandoan.tasktracker.data.database.DailyCount
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.data.database.TaskDao
import dev.tuandoan.tasktracker.data.database.TaskDatabase
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of ITaskRepository that handles task data operations.
 * Uses @Inject constructor for Hilt dependency injection.
 * @Singleton ensures single instance across the app lifecycle.
 */
class TaskRepository @Inject constructor(private val taskDao: TaskDao, private val taskDatabase: TaskDatabase) :
    ITaskRepository {

    override fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    override suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    override suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    override suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    override suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    override suspend fun upsert(task: Task) = taskDao.upsert(task)

    override fun getActiveTasks(): Flow<List<Task>> = taskDao.getActiveTasks()

    override fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks()

    override suspend fun toggleTaskCompletion(task: Task) {
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        updateTask(updatedTask)
    }

    // Bulk operations
    override suspend fun markCompleted(ids: List<Long>) {
        val currentTime = System.currentTimeMillis()
        taskDao.markCompleted(ids, currentTime)
    }

    override suspend fun markActive(ids: List<Long>) = taskDao.markActive(ids)

    override suspend fun deleteByIds(ids: List<Long>) = taskDao.deleteByIds(ids)

    override suspend fun getTasksByIds(ids: List<Long>): List<Task> = taskDao.getTasksByIds(ids)

    override suspend fun upsertAll(tasks: List<Task>) = taskDao.upsertAll(tasks)

    // Pin/Priority operations
    override suspend fun setPinned(taskId: Long, pinned: Boolean) = taskDao.setPinned(taskId, pinned)

    override suspend fun setPriority(taskId: Long, priority: Int) = taskDao.setPriority(taskId, priority)

    // Archive operations
    override fun getArchivedTasks(): Flow<List<Task>> = taskDao.getArchivedTasks()

    override suspend fun archiveTask(taskId: Long) {
        val currentTime = System.currentTimeMillis()
        taskDao.setArchived(taskId, archived = true, archivedAt = currentTime)
    }

    override suspend fun unarchiveTask(taskId: Long) {
        taskDao.setArchived(taskId, archived = false, archivedAt = null)
    }

    override suspend fun archiveTasks(ids: List<Long>) {
        val currentTime = System.currentTimeMillis()
        taskDao.setArchivedBulk(ids, archived = true, archivedAt = currentTime)
    }

    override suspend fun unarchiveTasks(ids: List<Long>) {
        taskDao.setArchivedBulk(ids, archived = false, archivedAt = null)
    }

    override suspend fun hardDeleteTask(taskId: Long) = taskDao.hardDeleteById(taskId)

    override suspend fun hardDeleteTasks(ids: List<Long>) = taskDao.hardDeleteByIds(ids)

    // Stats operations (exclude archived tasks)
    override fun observeActiveCount(): Flow<Int> = taskDao.observeActiveCount()

    override fun observeCompletedCount(): Flow<Int> = taskDao.observeCompletedCount()

    override fun observeCompletedTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> =
        taskDao.observeCompletedTodayCount(startOfDayMillis, endOfDayMillis)

    override fun observeDueTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> =
        taskDao.observeDueTodayCount(startOfDayMillis, endOfDayMillis)

    override fun observeOverdueCount(nowMillis: Long): Flow<Int> = taskDao.observeOverdueCount(nowMillis)

    override fun observeCompletedCountPerDay(startMillis: Long, endMillis: Long): Flow<List<DailyCount>> =
        taskDao.observeCompletedCountPerDay(startMillis, endMillis)

    // Backup operations
    override suspend fun getAllTasksIncludingArchived(): List<Task> = taskDao.getAllTasksIncludingArchived()

    override suspend fun replaceAllTasks(tasks: List<Task>) {
        taskDatabase.withTransaction {
            taskDao.deleteAllTasks()
            taskDao.upsertAll(tasks)
        }
    }
}
