package dev.tuandoan.tasktracker.domain.repository

import dev.tuandoan.tasktracker.data.database.Task
import kotlinx.coroutines.flow.Flow

interface ITaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Task?
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun upsert(task: Task)
    fun getActiveTasks(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>
    suspend fun toggleTaskCompletion(task: Task)

    // Bulk operations
    suspend fun markCompleted(ids: List<Long>)
    suspend fun markActive(ids: List<Long>)
    suspend fun deleteByIds(ids: List<Long>)
    suspend fun getTasksByIds(ids: List<Long>): List<Task>
    suspend fun upsertAll(tasks: List<Task>)
}