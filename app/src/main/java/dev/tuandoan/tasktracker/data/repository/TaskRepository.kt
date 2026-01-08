package dev.tuandoan.tasktracker.data.repository

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.data.database.TaskDao
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ITaskRepository that handles task data operations.
 * Uses @Inject constructor for Hilt dependency injection.
 * @Singleton ensures single instance across the app lifecycle.
 */
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) : ITaskRepository {

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
    override suspend fun markCompleted(ids: List<Long>) = taskDao.markCompleted(ids)

    override suspend fun markActive(ids: List<Long>) = taskDao.markActive(ids)

    override suspend fun deleteByIds(ids: List<Long>) = taskDao.deleteByIds(ids)

    override suspend fun getTasksByIds(ids: List<Long>): List<Task> = taskDao.getTasksByIds(ids)

    override suspend fun upsertAll(tasks: List<Task>) = taskDao.upsertAll(tasks)
}