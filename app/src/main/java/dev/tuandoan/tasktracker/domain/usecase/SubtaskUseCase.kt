package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Subtask
import dev.tuandoan.tasktracker.data.database.SubtaskProgress
import dev.tuandoan.tasktracker.diagnostics.AnalyticsEvent
import dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger
import dev.tuandoan.tasktracker.domain.repository.ISubtaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure-domain coordinator for subtask operations. Validates input (title length, blank,
 * ordered-id set match) and delegates storage to [ISubtaskRepository].
 *
 * Mutating operations return [Result] so the UI can surface validation errors without
 * exceptions propagating through flows.
 */
@Singleton
class SubtaskUseCase @Inject constructor(
    private val repository: ISubtaskRepository,
    private val analyticsLogger: AnalyticsLogger,
) {

    fun observeSubtasks(taskId: Long): Flow<List<Subtask>> = repository.observeSubtasks(taskId)

    /**
     * Live map of [SubtaskProgress] keyed by `taskId`. Tasks with no subtasks are absent — the
     * UI treats a missing entry as "no indicator". Suitable for O(1) per-row lookup on the list.
     */
    fun observeProgressByTaskId(): Flow<Map<Long, SubtaskProgress>> =
        repository.observeSubtaskProgress().map { list -> list.associateBy { it.taskId } }

    suspend fun addSubtask(taskId: Long, title: String): Result<Long> {
        val normalized = title.trim()
        if (normalized.isEmpty()) {
            return Result.failure(IllegalArgumentException("Subtask title cannot be blank"))
        }
        if (normalized.length > MAX_TITLE_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Subtask title must be ≤ $MAX_TITLE_LENGTH characters"),
            )
        }
        val nextOrder = repository.getSubtasks(taskId).size
        return runCatchingCancellable {
            repository.insertSubtask(
                Subtask(taskId = taskId, title = normalized, sortOrder = nextOrder),
            )
        }.also { result ->
            // FB-14: fire only on success so validation-failure paths above don't skew the metric.
            if (result.isSuccess) analyticsLogger.log(AnalyticsEvent.SubtaskAdded)
        }
    }

    suspend fun updateTitle(subtaskId: Long, title: String): Result<Unit> {
        val normalized = title.trim()
        if (normalized.isEmpty()) {
            return Result.failure(IllegalArgumentException("Subtask title cannot be blank"))
        }
        if (normalized.length > MAX_TITLE_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Subtask title must be ≤ $MAX_TITLE_LENGTH characters"),
            )
        }
        val existing = repository.getSubtaskById(subtaskId)
            ?: return Result.failure(IllegalArgumentException("Subtask $subtaskId not found"))
        return runCatchingCancellable { repository.updateSubtask(existing.copy(title = normalized)) }
    }

    suspend fun setCompleted(subtaskId: Long, completed: Boolean): Result<Unit> {
        val existing = repository.getSubtaskById(subtaskId)
            ?: return Result.failure(IllegalArgumentException("Subtask $subtaskId not found"))
        if (existing.isCompleted == completed) return Result.success(Unit)
        return runCatchingCancellable { repository.updateSubtask(existing.copy(isCompleted = completed)) }
    }

    suspend fun delete(subtaskId: Long): Result<Unit> = runCatchingCancellable { repository.deleteById(subtaskId) }

    suspend fun reorder(taskId: Long, orderedIds: List<Long>): Result<Unit> =
        runCatchingCancellable { repository.reorderSubtasks(taskId, orderedIds) }

    /**
     * Resets all subtasks under [taskId] to unchecked. Used by recurrence regeneration
     * (ST-04) so regenerated task instances start with a fresh checklist.
     */
    suspend fun resetCompletion(taskId: Long): Result<Unit> =
        runCatchingCancellable { repository.resetCompletionForTask(taskId) }

    companion object {
        const val MAX_TITLE_LENGTH = 500
    }
}

/**
 * Like [runCatching] but re-throws [CancellationException] so coroutine cancellation propagates
 * naturally through structured concurrency. Without this, a cancelled parent scope would see
 * a `Result.failure(CancellationException)` instead of the child being cancelled.
 */
private inline fun <R> runCatchingCancellable(block: () -> R): Result<R> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c
} catch (t: Throwable) {
    Result.failure(t)
}
