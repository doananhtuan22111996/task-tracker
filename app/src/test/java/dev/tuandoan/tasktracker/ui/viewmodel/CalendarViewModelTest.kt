package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.domain.model.AgendaItem
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.domain.usecase.CalendarUseCase
import dev.tuandoan.tasktracker.domain.usecase.SubtaskUseCase
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeSubtaskRepository
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.FakeWidgetUpdater
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.testutil.fakeBreadcrumbLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val zone: ZoneId = ZoneId.systemDefault()

    private lateinit var repo: FakeTaskRepository
    private lateinit var subtaskRepo: FakeSubtaskRepository
    private lateinit var useCase: CalendarUseCase
    private lateinit var subtaskUseCase: SubtaskUseCase
    private lateinit var taskManager: TaskManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeTaskRepository()
        subtaskRepo = FakeSubtaskRepository()
        useCase = CalendarUseCase(repo)
        subtaskUseCase = SubtaskUseCase(subtaskRepo)
        taskManager =
            TaskManager(repo, subtaskRepo, FakeReminderScheduler(), FakeWidgetUpdater(), fakeBreadcrumbLogger())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedState: SavedStateHandle = SavedStateHandle()): CalendarViewModel =
        CalendarViewModel(useCase, savedState, taskManager, subtaskUseCase, fakeBreadcrumbLogger())

    private fun dateEpoch(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    // ── Initial state ──

    @Test
    fun `empty SavedStateHandle defaults to today's month and today`() = runTest {
        val vm = createViewModel()
        val today = LocalDate.now(zone)

        assertEquals(YearMonth.from(today), vm.visibleMonth.value)
        assertEquals(today, vm.selectedDay.value)
    }

    @Test
    fun `malformed SavedStateHandle falls back to today without crashing`() = runTest {
        val savedState = SavedStateHandle(
            mapOf(
                CalendarViewModel.KEY_VISIBLE_MONTH to "not-a-month",
                CalendarViewModel.KEY_SELECTED_DAY to "42nd-of-Maytember",
            ),
        )
        val vm = createViewModel(savedState)
        val today = LocalDate.now(zone)

        assertEquals(YearMonth.from(today), vm.visibleMonth.value)
        assertEquals(today, vm.selectedDay.value)
    }

    @Test
    fun `populated SavedStateHandle restores saved month and day`() = runTest {
        val savedState = SavedStateHandle(
            mapOf(
                CalendarViewModel.KEY_VISIBLE_MONTH to "2026-08",
                CalendarViewModel.KEY_SELECTED_DAY to "2026-08-15",
            ),
        )
        val vm = createViewModel(savedState)

        assertEquals(YearMonth.of(2026, 8), vm.visibleMonth.value)
        assertEquals(LocalDate.of(2026, 8, 15), vm.selectedDay.value)
    }

    // ── onMonthChange ──

    @Test
    fun `onMonthChange plus 1 advances visible month`() = runTest {
        val savedState = SavedStateHandle(mapOf(CalendarViewModel.KEY_VISIBLE_MONTH to "2026-05"))
        val vm = createViewModel(savedState)

        vm.onMonthChange(1)

        assertEquals(YearMonth.of(2026, 6), vm.visibleMonth.value)
        assertEquals("2026-06", savedState.get<String>(CalendarViewModel.KEY_VISIBLE_MONTH))
    }

    @Test
    fun `onMonthChange minus 1 rewinds visible month across year boundary`() = runTest {
        val savedState = SavedStateHandle(mapOf(CalendarViewModel.KEY_VISIBLE_MONTH to "2026-01"))
        val vm = createViewModel(savedState)

        vm.onMonthChange(-1)

        assertEquals(YearMonth.of(2025, 12), vm.visibleMonth.value)
        assertEquals("2025-12", savedState.get<String>(CalendarViewModel.KEY_VISIBLE_MONTH))
    }

    @Test
    fun `onMonthChange does not touch selectedDay`() = runTest {
        val savedState = SavedStateHandle(
            mapOf(
                CalendarViewModel.KEY_VISIBLE_MONTH to "2026-05",
                CalendarViewModel.KEY_SELECTED_DAY to "2026-05-10",
            ),
        )
        val vm = createViewModel(savedState)

        vm.onMonthChange(1)

        assertEquals(LocalDate.of(2026, 5, 10), vm.selectedDay.value)
    }

    // ── onDaySelect ──

    @Test
    fun `onDaySelect updates selectedDay and persists`() = runTest {
        val savedState = SavedStateHandle()
        val vm = createViewModel(savedState)
        val target = LocalDate.of(2026, 5, 20)

        vm.onDaySelect(target)

        assertEquals(target, vm.selectedDay.value)
        assertEquals("2026-05-20", savedState.get<String>(CalendarViewModel.KEY_SELECTED_DAY))
    }

    // ── onTodayClick ──

    @Test
    fun `onTodayClick resets both month and selectedDay to today`() = runTest {
        val savedState = SavedStateHandle(
            mapOf(
                CalendarViewModel.KEY_VISIBLE_MONTH to "2020-01",
                CalendarViewModel.KEY_SELECTED_DAY to "2020-01-01",
            ),
        )
        val vm = createViewModel(savedState)
        val today = LocalDate.now(zone)

        vm.onTodayClick()

        assertEquals(YearMonth.from(today), vm.visibleMonth.value)
        assertEquals(today, vm.selectedDay.value)
        assertEquals(YearMonth.from(today).toString(), savedState.get<String>(CalendarViewModel.KEY_VISIBLE_MONTH))
        assertEquals(today.toString(), savedState.get<String>(CalendarViewModel.KEY_SELECTED_DAY))
    }

    // ── onJumpToMonth ──

    @Test
    fun `onJumpToMonth updates month without touching selectedDay`() = runTest {
        val savedState = SavedStateHandle(
            mapOf(
                CalendarViewModel.KEY_VISIBLE_MONTH to "2026-05",
                CalendarViewModel.KEY_SELECTED_DAY to "2026-05-15",
            ),
        )
        val vm = createViewModel(savedState)

        vm.onJumpToMonth(YearMonth.of(2027, 3))

        assertEquals(YearMonth.of(2027, 3), vm.visibleMonth.value)
        assertEquals(LocalDate.of(2026, 5, 15), vm.selectedDay.value)
        assertEquals("2027-03", savedState.get<String>(CalendarViewModel.KEY_VISIBLE_MONTH))
    }

    // ── uiState decorations ──

    @Test
    fun `uiState decorations reflect tasks in the visible month`() = runTest {
        val savedState = SavedStateHandle(mapOf(CalendarViewModel.KEY_VISIBLE_MONTH to "2026-05"))
        repo.seed(
            TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(LocalDate.of(2026, 5, 10))),
            TestTaskFactory.createTask(id = 2L, dueAt = dateEpoch(LocalDate.of(2026, 6, 1))), // outside
        )
        val vm = createViewModel(savedState)

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(YearMonth.of(2026, 5), state.visibleMonth)
            assertEquals(setOf(LocalDate.of(2026, 5, 10)), state.decorations.keys)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState decorations change when month changes`() = runTest {
        val savedState = SavedStateHandle(mapOf(CalendarViewModel.KEY_VISIBLE_MONTH to "2026-05"))
        repo.seed(
            TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(LocalDate.of(2026, 5, 10))),
            TestTaskFactory.createTask(id = 2L, dueAt = dateEpoch(LocalDate.of(2026, 6, 15))),
        )
        val vm = createViewModel(savedState)

        vm.uiState.test {
            // May: only 5/10
            val may = awaitItem()
            assertEquals(setOf(LocalDate.of(2026, 5, 10)), may.decorations.keys)

            vm.onMonthChange(1)

            // Skip the intermediate emission where month changed but decorations haven't caught up.
            var june = awaitItem()
            while (june.visibleMonth != YearMonth.of(2026, 6) ||
                !june.decorations.containsKey(LocalDate.of(2026, 6, 15))
            ) {
                june = awaitItem()
            }
            assertEquals(setOf(LocalDate.of(2026, 6, 15)), june.decorations.keys)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState decorations re-emit when a new task is inserted`() = runTest {
        val savedState = SavedStateHandle(mapOf(CalendarViewModel.KEY_VISIBLE_MONTH to "2026-05"))
        val vm = createViewModel(savedState)

        vm.uiState.test {
            val empty = awaitItem()
            assertTrue(empty.decorations.isEmpty())

            repo.insertTask(TestTaskFactory.createTask(id = 0L, dueAt = dateEpoch(LocalDate.of(2026, 5, 12))))

            // Multiple upstream flows participate in the uiState combine (decorations,
            // agenda, hasAnyDatedTask). An insert touches several at once, so we may see an
            // intermediate state where the flag flipped but decorations haven't caught up.
            var populated = awaitItem()
            while (!populated.decorations.containsKey(LocalDate.of(2026, 5, 12))) {
                populated = awaitItem()
            }
            assertEquals(setOf(LocalDate.of(2026, 5, 12)), populated.decorations.keys)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState selectedDay update preserves decorations map`() = runTest {
        val savedState = SavedStateHandle(mapOf(CalendarViewModel.KEY_VISIBLE_MONTH to "2026-05"))
        repo.seed(TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(LocalDate.of(2026, 5, 10))))
        val vm = createViewModel(savedState)

        vm.uiState.test {
            val initial = awaitItem()
            val decorations = initial.decorations

            vm.onDaySelect(LocalDate.of(2026, 5, 20))

            val after = awaitItem()
            assertEquals(LocalDate.of(2026, 5, 20), after.selectedDay)
            assertEquals(decorations, after.decorations)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Saved state persistence ──

    // ── agendaItems (CAL-17 / CAL-23 / CAL-24) ──

    @Test
    fun `uiState agendaItems wraps concrete tasks for the selected day`() = runTest {
        val savedState = SavedStateHandle(
            mapOf(
                CalendarViewModel.KEY_VISIBLE_MONTH to "2026-05",
                CalendarViewModel.KEY_SELECTED_DAY to "2026-05-10",
            ),
        )
        val target = LocalDate.of(2026, 5, 10)
        repo.seed(
            TestTaskFactory.createTask(id = 1L, title = "Today", dueAt = dateEpoch(target)),
            TestTaskFactory.createTask(id = 2L, title = "Other day", dueAt = dateEpoch(target.minusDays(1))),
        )
        val vm = createViewModel(savedState)

        vm.uiState.test {
            val state = awaitItem()
            val concrete = state.agendaItems.filterIsInstance<AgendaItem.Concrete>()
            assertEquals(listOf(1L), concrete.map { it.task.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState agendaItems includes projected occurrences for recurring rules hitting selected day`() = runTest {
        val selected = LocalDate.of(2026, 5, 11) // Monday
        val savedState = SavedStateHandle(
            mapOf(
                CalendarViewModel.KEY_VISIBLE_MONTH to "2026-05",
                CalendarViewModel.KEY_SELECTED_DAY to selected.toString(),
            ),
        )
        // Base before selected, rule = every Monday → projects onto 2026-05-11.
        repo.seed(
            TestTaskFactory.createTask(
                id = 7L,
                title = "Standup",
                dueAt = dateEpoch(LocalDate.of(2026, 2, 2)),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = 1, // Monday bit
            ),
        )
        val vm = createViewModel(savedState)

        vm.uiState.test {
            val state = awaitItem()
            val projected = state.agendaItems.filterIsInstance<AgendaItem.Projected>()
            assertEquals(1, projected.size)
            assertEquals(7L, projected.single().parentTaskId)
            assertEquals(selected, projected.single().date)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SavedStateHandle contains the latest values after mutations`() = runTest {
        val savedState = SavedStateHandle()
        val vm = createViewModel(savedState)

        vm.onJumpToMonth(YearMonth.of(2027, 1))
        vm.onDaySelect(LocalDate.of(2027, 1, 15))

        assertEquals("2027-01", savedState.get<String>(CalendarViewModel.KEY_VISIBLE_MONTH))
        assertEquals("2027-01-15", savedState.get<String>(CalendarViewModel.KEY_SELECTED_DAY))
    }

    @Test
    fun `fresh SavedStateHandle has no saved keys initially`() = runTest {
        val savedState = SavedStateHandle()
        // ViewModel constructor does not write defaults; only explicit mutations persist.
        createViewModel(savedState)

        assertNull(savedState.get<String>(CalendarViewModel.KEY_VISIBLE_MONTH))
        assertNull(savedState.get<String>(CalendarViewModel.KEY_SELECTED_DAY))
    }

    // ── Agenda-row actions (CAL-24) ──

    private fun concreteItem(task: dev.tuandoan.tasktracker.data.database.Task): AgendaItem.Concrete =
        AgendaItem.Concrete(task = task, date = LocalDate.of(2026, 5, 10))

    private fun projectedItem(parentId: Long, date: LocalDate): AgendaItem.Projected = AgendaItem.Projected(
        parentTaskId = parentId,
        date = date,
        title = "Standup",
        description = "",
        priority = 1,
        tag = null,
        tagColor = null,
        dueAtHasTime = false,
        reminderOffsetMinutes = null,
        recurrenceType = RecurrenceType.DAILY.value,
        recurrenceInterval = 1,
        recurrenceDaysOfWeek = 0,
        recurrenceEndDate = null,
    )

    @Test
    fun `onAgendaItemClick Concrete forwards task id to navigate callback`() = runTest {
        val task = TestTaskFactory.createTask(id = 42L, dueAt = dateEpoch(LocalDate.of(2026, 5, 10)))
        repo.seed(task)
        val vm = createViewModel()
        var navigatedTo: Long? = null

        vm.onAgendaItemClick(concreteItem(task)) { navigatedTo = it }

        assertEquals(42L, navigatedTo)
    }

    @Test
    fun `onAgendaItemClick Projected materializes then navigates to new id`() = runTest {
        val parentDate = LocalDate.of(2026, 5, 4)
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = dateEpoch(parentDate),
                recurrenceType = RecurrenceType.DAILY.value,
            ),
        )
        val vm = createViewModel()
        var navigatedTo: Long? = null
        val targetDate = LocalDate.of(2026, 5, 7)

        vm.onAgendaItemClick(projectedItem(parentId = 1L, date = targetDate)) { navigatedTo = it }

        val id = navigatedTo
        assertTrue("expected a materialized id", id != null && id > 0)
        val inserted = repo.getAllTasksSnapshot().single { it.id == id }
        assertEquals(1L, inserted.parentRecurringTaskId)
        assertEquals(dateEpoch(targetDate), inserted.dueAt)
    }

    @Test
    fun `onAgendaItemClick Projected with unknown parent does not invoke navigate`() = runTest {
        val vm = createViewModel()
        var navigateInvoked = false

        vm.onAgendaItemClick(projectedItem(parentId = 999L, date = LocalDate.of(2026, 5, 5))) {
            navigateInvoked = true
        }

        assertFalse(navigateInvoked)
        assertTrue(repo.getAllTasksSnapshot().isEmpty())
    }

    @Test
    fun `onAgendaItemToggleComplete Concrete flips completion`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            dueAt = dateEpoch(LocalDate.of(2026, 5, 10)),
            isCompleted = false,
        )
        repo.seed(task)
        val vm = createViewModel()

        vm.onAgendaItemToggleComplete(concreteItem(task))

        assertTrue(repo.getAllTasksSnapshot().single { it.id == 1L }.isCompleted)
    }

    @Test
    fun `onAgendaItemToggleComplete Projected materializes then completes (regenerates next)`() = runTest {
        val parentDate = LocalDate.of(2026, 5, 4)
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = dateEpoch(parentDate),
                recurrenceType = RecurrenceType.DAILY.value,
            ),
        )
        val vm = createViewModel()
        val targetDate = LocalDate.of(2026, 5, 7)

        vm.onAgendaItemToggleComplete(projectedItem(parentId = 1L, date = targetDate))

        // At least one new row exists. The materialized row got completed; completing a
        // recurring row generates the next occurrence, so the snapshot may also contain a
        // freshly-generated child.
        val materialized = repo.getAllTasksSnapshot().filter { it.parentRecurringTaskId == 1L }
        assertTrue(materialized.any { it.isCompleted && it.dueAt == dateEpoch(targetDate) })
    }

    @Test
    fun `onAgendaItemArchive Concrete marks archived`() = runTest {
        val task = TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(LocalDate.of(2026, 5, 10)))
        repo.seed(task)
        val vm = createViewModel()

        vm.onAgendaItemArchive(concreteItem(task))

        assertTrue(repo.getAllTasksSnapshot().single { it.id == 1L }.isArchived)
    }

    @Test
    fun `onAgendaItemArchive Projected materializes then archives the new row`() = runTest {
        val parentDate = LocalDate.of(2026, 5, 4)
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = dateEpoch(parentDate),
                recurrenceType = RecurrenceType.DAILY.value,
            ),
        )
        val vm = createViewModel()
        val targetDate = LocalDate.of(2026, 5, 7)

        vm.onAgendaItemArchive(projectedItem(parentId = 1L, date = targetDate))

        // The materialized child is archived; the root is untouched.
        val materialized = repo.getAllTasksSnapshot().single { it.parentRecurringTaskId == 1L }
        assertTrue(materialized.isArchived)
        assertFalse(repo.getAllTasksSnapshot().single { it.id == 1L }.isArchived)
    }

    @Test
    fun `onAgendaItemTogglePin Concrete flips pinned state`() = runTest {
        val task = TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(LocalDate.of(2026, 5, 10)))
        repo.seed(task)
        val vm = createViewModel()

        vm.onAgendaItemTogglePin(concreteItem(task))
        assertTrue(repo.getAllTasksSnapshot().single { it.id == 1L }.isPinned)

        vm.onAgendaItemTogglePin(concreteItem(repo.getAllTasksSnapshot().single { it.id == 1L }))
        assertFalse(repo.getAllTasksSnapshot().single { it.id == 1L }.isPinned)
    }

    @Test
    fun `onAgendaItemTogglePin Projected materializes then pins the new row`() = runTest {
        val parentDate = LocalDate.of(2026, 5, 4)
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = dateEpoch(parentDate),
                recurrenceType = RecurrenceType.DAILY.value,
            ),
        )
        val vm = createViewModel()
        val targetDate = LocalDate.of(2026, 5, 7)

        vm.onAgendaItemTogglePin(projectedItem(parentId = 1L, date = targetDate))

        // Root is untouched; materialized child is pinned.
        assertFalse(repo.getAllTasksSnapshot().single { it.id == 1L }.isPinned)
        val materialized = repo.getAllTasksSnapshot().single { it.parentRecurringTaskId == 1L }
        assertTrue(materialized.isPinned)
        assertEquals(dateEpoch(targetDate), materialized.dueAt)
    }

    // ── CAL-16: hasAnyDatedTask in UI state ──────────────────────────────────────────────

    @Test
    fun `uiState hasAnyDatedTask reflects repository as tasks are added`() = runTest {
        // Start with an undated task so the initial emission is explicit about the false case.
        repo.seed(TestTaskFactory.createTask(id = 1L, dueAt = null))
        val vm = createViewModel()

        vm.uiState.test {
            // The stateIn initialValue defaults to true (don't flash the hint during cold
            // start); the first downstream emission reflects the repository.
            val firstReal = awaitItem().takeIf { !it.hasAnyDatedTask } ?: awaitItem()
            assertFalse(firstReal.hasAnyDatedTask)

            repo.insertTask(TestTaskFactory.createTask(id = 2L, dueAt = dateEpoch(LocalDate.of(2026, 5, 10))))
            val afterInsert = awaitItem()
            assertTrue(afterInsert.hasAnyDatedTask)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // === FB-12: onDaySelect emits a NAV breadcrumb WITHOUT the date (leaks usage timing) ===

    @Test
    fun `onDaySelect logs NAV calendar_day_tap breadcrumb and omits the date`() = runTest {
        val breadcrumbLogger = io.mockk.mockk<dev.tuandoan.tasktracker.diagnostics.BreadcrumbLogger>(relaxed = true)
        val vm = CalendarViewModel(useCase, SavedStateHandle(), taskManager, subtaskUseCase, breadcrumbLogger)
        vm.onDaySelect(LocalDate.of(2026, 5, 12))
        io.mockk.verify {
            breadcrumbLogger.log(
                dev.tuandoan.tasktracker.diagnostics.BreadcrumbCategory.NAV,
                "calendar_day_tap",
            )
        }
    }
}
