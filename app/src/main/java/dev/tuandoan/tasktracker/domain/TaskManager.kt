package dev.tuandoan.tasktracker.domain

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import dev.tuandoan.tasktracker.domain.scheduler.TaskReminderScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ITaskManager that provides business logic for task operations.
 * Uses @Inject constructor for Hilt dependency injection.
 * @Singleton ensures single instance across the app lifecycle.
 */
class TaskManager @Inject constructor(
    private val repository: ITaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) : ITaskManager {

    // Data access
    override fun getAllTasks(): Flow<List<Task>> = repository.getAllTasks()

    override suspend fun getTaskById(id: Long): Task? = repository.getTaskById(id)

    // Task operations
    override suspend fun createTask(title: String, description: String): Long {
        return createTask(title, description, null, null)
    }

    override suspend fun createTask(title: String, description: String, dueAt: Long?, reminderOffsetMinutes: Int?): Long {
        return createTask(title, description, dueAt, reminderOffsetMinutes, null)
    }

    override suspend fun createTask(title: String, description: String, dueAt: Long?, reminderOffsetMinutes: Int?, tag: String?): Long {
        require(title.isNotBlank()) { "Task title cannot be blank" }

        val task = Task(
            title = title.trim(),
            description = description.trim(),
            dueAt = dueAt,
            reminderOffsetMinutes = reminderOffsetMinutes,
            tag = tag?.trim()?.takeIf { it.isNotEmpty() }
        )
        val taskId = repository.insertTask(task)

        // Schedule reminder if applicable
        scheduleReminderIfNeeded(taskId, title.trim(), dueAt, reminderOffsetMinutes)

        return taskId
    }

    override suspend fun updateTask(task: Task) {
        val existingTask = repository.getTaskById(task.id)
        repository.updateTask(task)

        // Handle reminder rescheduling if due date or reminder changed
        if (existingTask != null) {
            val dueDateChanged = existingTask.dueAt != task.dueAt
            val reminderChanged = existingTask.reminderOffsetMinutes != task.reminderOffsetMinutes

            if (dueDateChanged || reminderChanged) {
                // Cancel existing reminder
                reminderScheduler.cancel(task.id)

                // Schedule new reminder if task is not completed
                if (!task.isCompleted) {
                    scheduleReminderIfNeeded(task.id, task.title, task.dueAt, task.reminderOffsetMinutes)
                }
            }
        }
    }

    override suspend fun updateTaskContent(taskId: Long, title: String, description: String) {
        updateTaskContent(taskId, title, description, null, null, null)
    }

    override suspend fun updateTaskContent(taskId: Long, title: String, description: String, dueAt: Long?, reminderOffsetMinutes: Int?): Boolean {
        return updateTaskContent(taskId, title, description, dueAt, reminderOffsetMinutes, null)
    }

    override suspend fun updateTaskContent(taskId: Long, title: String, description: String, dueAt: Long?, reminderOffsetMinutes: Int?, tag: String?): Boolean {
        require(title.isNotBlank()) { "Task title cannot be blank" }

        val existingTask = repository.getTaskById(taskId)
        requireNotNull(existingTask) { "Task with id $taskId not found" }

        val updatedTask = existingTask.copy(
            title = title.trim(),
            description = description.trim(),
            dueAt = dueAt,
            reminderOffsetMinutes = reminderOffsetMinutes,
            tag = tag?.trim()?.takeIf { it.isNotEmpty() }
        )

        repository.updateTask(updatedTask)

        // Handle reminder rescheduling
        val dueDateChanged = existingTask.dueAt != dueAt
        val reminderChanged = existingTask.reminderOffsetMinutes != reminderOffsetMinutes

        if (dueDateChanged || reminderChanged) {
            // Cancel existing reminder
            reminderScheduler.cancel(taskId)

            // Schedule new reminder if task is not completed
            if (!updatedTask.isCompleted) {
                return scheduleReminderIfNeeded(taskId, title.trim(), dueAt, reminderOffsetMinutes)
            }
        }

        return true
    }

    override suspend fun deleteTask(task: Task) {
        // Cancel any pending reminder before deleting
        reminderScheduler.cancel(task.id)
        repository.deleteTask(task)
    }

    override suspend fun restoreTask(task: Task): Result<Unit> {
        return try {
            repository.upsert(task)
            // Reschedule reminder if task is not completed and has reminder settings
            if (!task.isCompleted) {
                scheduleReminderIfNeeded(task.id, task.title, task.dueAt, task.reminderOffsetMinutes)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleTaskCompletion(task: Task) {
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        repository.updateTask(updatedTask)

        // Handle reminder based on completion status
        if (updatedTask.isCompleted) {
            // Cancel reminder when task is completed
            reminderScheduler.cancel(task.id)
        } else {
            // Reschedule reminder when task is marked incomplete
            scheduleReminderIfNeeded(task.id, task.title, task.dueAt, task.reminderOffsetMinutes)
        }
    }

    override suspend fun markTaskComplete(task: Task) {
        if (!task.isCompleted) {
            val completedTask = task.copy(isCompleted = true)
            repository.updateTask(completedTask)
            // Cancel reminder when marking as complete
            reminderScheduler.cancel(task.id)
        }
    }

    override suspend fun markTaskIncomplete(task: Task) {
        if (task.isCompleted) {
            val incompleteTask = task.copy(isCompleted = false)
            repository.updateTask(incompleteTask)
            // Reschedule reminder when marking as incomplete
            scheduleReminderIfNeeded(task.id, task.title, task.dueAt, task.reminderOffsetMinutes)
        }
    }

    // Bulk operations
    override suspend fun setCompletedBulk(ids: List<Long>, completed: Boolean) {
        if (ids.isEmpty()) return

        if (completed) {
            repository.markCompleted(ids)
            // Cancel reminders for all completed tasks
            ids.forEach { taskId ->
                reminderScheduler.cancel(taskId)
            }
        } else {
            repository.markActive(ids)
            // Reschedule reminders for all reactivated tasks
            ids.forEach { taskId ->
                val task = repository.getTaskById(taskId)
                if (task != null) {
                    scheduleReminderIfNeeded(taskId, task.title, task.dueAt, task.reminderOffsetMinutes)
                }
            }
        }
    }

    override suspend fun deleteTasksByIds(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            // Cancel reminders for all deleted tasks
            ids.forEach { taskId ->
                reminderScheduler.cancel(taskId)
            }
            repository.deleteByIds(ids)
        }
    }

    override suspend fun restoreTasks(tasks: List<Task>): Result<Unit> {
        return try {
            if (tasks.isNotEmpty()) {
                repository.upsertAll(tasks)
                // Reschedule reminders for all restored incomplete tasks
                tasks.filter { !it.isCompleted }.forEach { task ->
                    scheduleReminderIfNeeded(task.id, task.title, task.dueAt, task.reminderOffsetMinutes)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Filtered data access
    override fun getActiveTasks(): Flow<List<Task>> = repository.getActiveTasks()

    override fun getCompletedTasks(): Flow<List<Task>> = repository.getCompletedTasks()

    // Pin/Priority operations
    override suspend fun setPinned(taskId: Long, pinned: Boolean) {
        repository.setPinned(taskId, pinned)
    }

    override suspend fun setPriority(taskId: Long, priority: Int) {
        repository.setPriority(taskId, priority)
    }

    // Archive operations
    override fun getArchivedTasks(): Flow<List<Task>> = repository.getArchivedTasks()

    override suspend fun archiveTask(taskId: Long) {
        // Cancel any pending reminder before archiving
        reminderScheduler.cancel(taskId)
        repository.archiveTask(taskId)
    }

    override suspend fun unarchiveTask(taskId: Long) {
        repository.unarchiveTask(taskId)

        // Reschedule reminder if task is not completed and has reminder settings
        val task = repository.getTaskById(taskId)
        if (task != null && !task.isCompleted) {
            scheduleReminderIfNeeded(taskId, task.title, task.dueAt, task.reminderOffsetMinutes)
        }
    }

    override suspend fun archiveTasks(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            // Cancel reminders for all archived tasks
            ids.forEach { taskId ->
                reminderScheduler.cancel(taskId)
            }
            repository.archiveTasks(ids)
        }
    }

    override suspend fun unarchiveTasks(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            repository.unarchiveTasks(ids)

            // Reschedule reminders for all restored incomplete tasks
            ids.forEach { taskId ->
                val task = repository.getTaskById(taskId)
                if (task != null && !task.isCompleted) {
                    scheduleReminderIfNeeded(taskId, task.title, task.dueAt, task.reminderOffsetMinutes)
                }
            }
        }
    }

    override suspend fun hardDeleteTask(taskId: Long) {
        // Cancel any pending reminder before permanently deleting
        reminderScheduler.cancel(taskId)
        repository.hardDeleteTask(taskId)
    }

    override suspend fun hardDeleteTasks(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            // Cancel reminders for all permanently deleted tasks
            ids.forEach { taskId ->
                reminderScheduler.cancel(taskId)
            }
            repository.hardDeleteTasks(ids)
        }
    }

    // Helper method for scheduling reminders
    private suspend fun scheduleReminderIfNeeded(taskId: Long, title: String, dueAt: Long?, reminderOffsetMinutes: Int?): Boolean {
        return if (dueAt != null && reminderOffsetMinutes != null && reminderOffsetMinutes > 0) {
            val scheduled = reminderScheduler.schedule(taskId, title, dueAt, reminderOffsetMinutes)
            if (!scheduled) {
                // This happens when reminder time is in the past
                throw IllegalArgumentException("Reminder time must be in the future. Please set a due date that allows for the selected reminder timing.")
            }
            scheduled
        } else {
            true
        }
    }
}