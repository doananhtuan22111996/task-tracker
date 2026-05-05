package dev.tuandoan.tasktracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.SubtaskProgress
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.model.AgendaItem
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import dev.tuandoan.tasktracker.domain.usecase.CalendarUseCase
import dev.tuandoan.tasktracker.domain.usecase.SubtaskUseCase
import dev.tuandoan.tasktracker.ui.events.UiEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 * - `decorations` — per-day aggregates for the visible month including projected
 *   recurrence occurrences (CAL-24 re-enabled this post-CAL-37).
 * - `agendaItems` — live list of [AgendaItem]s on `selectedDay` (concrete rows merged
 *   with projected occurrences; projections materialize on interaction, CAL-23 + CAL-24).
 * - `subtaskProgress` — `Map<Long, SubtaskProgress>` feeding agenda rows' progress indicator.
 *
 * Events:
 * - Navigation: `onMonthChange` / `onJumpToMonth` / `onDaySelect` / `onTodayClick`.
 * - Agenda-row actions: [onAgendaItemClick] / [onAgendaItemToggleComplete] /
 *   [onAgendaItemArchive] / [onAgendaItemTogglePin]. Each branches on [AgendaItem]:
 *   [AgendaItem.Concrete] dispatches directly; [AgendaItem.Projected] first materializes
 *   the occurrence via [ITaskManager.materializeProjectedOccurrence] (ADR-002 option c —
 *   materialize-then-open) and then routes to the concrete handler.
 * - One-shot UI events emitted via [uiEvent] for Snackbar/undo handling (CAL-21). Archive
 *   emits [UiEvent.ShowUndoDelete]; the screen collects it and triggers [onUndoArchive]
 *   if the user hits UNDO.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
    private val agendaFlow: Flow<List<AgendaItem>> =
        _selectedDay.flatMapLatest { day ->
            calendarUseCase.observeAgendaForDay(day, zone)
        }

    private val subtaskProgressFlow: Flow<Map<Long, SubtaskProgress>> =
        subtaskUseCase.observeProgressByTaskId()

    /**
     * One-shot UI events (Snackbar + undo, CAL-21). Default `MutableSharedFlow()` is what we
     * want here: `emit` suspends when no collector is attached and resumes when one reattaches
     * (e.g., during a brief configuration-change teardown). Adding `extraBufferCapacity = 1`
     * would let emits succeed immediately — but the buffer only serves slow *existing*
     * collectors, not late subscribers, so an archive event fired during a collector gap
     * would land in the buffer and never be delivered to the next subscriber.
     */
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    val uiState: StateFlow<CalendarUiState> = combine(
        _visibleMonth,
        _selectedDay,
        decorationsFlow,
        agendaFlow,
        subtaskProgressFlow,
    ) { month, day, decorations, items, progress ->
        CalendarUiState(
            visibleMonth = month,
            selectedDay = day,
            decorations = decorations,
            agendaItems = items,
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

    // ── Agenda-row actions (CAL-24) ───────────────────────────────────────────────────────
    // Each handler takes an [AgendaItem]. Concrete dispatches directly; Projected first
    // materializes via ITaskManager.materializeProjectedOccurrence then routes to the
    // concrete handler using the returned id. Materialize is idempotent (CAL-23 pt1), so
    // rapid double-taps collapse safely.

    fun onAgendaItemClick(item: AgendaItem, onNavigateToEditor: (Long) -> Unit) {
        when (item) {
            is AgendaItem.Concrete -> onNavigateToEditor(item.task.id)
            is AgendaItem.Projected -> viewModelScope.launch {
                val newId = taskManager.materializeProjectedOccurrence(item.parentTaskId, item.date, zone)
                if (newId != null) onNavigateToEditor(newId)
            }
        }
    }

    fun onAgendaItemToggleComplete(item: AgendaItem) {
        viewModelScope.launch {
            val task = resolveConcreteTask(item) ?: return@launch
            taskManager.toggleTaskCompletion(task)
        }
    }

    fun onAgendaItemArchive(item: AgendaItem) {
        viewModelScope.launch {
            val task = resolveConcreteTask(item) ?: return@launch
            taskManager.archiveTask(task.id)
            // CAL-21: emit an undo event so the screen can surface a Snackbar that
            // reverses the archive on tap. Captures the concrete id so a Projected that
            // just materialized + archived un-archives exactly the row we just created,
            // not some sibling in the chain.
            _uiEvent.emit(
                UiEvent.ShowUndoDelete(
                    tasks = listOf(task),
                    onUndo = { onUndoArchive(task.id) },
                    message = context.getString(R.string.snackbar_task_archived),
                ),
            )
        }
    }

    /** Un-archives the row with [taskId]. Invoked by the Snackbar UNDO action (CAL-21). */
    fun onUndoArchive(taskId: Long) {
        viewModelScope.launch { taskManager.unarchiveTask(taskId) }
    }

    fun onAgendaItemTogglePin(item: AgendaItem) {
        viewModelScope.launch {
            val task = resolveConcreteTask(item) ?: return@launch
            taskManager.setPinned(task.id, !task.isPinned)
        }
    }

    /**
     * Returns the concrete [Task] for [item], materializing a projection if needed. For
     * Concrete items we already have the full Task on hand — return it directly to avoid a
     * redundant DB roundtrip. For Projected items we have to fetch after materialize to
     * get the full row (materialize returns only the new id). Null if materialize declines
     * (archived/non-recurring/unknown parent — shouldn't happen from a live agenda).
     */
    private suspend fun resolveConcreteTask(item: AgendaItem): Task? = when (item) {
        is AgendaItem.Concrete -> item.task
        is AgendaItem.Projected -> {
            val newId = taskManager.materializeProjectedOccurrence(item.parentTaskId, item.date, zone)
            newId?.let { taskManager.getTaskById(it) }
        }
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
    val agendaItems: List<AgendaItem> = emptyList(),
    val subtaskProgress: Map<Long, SubtaskProgress> = emptyMap(),
)
