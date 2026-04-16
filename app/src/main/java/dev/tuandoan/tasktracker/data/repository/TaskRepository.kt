package dev.tuandoan.tasktracker.data.repository

import androidx.room.withTransaction
import dev.tuandoan.tasktracker.data.database.DailyCount
import dev.tuandoan.tasktracker.data.database.TagInfo
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

    // Recurrence operations
    override suspend fun getLatestGeneratedTask(parentRecurringTaskId: Long): Task? =
        taskDao.getLatestGeneratedTask(parentRecurringTaskId)

    override suspend fun completeAndGenerateNext(completedTask: Task, nextTask: Task): Long =
        taskDatabase.withTransaction {
            taskDao.updateTask(completedTask)
            taskDao.insertTask(nextTask)
        }

    override suspend fun uncompleteAndDeleteGenerated(reactivatedTask: Task, generatedTaskId: Long) {
        taskDatabase.withTransaction {
            taskDao.updateTask(reactivatedTask)
            taskDao.hardDeleteById(generatedTaskId)
        }
    }

    override suspend fun archiveAndGenerateNext(taskId: Long, nextTask: Task): Long {
        val currentTime = System.currentTimeMillis()
        return taskDatabase.withTransaction {
            taskDao.setArchived(taskId, archived = true, archivedAt = currentTime)
            taskDao.insertTask(nextTask)
        }
    }

    // Streak operations
    override suspend fun getCompletedTasksByChain(rootId: Long): List<Task> = taskDao.getCompletedTasksByChain(rootId)

    override suspend fun getCompletedTasksForChains(rootIds: List<Long>): List<Task> =
        taskDao.getCompletedTasksForChains(rootIds)

    override suspend fun getActiveRecurringRootIds(): List<Long> = taskDao.getActiveRecurringRootIds()

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

    // Tag management operations
    override fun getDistinctTagsWithCount(): Flow<List<TagInfo>> = taskDao.getDistinctTagsWithCount()

    override suspend fun updateTagName(oldName: String, newName: String) = taskDao.updateTagName(oldName, newName)

    override suspend fun clearTag(tagName: String) = taskDao.clearTag(tagName)

    override suspend fun updateTagColor(tagName: String, color: String?) = taskDao.updateTagColor(tagName, color)

    override suspend fun getTagColor(tagName: String): String? = taskDao.getTagColor(tagName)

    // Backup operations
    override suspend fun getAllTasksIncludingArchived(): List<Task> = taskDao.getAllTasksIncludingArchived()

    override suspend fun replaceAllTasks(tasks: List<Task>) {
        taskDatabase.withTransaction {
            taskDao.deleteAllTasks()
            taskDao.upsertAll(tasks)
        }
    }
}
