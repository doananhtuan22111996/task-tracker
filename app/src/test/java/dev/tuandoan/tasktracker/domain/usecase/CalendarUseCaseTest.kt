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

    // ── Recurrence projection behavior (CAL-05 + CAL-24) ────────────────────────────────
    // CAL-37 temporarily reduced decorations to concrete-only to eliminate a "dots but
    // empty agenda" UX lie. CAL-24 re-enables projections now that the agenda surfaces
    // them too (via observeAgendaForDay). Tests below lock the projection contract.

    @Test
    fun `recurring WEEKLY task projects onto every matching day in the window`() = runTest {
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
            val mondays = setOf(
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 18),
                LocalDate.of(2026, 5, 25),
            )
            assertEquals(mondays, emitted.keys)
            // Base day is concrete → projection flag off.
            assertFalse(emitted.getValue(baseDate).hasRecurringProjection)
            // Subsequent Mondays are projection-only → flag on.
            assertTrue(emitted.getValue(LocalDate.of(2026, 5, 11)).hasRecurringProjection)
            assertTrue(emitted.getValue(LocalDate.of(2026, 5, 18)).hasRecurringProjection)
            assertTrue(emitted.getValue(LocalDate.of(2026, 5, 25)).hasRecurringProjection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recurring task with base before window still projects into window`() = runTest {
        val repo = FakeTaskRepository()
        val base = LocalDate.of(2026, 2, 2) // Mon, well before May
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
            val mondays = setOf(
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 18),
                LocalDate.of(2026, 5, 25),
            )
            assertEquals(mondays, emitted.keys)
            // Every May Monday is projection-only (no concrete row exists in range).
            mondays.forEach { assertTrue(emitted.getValue(it).hasRecurringProjection) }
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
    fun `generated recurring children dedup their concrete dates and 5-25 stays projection`() = runTest {
        // Simulates a WEEKLY-Monday rule with two materialized children:
        //   parent (id=1, dueAt=5/4, completed)
        //   child  (id=2, parentRecurringTaskId=1, dueAt=5/11, completed)
        //   child  (id=3, parentRecurringTaskId=1, dueAt=5/18, active)
        // 5/4, 5/11, 5/18 each get exactly one dot (no chain double-counting). 5/25 is
        // projection-only — flag on.
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

            // Concrete days never flag as projection.
            assertFalse(emitted.getValue(LocalDate.of(2026, 5, 4)).hasRecurringProjection)
            assertFalse(emitted.getValue(LocalDate.of(2026, 5, 11)).hasRecurringProjection)
            assertFalse(emitted.getValue(LocalDate.of(2026, 5, 18)).hasRecurringProjection)

            // 5/25 is projection-only.
            val projected = emitted.getValue(LocalDate.of(2026, 5, 25))
            assertEquals(1, projected.taskCount)
            assertTrue(projected.hasRecurringProjection)

            assertEquals(
                setOf(
                    LocalDate.of(2026, 5, 4),
                    LocalDate.of(2026, 5, 11),
                    LocalDate.of(2026, 5, 18),
                    LocalDate.of(2026, 5, 25),
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

    // observeTasksForDay removed in CAL-24 — callers switched to observeAgendaForDay.
    // See CalendarUseCaseAgendaTest for the replacement coverage.

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
