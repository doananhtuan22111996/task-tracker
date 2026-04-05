package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.data.database.DailyCount
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.data.preferences.UserPreferences
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.model.StreakStats
import dev.tuandoan.tasktracker.domain.usecase.StreakUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val taskManager: ITaskManager,
    private val settingsRepository: SettingsRepository,
    private val streakUseCase: StreakUseCase,
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun setTipStatsCardsShown() {
        viewModelScope.launch { settingsRepository.setTipShown(SettingsRepository.TipKeys.STATS_CARDS) }
    }

    data class StatsUiState(
        val activeCount: Int = 0,
        val completedCount: Int = 0,
        val completedTodayCount: Int = 0,
        val dueTodayCount: Int = 0,
        val overdueCount: Int = 0,
    )

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

    // SPEC-S03: Daily progress
    val dailyProgress: StateFlow<Float> = uiState
        .map { state ->
            if (state.dueTodayCount == 0) {
                1f
            } else {
                (state.completedTodayCount.toFloat() / state.dueTodayCount.toFloat()).coerceIn(0f, 1f)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // SPEC-S04: Weekly breakdown
    val weeklyBreakdown: StateFlow<List<DailyCount>> = taskManager
        .observeCompletedCountPerDay(getWeekStartMillis(), getTodayEndMillis())
        .map { dbCounts -> fillMissingDays(dbCounts) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SPEC-S05: Completion rate
    val completionRate: StateFlow<Int?> = uiState
        .map { state ->
            val total = state.completedCount + state.activeCount
            if (total == 0) {
                null
            } else {
                (state.completedCount * 100 / total)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // SPEC-S07: Empty state
    val isEmpty: StateFlow<Boolean> = uiState
        .map { state -> state.activeCount == 0 && state.completedCount == 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Streak stats
    private val _streakStats =
        MutableStateFlow(StreakStats(activeRecurringCount = 0, bestCurrentStreak = null, allTimeBestStreak = null))
    val streakStats: StateFlow<StreakStats> = _streakStats.asStateFlow()

    init {
        loadStreakStats()
    }

    fun loadStreakStats() {
        viewModelScope.launch {
            _streakStats.value = streakUseCase.getStreakStats()
        }
    }

    private fun getTodayStartMillis(): Long {
        val today = LocalDate.now()
        return today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun getTodayEndMillis(): Long {
        val tomorrow = LocalDate.now().plusDays(1)
        return tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun getWeekStartMillis(): Long {
        val weekStart = LocalDate.now().minusDays(6)
        return weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun fillMissingDays(dbCounts: List<DailyCount>): List<DailyCount> {
        val today = LocalDate.now()
        val countMap = dbCounts.associate { it.date to it.count }
        return (0..6).map { daysAgo ->
            val date = today.minusDays((6 - daysAgo).toLong())
            val dateStr = date.toString()
            val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            DailyCount(date = dayLabel, count = countMap[dateStr] ?: 0)
        }
    }
}
