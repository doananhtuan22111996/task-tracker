package dev.tuandoan.tasktracker.domain

import dev.tuandoan.tasktracker.data.database.DailyCount
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import dev.tuandoan.tasktracker.domain.scheduler.TaskReminderScheduler
import dev.tuandoan.tasktracker.domain.scheduler.WidgetUpdater
import dev.tuandoan.tasktracker.domain.service.RecurrenceCalculator
import dev.tuandoan.tasktracker.domain.service.TagNormalizer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of ITaskManager that provides business logic for task operations.
 * Uses @Inject constructor for Hilt dependency injection.
 * @Singleton ensures single instance across the app lifecycle.
 */
class TaskManager @Inject constructor(
    private val repository: ITaskRepository,
    private val reminderScheduler: TaskReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
) : ITaskManager {

    // Data access
    override fun getAllTasks(): Flow<List<Task>> = repository.getAllTasks()

    override suspend fun getTaskById(id: Long): Task? = repository.getTaskById(id)

    // Task operations
    override suspend fun createTask(title: String, description: String): Long =
        createTask(title, description, null, null)

    override suspend fun createTask(
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
    ): Long = createTask(title, description, dueAt, false, reminderOffsetMinutes, null)

    override suspend fun createTask(
        title: String,
        description: String,
        dueAt: Long?,
        dueAtHasTime: Boolean,
        reminderOffsetMinutes: Int?,
        tag: String?,
    ): Long = createTask(title, description, dueAt, dueAtHasTime, reminderOffsetMinutes, tag, 0, 1, 0, null)

    override suspend fun createTask(
        title: String,
        description: String,
        dueAt: Long?,
        dueAtHasTime: Boolean,
        reminderOffsetMinutes: Int?,
        tag: String?,
        recurrenceType: Int,
        recurrenceInterval: Int,
        recurrenceDaysOfWeek: Int,
        recurrenceEndDate: Long?,
    ): Long {
        require(title.isNotBlank()) { "Task title cannot be blank" }

        val task = Task(
            title = title.trim(),
            description = description.trim(),
            dueAt = dueAt,
            dueAtHasTime = dueAtHasTime,
            reminderOffsetMinutes = reminderOffsetMinutes,
            tag = TagNormalizer.normalize(tag),
            recurrenceType = recurrenceType,
            recurrenceInterval = recurrenceInterval,
            recurrenceDaysOfWeek = recurrenceDaysOfWeek,
            recurrenceEndDate = recurrenceEndDate,
        )
        val taskId = repository.insertTask(task)

        // Schedule reminder if applicable
        scheduleReminderIfNeeded(taskId, title.trim(), dueAt, reminderOffsetMinutes)
        widgetUpdater.requestUpdate()

        return taskId
    }

    override suspend fun updateTask(task: Task) {
        val normalizedTag = TagNormalizer.normalize(task.tag)
        // Invariant: no orphan tagColor. Same rule as v10→v11 migration and backup import.
        val normalizedTask = task.copy(
            tag = normalizedTag,
            tagColor = if (normalizedTag == null) null else task.tagColor,
        )
        val existingTask = repository.getTaskById(normalizedTask.id)
        repository.updateTask(normalizedTask)

        // Handle reminder rescheduling if due date or reminder changed
        if (existingTask != null) {
            val dueDateChanged = existingTask.dueAt != normalizedTask.dueAt
            val reminderChanged = existingTask.reminderOffsetMinutes != normalizedTask.reminderOffsetMinutes

            if (dueDateChanged || reminderChanged) {
                // Cancel existing reminder
                reminderScheduler.cancel(normalizedTask.id)

                // Schedule new reminder if task is not completed
                if (!normalizedTask.isCompleted) {
                    scheduleReminderIfNeeded(
                        normalizedTask.id,
                        normalizedTask.title,
                        normalizedTask.dueAt,
                        normalizedTask.reminderOffsetMinutes,
                    )
                }
            }
        }
        widgetUpdater.requestUpdate()
    }

    override suspend fun updateTaskContent(taskId: Long, title: String, description: String) {
        updateTaskContent(taskId, title, description, null, false, null, null)
    }

    override suspend fun updateTaskContent(
        taskId: Long,
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
    ): Boolean = updateTaskContent(taskId, title, description, dueAt, false, reminderOffsetMinutes, null)

    override suspend fun updateTaskContent(
        taskId: Long,
        title: String,
        description: String,
        dueAt: Long?,
        dueAtHasTime: Boolean,
        reminderOffsetMinutes: Int?,
        tag: String?,
    ): Boolean {
        require(title.isNotBlank()) { "Task title cannot be blank" }

        val existingTask = repository.getTaskById(taskId)
        requireNotNull(existingTask) { "Task with id $taskId not found" }

        val normalizedTag = TagNormalizer.normalize(tag)
        val updatedTask = existingTask.copy(
            title = title.trim(),
            description = description.trim(),
            dueAt = dueAt,
            dueAtHasTime = dueAtHasTime,
            reminderOffsetMinutes = reminderOffsetMinutes,
            tag = normalizedTag,
            tagColor = if (normalizedTag == null) null else existingTask.tagColor,
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
                val result = scheduleReminderIfNeeded(taskId, title.trim(), dueAt, reminderOffsetMinutes)
                widgetUpdater.requestUpdate()
                return result
            }
        }

        widgetUpdater.requestUpdate()
        return true
    }

    override suspend fun deleteTask(task: Task) {
        // Cancel any pending reminder before deleting
        reminderScheduler.cancel(task.id)
        repository.deleteTask(task)
        widgetUpdater.requestUpdate()
    }

    override suspend fun restoreTask(task: Task): Result<Unit> = try {
        repository.upsert(task)
        // Reschedule reminder if task is not completed and has reminder settings
        if (!task.isCompleted) {
            scheduleReminderIfNeeded(task.id, task.title, task.dueAt, task.reminderOffsetMinutes)
        }
        widgetUpdater.requestUpdate()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun toggleTaskCompletion(task: Task) {
        val currentTime = System.currentTimeMillis()
        val isCompleting = !task.isCompleted
        val updatedTask = task.copy(
            isCompleted = isCompleting,
            completedAt = if (isCompleting) currentTime else null,
        )

        if (isCompleting) {
            // Cancel reminder when task is completed
            reminderScheduler.cancel(task.id)
            // Atomically complete + generate next occurrence
            completeAndGenerateNext(task, updatedTask, currentTime)
        } else {
            // Atomically un-complete + delete generated next instance
            uncompleteAndDeleteGenerated(task, updatedTask)
            // Reschedule reminder when task is marked incomplete
            scheduleReminderIfNeeded(task.id, task.title, task.dueAt, task.reminderOffsetMinutes)
        }
        widgetUpdater.requestUpdate()
    }

    override suspend fun skipOccurrence(task: Task) {
        if (RecurrenceType.fromValue(task.recurrenceType) == RecurrenceType.NONE) return

        val currentTime = System.currentTimeMillis()
        reminderScheduler.cancel(task.id)
        // Atomically archive + generate next occurrence
        archiveAndGenerateNext(task, currentTime)
        widgetUpdater.requestUpdate()
    }

    override suspend fun markTaskComplete(task: Task) {
        if (!task.isCompleted) {
            val currentTime = System.currentTimeMillis()
            val completedTask = task.copy(isCompleted = true, completedAt = currentTime)
            repository.updateTask(completedTask)
            // Cancel reminder when marking as complete
            reminderScheduler.cancel(task.id)
            widgetUpdater.requestUpdate()
        }
    }

    override suspend fun markTaskIncomplete(task: Task) {
        if (task.isCompleted) {
            val incompleteTask = task.copy(isCompleted = false, completedAt = null)
            repository.updateTask(incompleteTask)
            // Reschedule reminder when marking as incomplete
            scheduleReminderIfNeeded(task.id, task.title, task.dueAt, task.reminderOffsetMinutes)
            widgetUpdater.requestUpdate()
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
        widgetUpdater.requestUpdate()
    }

    override suspend fun deleteTasksByIds(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            // Cancel reminders for all deleted tasks
            ids.forEach { taskId ->
                reminderScheduler.cancel(taskId)
            }
            repository.deleteByIds(ids)
            widgetUpdater.requestUpdate()
        }
    }

    override suspend fun restoreTasks(tasks: List<Task>): Result<Unit> = try {
        if (tasks.isNotEmpty()) {
            repository.upsertAll(tasks)
            // Reschedule reminders for all restored incomplete tasks
            tasks.filter { !it.isCompleted }.forEach { task ->
                scheduleReminderIfNeeded(task.id, task.title, task.dueAt, task.reminderOffsetMinutes)
            }
            widgetUpdater.requestUpdate()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Filtered data access
    override fun getActiveTasks(): Flow<List<Task>> = repository.getActiveTasks()

    override fun getCompletedTasks(): Flow<List<Task>> = repository.getCompletedTasks()

    // Pin/Priority operations
    override suspend fun setPinned(taskId: Long, pinned: Boolean) {
        repository.setPinned(taskId, pinned)
        widgetUpdater.requestUpdate()
    }

    override suspend fun setPriority(taskId: Long, priority: Int) {
        repository.setPriority(taskId, priority)
        widgetUpdater.requestUpdate()
    }

    // Archive operations
    override fun getArchivedTasks(): Flow<List<Task>> = repository.getArchivedTasks()

    override suspend fun archiveTask(taskId: Long) {
        // Cancel any pending reminder before archiving
        reminderScheduler.cancel(taskId)
        repository.archiveTask(taskId)
        widgetUpdater.requestUpdate()
    }

    override suspend fun unarchiveTask(taskId: Long) {
        repository.unarchiveTask(taskId)

        // Reschedule reminder if task is not completed and has reminder settings
        val task = repository.getTaskById(taskId)
        if (task != null && !task.isCompleted) {
            scheduleReminderIfNeeded(taskId, task.title, task.dueAt, task.reminderOffsetMinutes)
        }
        widgetUpdater.requestUpdate()
    }

    override suspend fun archiveTasks(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            // Cancel reminders for all archived tasks
            ids.forEach { taskId ->
                reminderScheduler.cancel(taskId)
            }
            repository.archiveTasks(ids)
            widgetUpdater.requestUpdate()
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
            widgetUpdater.requestUpdate()
        }
    }

    override suspend fun hardDeleteTask(taskId: Long) {
        // Cancel any pending reminder before permanently deleting
        reminderScheduler.cancel(taskId)
        repository.hardDeleteTask(taskId)
        widgetUpdater.requestUpdate()
    }

    override suspend fun hardDeleteTasks(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            // Cancel reminders for all permanently deleted tasks
            ids.forEach { taskId ->
                reminderScheduler.cancel(taskId)
            }
            repository.hardDeleteTasks(ids)
            widgetUpdater.requestUpdate()
        }
    }

    // Stats operations (exclude archived tasks)
    override fun observeActiveCount(): Flow<Int> = repository.observeActiveCount()

    override fun observeCompletedCount(): Flow<Int> = repository.observeCompletedCount()

    override fun observeCompletedTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> =
        repository.observeCompletedTodayCount(startOfDayMillis, endOfDayMillis)

    override fun observeDueTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> =
        repository.observeDueTodayCount(startOfDayMillis, endOfDayMillis)

    override fun observeOverdueCount(nowMillis: Long): Flow<Int> = repository.observeOverdueCount(nowMillis)

    override fun observeCompletedCountPerDay(startMillis: Long, endMillis: Long): Flow<List<DailyCount>> =
        repository.observeCompletedCountPerDay(startMillis, endMillis)

    // Recurrence helpers

    private fun buildNextTask(task: Task, currentTime: Long): Task? {
        if (RecurrenceType.fromValue(task.recurrenceType) == RecurrenceType.NONE) return null

        val nextDueAt = RecurrenceCalculator.nextDueDate(task) ?: return null

        return task.copy(
            id = 0,
            isCompleted = false,
            completedAt = null,
            createdAt = currentTime,
            dueAt = nextDueAt,
            parentRecurringTaskId = task.parentRecurringTaskId ?: task.id,
            isArchived = false,
            archivedAt = null,
        )
    }

    private suspend fun completeAndGenerateNext(task: Task, completedTask: Task, currentTime: Long) {
        val nextTask = buildNextTask(task, currentTime)
        if (nextTask != null) {
            val newTaskId = repository.completeAndGenerateNext(completedTask, nextTask)
            scheduleReminderIfNeeded(newTaskId, nextTask.title, nextTask.dueAt, nextTask.reminderOffsetMinutes)
        } else {
            repository.updateTask(completedTask)
        }
    }

    private suspend fun uncompleteAndDeleteGenerated(task: Task, reactivatedTask: Task) {
        if (RecurrenceType.fromValue(task.recurrenceType) == RecurrenceType.NONE) {
            repository.updateTask(reactivatedTask)
            return
        }

        val parentId = task.parentRecurringTaskId ?: task.id
        val generatedTask = repository.getLatestGeneratedTask(parentId)
        if (generatedTask != null) {
            reminderScheduler.cancel(generatedTask.id)
            repository.uncompleteAndDeleteGenerated(reactivatedTask, generatedTask.id)
        } else {
            repository.updateTask(reactivatedTask)
        }
    }

    private suspend fun archiveAndGenerateNext(task: Task, currentTime: Long) {
        val nextTask = buildNextTask(task, currentTime)
        if (nextTask != null) {
            val newTaskId = repository.archiveAndGenerateNext(task.id, nextTask)
            scheduleReminderIfNeeded(newTaskId, nextTask.title, nextTask.dueAt, nextTask.reminderOffsetMinutes)
        } else {
            repository.archiveTask(task.id)
        }
    }

    // Helper method for scheduling reminders
    private suspend fun scheduleReminderIfNeeded(
        taskId: Long,
        title: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
    ): Boolean = if (dueAt != null && reminderOffsetMinutes != null && reminderOffsetMinutes > 0) {
        val scheduled = reminderScheduler.schedule(taskId, title, dueAt, reminderOffsetMinutes)
        if (!scheduled) {
            // This happens when reminder time is in the past
            throw IllegalArgumentException(
                "Reminder time must be in the future. Please set a due date that allows for the selected reminder timing.",
            )
        }
        scheduled
    } else {
        true
    }
}
