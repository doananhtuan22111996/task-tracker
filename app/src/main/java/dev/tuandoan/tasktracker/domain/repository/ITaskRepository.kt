package dev.tuandoan.tasktracker.domain.repository

import dev.tuandoan.tasktracker.data.database.DailyCount
import dev.tuandoan.tasktracker.data.database.TagInfo
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

    // Pin/Priority operations
    suspend fun setPinned(taskId: Long, pinned: Boolean)
    suspend fun setPriority(taskId: Long, priority: Int)
    suspend fun setPriorityBulk(ids: List<Long>, priority: Int)
    suspend fun setTagBulk(ids: List<Long>, tag: String?, tagColor: String?)

    // Archive operations
    fun getArchivedTasks(): Flow<List<Task>>
    suspend fun archiveTask(taskId: Long)
    suspend fun unarchiveTask(taskId: Long)
    suspend fun archiveTasks(ids: List<Long>)
    suspend fun unarchiveTasks(ids: List<Long>)
    suspend fun hardDeleteTask(taskId: Long)
    suspend fun hardDeleteTasks(ids: List<Long>)

    // Stats operations (exclude archived tasks)
    fun observeActiveCount(): Flow<Int>
    fun observeCompletedCount(): Flow<Int>
    fun observeCompletedTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int>
    fun observeDueTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int>
    fun observeOverdueCount(nowMillis: Long): Flow<Int>
    fun observeCompletedCountPerDay(startMillis: Long, endMillis: Long): Flow<List<DailyCount>>

    // Recurrence operations
    suspend fun getLatestGeneratedTask(parentRecurringTaskId: Long): Task?
    suspend fun completeAndGenerateNext(completedTask: Task, nextTask: Task): Long
    suspend fun uncompleteAndDeleteGenerated(reactivatedTask: Task, generatedTaskId: Long)
    suspend fun archiveAndGenerateNext(taskId: Long, nextTask: Task): Long

    // Streak operations
    suspend fun getCompletedTasksByChain(rootId: Long): List<Task>
    suspend fun getCompletedTasksForChains(rootIds: List<Long>): List<Task>
    suspend fun getActiveRecurringRootIds(): List<Long>

    // Tag management operations
    fun getDistinctTagsWithCount(): Flow<List<TagInfo>>
    suspend fun updateTagName(oldName: String, newName: String)
    suspend fun clearTag(tagName: String)
    suspend fun updateTagColor(tagName: String, color: String?)
    suspend fun getTagColor(tagName: String): String?

    // Backup operations
    suspend fun getAllTasksIncludingArchived(): List<Task>
    suspend fun replaceAllTasks(tasks: List<Task>)

    /**
     * Atomically replaces all tasks AND all subtasks. Used by import to avoid a partial state
     * where tasks exist without their subtasks. The caller is responsible for supplying subtasks
     * whose `taskId` references a task id also present in [tasks].
     */
    suspend fun replaceAllTasksAndSubtasks(
        tasks: List<Task>,
        subtasks: List<dev.tuandoan.tasktracker.data.database.Subtask>,
    )
}
