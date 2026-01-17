package dev.tuandoan.tasktracker.domain

import dev.tuandoan.tasktracker.data.database.Task
import kotlinx.coroutines.flow.Flow

interface ITaskManager {
    // Data access
    fun getAllTasks(): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Task?

    // Task operations
    suspend fun createTask(title: String, description: String = ""): Long
    suspend fun createTask(title: String, description: String = "", dueAt: Long? = null, reminderOffsetMinutes: Int? = null): Long
    suspend fun createTask(title: String, description: String = "", dueAt: Long? = null, reminderOffsetMinutes: Int? = null, tag: String? = null): Long
    suspend fun updateTask(task: Task)
    suspend fun updateTaskContent(taskId: Long, title: String, description: String)
    suspend fun updateTaskContent(taskId: Long, title: String, description: String, dueAt: Long?, reminderOffsetMinutes: Int?): Boolean
    suspend fun updateTaskContent(taskId: Long, title: String, description: String, dueAt: Long?, reminderOffsetMinutes: Int?, tag: String?): Boolean
    suspend fun deleteTask(task: Task)
    suspend fun restoreTask(task: Task): Result<Unit>
    suspend fun toggleTaskCompletion(task: Task)
    suspend fun markTaskComplete(task: Task)
    suspend fun markTaskIncomplete(task: Task)

    // Bulk operations
    suspend fun setCompletedBulk(ids: List<Long>, completed: Boolean)
    suspend fun deleteTasksByIds(ids: List<Long>)
    suspend fun restoreTasks(tasks: List<Task>): Result<Unit>

    // Filtered data access
    fun getActiveTasks(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>

    // Pin/Priority operations
    suspend fun setPinned(taskId: Long, pinned: Boolean)
    suspend fun setPriority(taskId: Long, priority: Int)

    // Archive operations
    fun getArchivedTasks(): Flow<List<Task>>
    suspend fun archiveTask(taskId: Long)
    suspend fun unarchiveTask(taskId: Long)
    suspend fun archiveTasks(ids: List<Long>)
    suspend fun unarchiveTasks(ids: List<Long>)
    suspend fun hardDeleteTask(taskId: Long)
    suspend fun hardDeleteTasks(ids: List<Long>)
}