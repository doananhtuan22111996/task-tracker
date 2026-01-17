package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.domain.ITaskManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * ViewModel for the Stats screen showing task statistics.
 *
 * Provides lightweight numeric stats for user progress tracking:
 * - Active tasks count
 * - Completed tasks count (overall)
 * - Completed today count
 * - Due today count (active tasks only)
 * - Overdue count
 *
 * All stats exclude archived tasks by default.
 * Uses local timezone for "today" calculations.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(private val taskManager: ITaskManager) : ViewModel() {

    /**
     * UI state for stats screen containing all statistical counts
     */
    data class StatsUiState(
        val activeCount: Int = 0,
        val completedCount: Int = 0,
        val completedTodayCount: Int = 0,
        val dueTodayCount: Int = 0,
        val overdueCount: Int = 0,
    )

    /**
     * Combined UI state that updates reactively when task data changes
     */
    val uiState: StateFlow<StatsUiState> = combine(
        taskManager.observeActiveCount(),
        taskManager.observeCompletedCount(),
        taskManager.observeCompletedTodayCount(getTodayStartMillis(), getTodayEndMillis()),
        taskManager.observeDueTodayCount(getTodayStartMillis(), getTodayEndMillis()),
        taskManager.observeOverdueCount(System.currentTimeMillis()),
    ) { activeCount, completedCount, completedTodayCount, dueTodayCount, overdueCount ->
        StatsUiState(
            activeCount = activeCount,
            completedCount = completedCount,
            completedTodayCount = completedTodayCount,
            dueTodayCount = dueTodayCount,
            overdueCount = overdueCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState(),
    )

    /**
     * Get start of today in milliseconds (00:00:00) in local timezone
     */
    private fun getTodayStartMillis(): Long {
        val today = LocalDate.now()
        return today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Get end of today in milliseconds (23:59:59.999) in local timezone
     */
    private fun getTodayEndMillis(): Long {
        val tomorrow = LocalDate.now().plusDays(1)
        return tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
