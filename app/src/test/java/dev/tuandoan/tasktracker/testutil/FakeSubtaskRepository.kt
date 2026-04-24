package dev.tuandoan.tasktracker.testutil

import dev.tuandoan.tasktracker.data.database.Subtask
import dev.tuandoan.tasktracker.domain.repository.ISubtaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake implementation of [ISubtaskRepository] for JVM unit tests.
 * Maintains internal state via a [MutableStateFlow] for reactive observation.
 */
class FakeSubtaskRepository : ISubtaskRepository {

    private val subtasks = MutableStateFlow<List<Subtask>>(emptyList())
    private var nextId = 1L

    /** Expose internal state for test assertions */
    fun getAllSubtasksSnapshot(): List<Subtask> = subtasks.value

    /** Seed the fake with initial data */
    fun seed(vararg items: Subtask) {
        subtasks.value = items.toList()
        nextId = (items.maxOfOrNull { it.id } ?: 0L) + 1
    }

    override fun observeSubtasks(taskId: Long): Flow<List<Subtask>> = subtasks.map { list ->
        list.filter { it.taskId == taskId }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
    }

    override suspend fun getSubtasks(taskId: Long): List<Subtask> =
        subtasks.value.filter { it.taskId == taskId }.sortedWith(compareBy({ it.sortOrder }, { it.id }))

    override suspend fun getSubtaskById(id: Long): Subtask? = subtasks.value.firstOrNull { it.id == id }

    override suspend fun insertSubtask(subtask: Subtask): Long {
        val id = if (subtask.id == 0L) nextId++ else subtask.id
        val newSubtask = subtask.copy(id = id)
        subtasks.value = subtasks.value + newSubtask
        return id
    }

    override suspend fun updateSubtask(subtask: Subtask) {
        subtasks.value = subtasks.value.map { if (it.id == subtask.id) subtask else it }
    }

    override suspend fun deleteById(id: Long) {
        subtasks.value = subtasks.value.filter { it.id != id }
    }

    override suspend fun deleteByTaskId(taskId: Long) {
        subtasks.value = subtasks.value.filter { it.taskId != taskId }
    }

    override suspend fun resetCompletionForTask(taskId: Long) {
        subtasks.value = subtasks.value.map {
            if (it.taskId == taskId) it.copy(isCompleted = false) else it
        }
    }

    override suspend fun countForTask(taskId: Long): Int = subtasks.value.count { it.taskId == taskId }

    override suspend fun upsertAll(subtasks: List<Subtask>) {
        val existingIds = this.subtasks.value.map { it.id }.toSet()
        val updated = this.subtasks.value.toMutableList()
        for (item in subtasks) {
            if (item.id in existingIds) {
                val index = updated.indexOfFirst { it.id == item.id }
                if (index >= 0) updated[index] = item
            } else {
                updated.add(item)
            }
        }
        this.subtasks.value = updated
    }

    override suspend fun reorderSubtasks(taskId: Long, orderedIds: List<Long>) {
        val current = subtasks.value.filter { it.taskId == taskId }
        val currentIds = current.map { it.id }.toSet()
        val requestedIds = orderedIds.toSet()
        require(currentIds == requestedIds && orderedIds.size == currentIds.size) {
            "reorderSubtasks: orderedIds must match the current subtask id set for task $taskId"
        }
        val byId = current.associateBy { it.id }
        val reordered = orderedIds.mapIndexed { index, id -> byId.getValue(id).copy(sortOrder = index) }
        val untouched = subtasks.value.filter { it.taskId != taskId }
        subtasks.value = untouched + reordered
    }

    override suspend fun copySubtasksResetCompletion(fromTaskId: Long, toTaskId: Long) {
        val source = subtasks.value.filter { it.taskId == fromTaskId }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
        if (source.isEmpty()) return
        val copies = source.map { original ->
            val newId = nextId++
            original.copy(id = newId, taskId = toTaskId, isCompleted = false)
        }
        subtasks.value = subtasks.value + copies
    }

    override suspend fun getAllSubtasks(): List<Subtask> =
        subtasks.value.sortedWith(compareBy({ it.taskId }, { it.sortOrder }, { it.id }))
}
