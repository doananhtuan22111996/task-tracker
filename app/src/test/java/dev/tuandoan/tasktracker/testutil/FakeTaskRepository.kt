package dev.tuandoan.tasktracker.testutil

import dev.tuandoan.tasktracker.data.database.DailyCount
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * In-memory fake implementation of [ITaskRepository] for JVM unit tests.
 * Maintains internal state via a [MutableStateFlow] for reactive observation.
 */
class FakeTaskRepository : ITaskRepository {

    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L

    /** Expose internal state for test assertions */
    fun getAllTasksSnapshot(): List<Task> = tasks.value

    /** Seed the fake with initial data */
    fun seed(vararg taskList: Task) {
        tasks.value = taskList.toList()
        nextId = (taskList.maxOfOrNull { it.id } ?: 0L) + 1
    }

    override fun getAllTasks(): Flow<List<Task>> = tasks.map { list -> list.filter { !it.isArchived } }

    override suspend fun getTaskById(id: Long): Task? = tasks.value.find { it.id == id }

    override suspend fun insertTask(task: Task): Long {
        val id = if (task.id == 0L) nextId++ else task.id
        val newTask = task.copy(id = id)
        tasks.value = tasks.value + newTask
        return id
    }

    override suspend fun updateTask(task: Task) {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
    }

    override suspend fun deleteTask(task: Task) {
        tasks.value = tasks.value.filter { it.id != task.id }
    }

    override suspend fun upsert(task: Task) {
        val existing = tasks.value.find { it.id == task.id }
        if (existing != null) {
            tasks.value = tasks.value.map { if (it.id == task.id) task else it }
        } else {
            tasks.value = tasks.value + task
        }
    }

    override fun getActiveTasks(): Flow<List<Task>> =
        tasks.map { list -> list.filter { !it.isCompleted && !it.isArchived } }

    override fun getCompletedTasks(): Flow<List<Task>> =
        tasks.map { list -> list.filter { it.isCompleted && !it.isArchived } }

    override suspend fun toggleTaskCompletion(task: Task) {
        val updated = task.copy(isCompleted = !task.isCompleted)
        updateTask(updated)
    }

    override suspend fun markCompleted(ids: List<Long>) {
        val now = System.currentTimeMillis()
        tasks.value = tasks.value.map {
            if (it.id in ids && !it.isArchived) it.copy(isCompleted = true, completedAt = now) else it
        }
    }

    override suspend fun markActive(ids: List<Long>) {
        tasks.value = tasks.value.map {
            if (it.id in ids && !it.isArchived) it.copy(isCompleted = false, completedAt = null) else it
        }
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        tasks.value = tasks.value.filter { it.id !in ids }
    }

    override suspend fun getTasksByIds(ids: List<Long>): List<Task> = tasks.value.filter { it.id in ids }

    override suspend fun upsertAll(tasks: List<Task>) {
        val existingIds = this.tasks.value.map { it.id }.toSet()
        val updated = this.tasks.value.toMutableList()
        for (task in tasks) {
            if (task.id in existingIds) {
                val index = updated.indexOfFirst { it.id == task.id }
                if (index >= 0) updated[index] = task
            } else {
                updated.add(task)
            }
        }
        this.tasks.value = updated
    }

    override suspend fun setPinned(taskId: Long, pinned: Boolean) {
        tasks.value = tasks.value.map { if (it.id == taskId) it.copy(isPinned = pinned) else it }
    }

    override suspend fun setPriority(taskId: Long, priority: Int) {
        tasks.value = tasks.value.map { if (it.id == taskId) it.copy(priority = priority) else it }
    }

    override fun getArchivedTasks(): Flow<List<Task>> = tasks.map { list -> list.filter { it.isArchived } }

    override suspend fun archiveTask(taskId: Long) {
        val now = System.currentTimeMillis()
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(isArchived = true, archivedAt = now) else it
        }
    }

    override suspend fun unarchiveTask(taskId: Long) {
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(isArchived = false, archivedAt = null) else it
        }
    }

    override suspend fun archiveTasks(ids: List<Long>) {
        val now = System.currentTimeMillis()
        tasks.value = tasks.value.map {
            if (it.id in ids) it.copy(isArchived = true, archivedAt = now) else it
        }
    }

    override suspend fun unarchiveTasks(ids: List<Long>) {
        tasks.value = tasks.value.map {
            if (it.id in ids) it.copy(isArchived = false, archivedAt = null) else it
        }
    }

    override suspend fun hardDeleteTask(taskId: Long) {
        tasks.value = tasks.value.filter { it.id != taskId }
    }

    override suspend fun hardDeleteTasks(ids: List<Long>) {
        tasks.value = tasks.value.filter { it.id !in ids }
    }

    override fun observeActiveCount(): Flow<Int> =
        tasks.map { list -> list.count { !it.isCompleted && !it.isArchived } }

    override fun observeCompletedCount(): Flow<Int> =
        tasks.map { list -> list.count { it.isCompleted && !it.isArchived } }

    override fun observeCompletedTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> =
        tasks.map { list ->
            list.count {
                it.isCompleted &&
                    !it.isArchived &&
                    it.completedAt != null &&
                    it.completedAt in startOfDayMillis until endOfDayMillis
            }
        }

    override fun observeDueTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> = tasks.map { list ->
        list.count {
            !it.isCompleted &&
                !it.isArchived &&
                it.dueAt != null &&
                it.dueAt in startOfDayMillis until endOfDayMillis
        }
    }

    override fun observeOverdueCount(nowMillis: Long): Flow<Int> = tasks.map { list ->
        list.count {
            !it.isCompleted &&
                !it.isArchived &&
                it.dueAt != null &&
                it.dueAt < nowMillis
        }
    }

    override fun observeCompletedCountPerDay(startMillis: Long, endMillis: Long): Flow<List<DailyCount>> =
        tasks.map { list ->
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            list.filter {
                it.isCompleted &&
                    !it.isArchived &&
                    it.completedAt != null &&
                    it.completedAt in startMillis until endMillis
            }.groupBy { task ->
                Instant.ofEpochMilli(task.completedAt!!)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(formatter)
            }.map { (date, tasks) -> DailyCount(date = date, count = tasks.size) }
                .sortedBy { it.date }
        }

    override suspend fun getLatestGeneratedTask(parentRecurringTaskId: Long): Task? = tasks.value
        .filter { it.parentRecurringTaskId == parentRecurringTaskId && !it.isCompleted && !it.isArchived }
        .maxByOrNull { it.createdAt }

    override suspend fun getAllTasksIncludingArchived(): List<Task> = tasks.value

    override suspend fun replaceAllTasks(tasks: List<Task>) {
        this.tasks.value = tasks
    }
}
