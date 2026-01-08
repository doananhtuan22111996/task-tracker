package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(FlowPreview::class)
@Singleton
class TaskSearchUseCase @Inject constructor() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Debounced search query to avoid excessive filtering
    val debouncedSearchQuery: Flow<String> = _searchQuery
        .debounce(350)
        .distinctUntilChanged()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Applies text search filter to a list of tasks
     * Searches in both title and description (case-insensitive)
     */
    fun filterTasksBySearch(tasks: List<Task>, query: String): List<Task> {
        val trimmedQuery = query.trim()

        return if (trimmedQuery.isEmpty()) {
            tasks
        } else {
            val lowercaseQuery = trimmedQuery.lowercase(Locale.getDefault())
            tasks.filter { task ->
                task.title.lowercase(Locale.getDefault()).contains(lowercaseQuery) ||
                task.description.lowercase(Locale.getDefault()).contains(lowercaseQuery)
            }
        }
    }

    /**
     * Check if search has active query
     */
    fun hasActiveSearch(): Flow<Boolean> = _searchQuery.map { it.isNotBlank() }
}