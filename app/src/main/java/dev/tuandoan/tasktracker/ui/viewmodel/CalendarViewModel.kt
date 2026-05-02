package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import dev.tuandoan.tasktracker.domain.usecase.CalendarUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * Drives the v1.11.0 Calendar screen. Holds the visible month + selected day and exposes the
 * per-day [DayDecoration] map from [CalendarUseCase] for the currently visible month.
 *
 * Both `visibleMonth` and `selectedDay` survive process death via [SavedStateHandle] keys
 * `calendar_visible_month` (ISO `YYYY-MM`) and `calendar_selected_day` (ISO `YYYY-MM-DD`).
 * On first launch without saved state, both default to today.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarUseCase: CalendarUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val _visibleMonth: MutableStateFlow<YearMonth> = MutableStateFlow(
        parseOrNull(savedStateHandle.get<String>(KEY_VISIBLE_MONTH), YearMonth::parse)
            ?: YearMonth.now(zone),
    )

    private val _selectedDay: MutableStateFlow<LocalDate> = MutableStateFlow(
        parseOrNull(savedStateHandle.get<String>(KEY_SELECTED_DAY), LocalDate::parse)
            ?: LocalDate.now(zone),
    )

    val visibleMonth: StateFlow<YearMonth> = _visibleMonth
    val selectedDay: StateFlow<LocalDate> = _selectedDay

    @OptIn(ExperimentalCoroutinesApi::class)
    private val decorationsFlow: Flow<Map<LocalDate, DayDecoration>> =
        _visibleMonth.flatMapLatest { month ->
            calendarUseCase.observeMonthDecorations(
                monthStart = month.atDay(1),
                monthEnd = month.atEndOfMonth(),
                zone = zone,
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedDayTasksFlow: Flow<List<Task>> =
        _selectedDay.flatMapLatest { day ->
            calendarUseCase.observeTasksForDay(day, zone)
        }

    val uiState: StateFlow<CalendarUiState> = combine(
        _visibleMonth,
        _selectedDay,
        decorationsFlow,
        selectedDayTasksFlow,
    ) { month, day, decorations, dayTasks ->
        CalendarUiState(
            visibleMonth = month,
            selectedDay = day,
            decorations = decorations,
            selectedDayTasks = dayTasks,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MS),
        initialValue = CalendarUiState(
            visibleMonth = _visibleMonth.value,
            selectedDay = _selectedDay.value,
        ),
    )

    fun onMonthChange(delta: Int) {
        val next = _visibleMonth.value.plusMonths(delta.toLong())
        _visibleMonth.value = next
        savedStateHandle[KEY_VISIBLE_MONTH] = next.toString()
    }

    fun onJumpToMonth(target: YearMonth) {
        _visibleMonth.value = target
        savedStateHandle[KEY_VISIBLE_MONTH] = target.toString()
    }

    fun onDaySelect(date: LocalDate) {
        _selectedDay.value = date
        savedStateHandle[KEY_SELECTED_DAY] = date.toString()
    }

    fun onTodayClick() {
        val today = LocalDate.now(zone)
        val thisMonth = YearMonth.from(today)
        _visibleMonth.value = thisMonth
        _selectedDay.value = today
        savedStateHandle[KEY_VISIBLE_MONTH] = thisMonth.toString()
        savedStateHandle[KEY_SELECTED_DAY] = today.toString()
    }

    companion object {
        const val KEY_VISIBLE_MONTH = "calendar_visible_month"
        const val KEY_SELECTED_DAY = "calendar_selected_day"
        private const val STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

        /**
         * Parses [raw] using [parser] and returns `null` on any failure (null input,
         * malformed string). Keeps construction crash-free when SavedStateHandle holds a
         * corrupt value from a prior process or schema change.
         */
        private inline fun <T> parseOrNull(raw: String?, parser: (String) -> T): T? {
            if (raw == null) return null
            return runCatching { parser(raw) }.getOrNull()
        }
    }
}

/**
 * Immutable snapshot rendered by [dev.tuandoan.tasktracker.ui.screens.CalendarScreen].
 * A day without an entry in [decorations] has no tasks — the UI treats a missing key as empty.
 */
data class CalendarUiState(
    val visibleMonth: YearMonth,
    val selectedDay: LocalDate,
    val decorations: Map<LocalDate, DayDecoration> = emptyMap(),
    val selectedDayTasks: List<Task> = emptyList(),
)
