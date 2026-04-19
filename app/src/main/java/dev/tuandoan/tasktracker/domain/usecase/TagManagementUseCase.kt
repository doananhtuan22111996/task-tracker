package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.domain.model.TagItem
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagManagementUseCase @Inject constructor(private val repository: ITaskRepository) {

    fun observeTags(): Flow<List<TagItem>> = repository.getDistinctTagsWithCount().map { tags ->
        tags.map { TagItem(name = it.tag, color = it.tagColor, taskCount = it.taskCount) }
    }

    suspend fun renameTag(oldName: String, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Tag name cannot be blank"))
        if (trimmed.length > MAX_TAG_LENGTH) {
            return Result.failure(IllegalArgumentException("Tag must be ≤ $MAX_TAG_LENGTH characters"))
        }
        return runCatching { repository.updateTagName(oldName, trimmed) }
    }

    suspend fun deleteTag(tagName: String): Result<Unit> = runCatching {
        repository.clearTag(tagName)
    }

    suspend fun updateTagColor(tagName: String, color: String?): Result<Unit> = runCatching {
        repository.updateTagColor(tagName, color)
    }

    suspend fun getTagColor(tagName: String): String? = repository.getTagColor(tagName)

    companion object {
        const val MAX_TAG_LENGTH = 30
    }
}
