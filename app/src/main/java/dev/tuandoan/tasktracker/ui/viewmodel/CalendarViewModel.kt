package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.tasktracker.data.database.SubtaskProgress
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import dev.tuandoan.tasktracker.domain.usecase.CalendarUseCase
import dev.tuandoan.tasktracker.domain.usecase.SubtaskUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * Drives the v1.11.0 Calendar screen. Exposes [CalendarUiState] combining:
 * - `visibleMonth` + `selectedDay` (persisted via [SavedStateHandle] — ISO `YYYY-MM` and
 *   `YYYY-MM-DD` keys respectively; both default to today on first launch).
 * - `decorations` — per-day aggregates for the visible month (CAL-05).
 * - `selectedDayTasks` — live list of tasks on `selectedDay`, for the day agenda sheet (CAL-17).
 * - `subtaskProgress` — `Map<Long, SubtaskProgress>` feeding agenda rows' progress indicator
 *   (CAL-18).
 *
 * Events:
 * - Navigation: `onMonthChange` / `onJumpToMonth` / `onDaySelect` / `onTodayClick`.
 * - Agenda-row actions (CAL-18): `onToggleTaskComplete` / `onArchiveTask` / `onTogglePin`.
 *   All delegate to [ITaskManager] on [viewModelScope]; duplicate + skip-occurrence stay on
 *   the main task list for now (wired as `{}` defaults in `TaskItem`).
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarUseCase: CalendarUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val taskManager: ITaskManager,
    private val subtaskUseCase: SubtaskUseCase,
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

    private val subtaskProgressFlow: Flow<Map<Long, SubtaskProgress>> =
        subtaskUseCase.observeProgressByTaskId()

    val uiState: StateFlow<CalendarUiState> = combine(
        _visibleMonth,
        _selectedDay,
        decorationsFlow,
        selectedDayTasksFlow,
        subtaskProgressFlow,
    ) { month, day, decorations, dayTasks, progress ->
        CalendarUiState(
            visibleMonth = month,
            selectedDay = day,
            decorations = decorations,
            selectedDayTasks = dayTasks,
            subtaskProgress = progress,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MS),
        initialValue = CalendarUiState(
            visibleMonth = _visibleMonth.value,
            selectedDay = _selectedDay.value,
        ),
    )

    // ── Agenda-row actions (CAL-18). All delegate to ITaskManager on viewModelScope. ──

    fun onToggleTaskComplete(task: Task) {
        viewModelScope.launch { taskManager.toggleTaskCompletion(task) }
    }

    fun onArchiveTask(task: Task) {
        viewModelScope.launch { taskManager.archiveTask(task.id) }
    }

    fun onTogglePin(task: Task) {
        viewModelScope.launch { taskManager.setPinned(task.id, !task.isPinned) }
    }

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
    val subtaskProgress: Map<Long, SubtaskProgress> = emptyMap(),
)
