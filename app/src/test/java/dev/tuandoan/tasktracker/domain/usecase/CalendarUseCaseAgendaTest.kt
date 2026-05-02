package dev.tuandoan.tasktracker.domain.usecase

import app.cash.turbine.test
import dev.tuandoan.tasktracker.domain.model.AgendaItem
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Focused tests for [CalendarUseCase.observeAgendaForDay] — the new CAL-23 part 2 entry
 * point that merges concrete rows with projected recurrence occurrences for a single day.
 *
 * Kept in its own class so the existing [CalendarUseCaseTest] (which covers
 * decoration + `observeTasksForDay`) stays focused on concrete-row semantics.
 */
class CalendarUseCaseAgendaTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun dateEpoch(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    // Mondays: 2026-05-04, 05-11, 05-18, 05-25.
    private val monday1 = LocalDate.of(2026, 5, 4)
    private val monday2 = LocalDate.of(2026, 5, 11)

    @Test
    fun `day with only concrete rows emits Concrete items ordered by dueAt`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 2L, dueAt = dateEpoch(monday1) + 2L),
            TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(monday1) + 1L),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeAgendaForDay(monday1, zone).test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertTrue(items.all { it is AgendaItem.Concrete })
            assertEquals(listOf(1L, 2L), items.map { (it as AgendaItem.Concrete).task.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `day with only a recurrence projection emits a single Projected item`() = runTest {
        val repo = FakeTaskRepository()
        // Base far before the queried day; rule = every Monday. No concrete in range.
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = dateEpoch(LocalDate.of(2026, 2, 2)),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = AgendaDayOfWeekBit.MONDAY,
            ),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeAgendaForDay(monday1, zone).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            val projected = items.single() as AgendaItem.Projected
            assertEquals(1L, projected.parentTaskId)
            assertEquals(monday1, projected.date)
            assertEquals("Standup", projected.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concrete row suppresses same-day projection from the same chain`() = runTest {
        val repo = FakeTaskRepository()
        val mondayBits = AgendaDayOfWeekBit.MONDAY
        // Root (id=1) + generated child (id=2, parent=1). Both inherit the WEEKLY-Monday
        // rule via buildNextTask.copy. On monday2 the child is concrete; projecting from
        // the root would add a second row for the same user-perceived "this week's standup".
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                dueAt = dateEpoch(monday1),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = mondayBits,
            ),
            TestTaskFactory.createTask(
                id = 2L,
                dueAt = dateEpoch(monday2),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = mondayBits,
                parentRecurringTaskId = 1L,
            ),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeAgendaForDay(monday2, zone).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            val concrete = items.single() as AgendaItem.Concrete
            assertEquals(2L, concrete.task.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concrete row from one chain coexists with projection from unrelated chain`() = runTest {
        val repo = FakeTaskRepository()
        val mondayBits = AgendaDayOfWeekBit.MONDAY
        repo.seed(
            // Unrelated one-off concrete on monday2.
            TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(monday2)),
            // Separate WEEKLY-Monday chain whose base is earlier — projects onto monday2.
            TestTaskFactory.createTask(
                id = 7L,
                title = "Standup",
                dueAt = dateEpoch(LocalDate.of(2026, 2, 2)),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = mondayBits,
            ),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeAgendaForDay(monday2, zone).test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertTrue(items[0] is AgendaItem.Concrete)
            assertTrue(items[1] is AgendaItem.Projected)
            assertEquals(1L, (items[0] as AgendaItem.Concrete).task.id)
            assertEquals(7L, (items[1] as AgendaItem.Projected).parentTaskId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `archived recurring parent produces no projection`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                dueAt = dateEpoch(LocalDate.of(2026, 2, 2)),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = AgendaDayOfWeekBit.MONDAY,
                isArchived = true,
            ),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeAgendaForDay(monday1, zone).test {
            val items = awaitItem()
            assertTrue(items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recurrence end date before queried day produces no projection`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                dueAt = dateEpoch(LocalDate.of(2026, 2, 2)),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = AgendaDayOfWeekBit.MONDAY,
                recurrenceEndDate = dateEpoch(LocalDate.of(2026, 3, 1)), // before monday1
            ),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeAgendaForDay(monday1, zone).test {
            val items = awaitItem()
            assertTrue(items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-recurring task contributes Concrete only, never Projected`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(monday1)))

        val useCase = CalendarUseCase(repo)
        useCase.observeAgendaForDay(monday1, zone).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertTrue(items.single() is AgendaItem.Concrete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flow re-emits when a task is inserted on the queried day`() = runTest {
        val repo = FakeTaskRepository()
        val useCase = CalendarUseCase(repo)

        useCase.observeAgendaForDay(monday1, zone).test {
            assertTrue(awaitItem().isEmpty())

            repo.insertTask(TestTaskFactory.createTask(id = 0L, dueAt = dateEpoch(monday1)))

            val items = awaitItem()
            assertEquals(1, items.size)
            assertTrue(items.single() is AgendaItem.Concrete)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/** Local convenience mirror of the bitmask used by [RecurrenceCalculator]. */
private object AgendaDayOfWeekBit {
    const val MONDAY = 1 // 1 shl 0
}
