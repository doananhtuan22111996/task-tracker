package dev.tuandoan.tasktracker.domain

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ITaskManager that provides business logic for task operations.
 * Uses @Inject constructor for Hilt dependency injection.
 * @Singleton ensures single instance across the app lifecycle.
 */
class TaskManager @Inject constructor(
    private val repository: ITaskRepository
) : ITaskManager {

    // Data access
    override fun getAllTasks(): Flow<List<Task>> = repository.getAllTasks()

    override suspend fun getTaskById(id: Long): Task? = repository.getTaskById(id)

    // Task operations
    override suspend fun createTask(title: String, description: String): Long {
        require(title.isNotBlank()) { "Task title cannot be blank" }

        val task = Task(
            title = title.trim(),
            description = description.trim()
        )
        return repository.insertTask(task)
    }

    override suspend fun updateTask(task: Task) {
        repository.updateTask(task)
    }

    override suspend fun updateTaskContent(taskId: Long, title: String, description: String) {
        require(title.isNotBlank()) { "Task title cannot be blank" }

        val existingTask = repository.getTaskById(taskId)
        requireNotNull(existingTask) { "Task with id $taskId not found" }

        val updatedTask = existingTask.copy(
            title = title.trim(),
            description = description.trim()
        )
        repository.updateTask(updatedTask)
    }

    override suspend fun deleteTask(task: Task) {
        repository.deleteTask(task)
    }

    override suspend fun restoreTask(task: Task): Result<Unit> {
        return try {
            repository.upsert(task)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleTaskCompletion(task: Task) {
        repository.toggleTaskCompletion(task)
    }

    override suspend fun markTaskComplete(task: Task) {
        if (!task.isCompleted) {
            val completedTask = task.copy(isCompleted = true)
            repository.updateTask(completedTask)
        }
    }

    override suspend fun markTaskIncomplete(task: Task) {
        if (task.isCompleted) {
            val incompleteTask = task.copy(isCompleted = false)
            repository.updateTask(incompleteTask)
        }
    }

    // Filtered data access
    override fun getActiveTasks(): Flow<List<Task>> = repository.getActiveTasks()

    override fun getCompletedTasks(): Flow<List<Task>> = repository.getCompletedTasks()
}