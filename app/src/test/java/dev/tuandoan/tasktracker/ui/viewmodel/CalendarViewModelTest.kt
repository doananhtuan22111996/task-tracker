package dev.tuandoan.tasktracker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.tuandoan.tasktracker.domain.usecase.CalendarUseCase
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
    private lateinit var useCase: CalendarUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeTaskRepository()
        useCase = CalendarUseCase(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedState: SavedStateHandle = SavedStateHandle()): CalendarViewModel =
        CalendarViewModel(useCase, savedState)

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

            val populated = awaitItem()
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

    // ── selectedDayTasks (CAL-17) ──

    @Test
    fun `uiState selectedDayTasks reflects tasks on the selected day`() = runTest {
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
            assertEquals(listOf(1L), state.selectedDayTasks.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState selectedDayTasks updates when selectedDay changes`() = runTest {
        val savedState = SavedStateHandle(
            mapOf(
                CalendarViewModel.KEY_VISIBLE_MONTH to "2026-05",
                CalendarViewModel.KEY_SELECTED_DAY to "2026-05-10",
            ),
        )
        repo.seed(
            TestTaskFactory.createTask(id = 1L, title = "On 10", dueAt = dateEpoch(LocalDate.of(2026, 5, 10))),
            TestTaskFactory.createTask(id = 2L, title = "On 20", dueAt = dateEpoch(LocalDate.of(2026, 5, 20))),
        )
        val vm = createViewModel(savedState)

        vm.uiState.test {
            val first = awaitItem()
            assertEquals(listOf(1L), first.selectedDayTasks.map { it.id })

            vm.onDaySelect(LocalDate.of(2026, 5, 20))

            var latest = awaitItem()
            while (latest.selectedDay != LocalDate.of(2026, 5, 20) ||
                latest.selectedDayTasks.map { it.id } != listOf(2L)
            ) {
                latest = awaitItem()
            }
            assertEquals(listOf(2L), latest.selectedDayTasks.map { it.id })
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
}
