package dev.tuandoan.tasktracker.domain.usecase

import app.cash.turbine.test
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CalendarUseCaseTest {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val monthStart: LocalDate = LocalDate.of(2026, 5, 1)
    private val monthEnd: LocalDate = LocalDate.of(2026, 5, 31)

    private fun dateEpoch(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `empty repository emits empty map`() = runTest {
        val repo = FakeTaskRepository()
        val useCase = CalendarUseCase(repo)

        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val emitted = awaitItem()
            assertTrue(emitted.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `single concrete dated task produces one decoration`() = runTest {
        val repo = FakeTaskRepository()
        val day = LocalDate.of(2026, 5, 10)
        repo.seed(TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(day), priority = 1))

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val emitted = awaitItem()
            assertEquals(1, emitted.size)
            val deco = emitted.getValue(day)
            assertEquals(day, deco.date)
            assertEquals(1, deco.taskCount)
            assertEquals(setOf(1), deco.priorityBuckets)
            assertEquals(0, deco.completedCount)
            assertFalse(deco.hasRecurringProjection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple tasks on same day aggregate priority buckets`() = runTest {
        val repo = FakeTaskRepository()
        val day = LocalDate.of(2026, 5, 15)
        repo.seed(
            TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(day), priority = 0),
            TestTaskFactory.createTask(id = 2L, dueAt = dateEpoch(day), priority = 0),
            TestTaskFactory.createTask(id = 3L, dueAt = dateEpoch(day), priority = 1),
            TestTaskFactory.createTask(id = 4L, dueAt = dateEpoch(day), priority = 2),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val deco = awaitItem().getValue(day)
            assertEquals(4, deco.taskCount)
            assertEquals(setOf(0, 1, 2), deco.priorityBuckets)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completed tasks contribute to completedCount and stay in taskCount`() = runTest {
        val repo = FakeTaskRepository()
        val day = LocalDate.of(2026, 5, 20)
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                dueAt = dateEpoch(day),
                priority = 1,
                isCompleted = true,
                completedAt = dateEpoch(day),
            ),
            TestTaskFactory.createTask(id = 2L, dueAt = dateEpoch(day), priority = 1, isCompleted = false),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val deco = awaitItem().getValue(day)
            assertEquals(2, deco.taskCount)
            assertEquals(1, deco.completedCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `archived tasks are excluded from decorations`() = runTest {
        val repo = FakeTaskRepository()
        val day = LocalDate.of(2026, 5, 10)
        repo.seed(
            TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(day), isArchived = true),
            TestTaskFactory.createTask(id = 2L, dueAt = dateEpoch(day)),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val deco = awaitItem().getValue(day)
            assertEquals(1, deco.taskCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Recurrence projection behavior (CAL-37) ─────────────────────────────────────────
    // Before CAL-37, decorations included projected recurrence occurrences. That produced
    // a UX bug: dots rendered on every projected day but the agenda (which only surfaces
    // concrete Room rows) was empty — "dots but no tasks". Projections now stay out of the
    // grid until CAL-23/24 materializes them into the agenda.

    @Test
    fun `recurring WEEKLY task only decorates its concrete base date, not projected occurrences`() = runTest {
        val repo = FakeTaskRepository()
        val baseDate = LocalDate.of(2026, 5, 4)
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                dueAt = dateEpoch(baseDate),
                priority = 2,
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = RecurrenceCalculatorDayOfWeekBit.MONDAY,
            ),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val emitted = awaitItem()
            // Only the concrete base Monday is decorated. 5/11, 5/18, 5/25 stay empty
            // because projecting them would lie: the agenda has no row to show there.
            assertEquals(setOf(baseDate), emitted.keys)
            assertFalse(emitted.getValue(baseDate).hasRecurringProjection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recurring task with base before window produces no decorations`() = runTest {
        val repo = FakeTaskRepository()
        // Base far before window — no concrete rows will land in May.
        val base = LocalDate.of(2026, 2, 2) // Mon
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                dueAt = dateEpoch(base),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = RecurrenceCalculatorDayOfWeekBit.MONDAY,
            ),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val emitted = awaitItem()
            // No projection decorations — agenda has nothing to show on those Mondays.
            assertTrue(emitted.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `archived recurring task produces no projections`() = runTest {
        val repo = FakeTaskRepository()
        val base = LocalDate.of(2026, 5, 4)
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                dueAt = dateEpoch(base),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = RecurrenceCalculatorDayOfWeekBit.MONDAY,
                isArchived = true,
            ),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val emitted = awaitItem()
            assertTrue(emitted.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `generated recurring children decorate their concrete dates without projecting future days`() = runTest {
        // Simulates a WEEKLY-Monday rule completed twice, so the DB holds:
        //   parent (id=1, dueAt=5/4, completed)
        //   child  (id=2, parentRecurringTaskId=1, dueAt=5/11, completed)
        //   child  (id=3, parentRecurringTaskId=1, dueAt=5/18, active)
        // Each concrete Monday gets exactly one dot (no chain double-counting). 5/25 stays
        // empty until the next child is actually generated — no projection is added.
        val repo = FakeTaskRepository()
        val mondayBitmask = RecurrenceCalculatorDayOfWeekBit.MONDAY
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                dueAt = dateEpoch(LocalDate.of(2026, 5, 4)),
                isCompleted = true,
                completedAt = dateEpoch(LocalDate.of(2026, 5, 4)),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = mondayBitmask,
            ),
            TestTaskFactory.createTask(
                id = 2L,
                dueAt = dateEpoch(LocalDate.of(2026, 5, 11)),
                isCompleted = true,
                completedAt = dateEpoch(LocalDate.of(2026, 5, 11)),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = mondayBitmask,
                parentRecurringTaskId = 1L,
            ),
            TestTaskFactory.createTask(
                id = 3L,
                dueAt = dateEpoch(LocalDate.of(2026, 5, 18)),
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceDaysOfWeek = mondayBitmask,
                parentRecurringTaskId = 1L,
            ),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val emitted = awaitItem()

            // 5/4, 5/11, 5/18 each hold a single concrete task (taskCount=1, not 2 or 3).
            assertEquals(1, emitted.getValue(LocalDate.of(2026, 5, 4)).taskCount)
            assertEquals(1, emitted.getValue(LocalDate.of(2026, 5, 11)).taskCount)
            assertEquals(1, emitted.getValue(LocalDate.of(2026, 5, 18)).taskCount)

            // All decorated days are concrete — the projection flag is always false under CAL-37.
            assertFalse(emitted.getValue(LocalDate.of(2026, 5, 4)).hasRecurringProjection)
            assertFalse(emitted.getValue(LocalDate.of(2026, 5, 11)).hasRecurringProjection)
            assertFalse(emitted.getValue(LocalDate.of(2026, 5, 18)).hasRecurringProjection)

            // 5/25 has no concrete row yet → absent from the map.
            assertEquals(
                setOf(
                    LocalDate.of(2026, 5, 4),
                    LocalDate.of(2026, 5, 11),
                    LocalDate.of(2026, 5, 18),
                ),
                emitted.keys,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-recurring tasks never add projection flag`() = runTest {
        val repo = FakeTaskRepository()
        val day = LocalDate.of(2026, 5, 10)
        repo.seed(TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(day)))

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val deco = awaitItem().getValue(day)
            assertFalse(deco.hasRecurringProjection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `days outside window are absent`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(LocalDate.of(2026, 4, 20))),
            TestTaskFactory.createTask(id = 2L, dueAt = dateEpoch(LocalDate.of(2026, 6, 2))),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val emitted = awaitItem()
            assertTrue(emitted.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `inclusive boundaries include monthEnd tasks`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(monthStart)),
            TestTaskFactory.createTask(id = 2L, dueAt = dateEpoch(monthEnd)),
        )

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val emitted = awaitItem()
            assertEquals(setOf(monthStart, monthEnd), emitted.keys)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reactive re-emit when a new task is inserted`() = runTest {
        val repo = FakeTaskRepository()
        val useCase = CalendarUseCase(repo)
        val day = LocalDate.of(2026, 5, 12)

        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            assertTrue(awaitItem().isEmpty())

            repo.insertTask(TestTaskFactory.createTask(id = 0L, dueAt = dateEpoch(day)))

            val second = awaitItem()
            assertEquals(1, second.size)
            assertEquals(day, second.keys.single())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── observeTasksForDay (CAL-17) ──

    @Test
    fun `observeTasksForDay returns only tasks on that day`() = runTest {
        val day = LocalDate.of(2026, 5, 10)
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 1L, title = "Hit", dueAt = dateEpoch(day)),
            TestTaskFactory.createTask(id = 2L, title = "Day before", dueAt = dateEpoch(day.minusDays(1))),
            TestTaskFactory.createTask(id = 3L, title = "Day after", dueAt = dateEpoch(day.plusDays(1))),
        )
        val useCase = CalendarUseCase(repo)

        useCase.observeTasksForDay(day, zone).test {
            val emitted = awaitItem()
            assertEquals(listOf(1L), emitted.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeTasksForDay emits empty list for a day with no tasks`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(LocalDate.of(2026, 5, 10))))
        val useCase = CalendarUseCase(repo)

        useCase.observeTasksForDay(LocalDate.of(2026, 5, 11), zone).test {
            val emitted = awaitItem()
            assertTrue(emitted.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeTasksForDay re-emits when a task is inserted on that day`() = runTest {
        val day = LocalDate.of(2026, 5, 10)
        val repo = FakeTaskRepository()
        val useCase = CalendarUseCase(repo)

        useCase.observeTasksForDay(day, zone).test {
            assertTrue(awaitItem().isEmpty())

            repo.insertTask(TestTaskFactory.createTask(id = 0L, dueAt = dateEpoch(day)))

            val populated = awaitItem()
            assertEquals(1, populated.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `missing entry means no tasks`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(TestTaskFactory.createTask(id = 1L, dueAt = dateEpoch(LocalDate.of(2026, 5, 10))))

        val useCase = CalendarUseCase(repo)
        useCase.observeMonthDecorations(monthStart, monthEnd, zone).test {
            val emitted = awaitItem()
            assertNull(emitted[LocalDate.of(2026, 5, 11)])
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/**
 * Local convenience for readable test fixtures. Mirrors the bitmask convention in
 * [dev.tuandoan.tasktracker.domain.service.RecurrenceCalculator].
 */
private object RecurrenceCalculatorDayOfWeekBit {
    const val MONDAY = 1 // 1 shl 0
}
