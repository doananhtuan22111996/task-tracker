package dev.tuandoan.tasktracker.widget.action

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeSubtaskRepository
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.FakeWidgetUpdater
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.testutil.fakeAnalyticsLogger
import dev.tuandoan.tasktracker.testutil.fakeBreadcrumbLogger
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * V13-04/V13-06: covers FR-11 (cancel reminder → mark complete → widget update ordering),
 * FR-12 (idempotency under double-tap), and NFR-08 (no leftover-cancelled reminder
 * when completion no-ops). Uses fakes only — no Glance runtime, no mocks.
 *
 * V13-06 additions: timed double-tap scenarios, explicit ordering assertion via call log,
 * and no-reschedule guarantee post-completion.
 */
class WidgetCompleteHandlerTest {

    private lateinit var repository: FakeTaskRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var widgetUpdater: FakeWidgetUpdater
    private lateinit var handler: WidgetCompleteHandler

    @Before
    fun setup() {
        repository = FakeTaskRepository()
        scheduler = FakeReminderScheduler()
        widgetUpdater = FakeWidgetUpdater()
        val taskManager = TaskManager(
            repository,
            FakeSubtaskRepository(),
            scheduler,
            widgetUpdater,
            fakeBreadcrumbLogger(),
            fakeAnalyticsLogger(),
        )
        handler = WidgetCompleteHandler(taskManager)
    }

    @Test
    fun `complete marks active task done and returns true`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Buy groceries")
        repository.seed(task)

        val applied = handler.complete(1L)

        assertTrue(applied)
        val result = repository.getTaskById(1L)
        assertNotNull(result)
        assertTrue(result!!.isCompleted)
        assertNotNull(result.completedAt)
    }

    @Test
    fun `complete cancels reminder before persisting completion`() = runTest {
        // Inherits the cancel-then-persist ordering from TaskManager.toggleTaskCompletion.
        // We assert both effects happened — ordering is enforced by TaskManager and
        // already covered there; this test pins the contract from the widget surface.
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "Meeting",
            dueAt = System.currentTimeMillis() + 3_600_000L,
            reminderOffsetMinutes = 15,
        )
        repository.seed(task)

        handler.complete(1L)

        assertTrue(scheduler.cancelledTaskIds.contains(1L))
        assertTrue(repository.getTaskById(1L)!!.isCompleted)
    }

    @Test
    fun `complete triggers a widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Refresh me")
        repository.seed(task)

        handler.complete(1L)

        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `complete on already completed task is a no-op (idempotent)`() = runTest {
        val task = TestTaskFactory.completedTask(id = 1, title = "Already done")
        repository.seed(task)

        val applied = handler.complete(1L)

        assertFalse(applied)
        // No reminder cancel, no widget update — second tap is fully absorbed.
        assertFalse(scheduler.cancelledTaskIds.contains(1L))
        assertEquals(0, widgetUpdater.updateCount)
    }

    @Test
    fun `complete on missing task returns false with no side effects`() = runTest {
        val applied = handler.complete(999L)

        assertFalse(applied)
        assertTrue(scheduler.cancelledTaskIds.isEmpty())
        assertEquals(0, widgetUpdater.updateCount)
    }

    @Test
    fun `complete with non-positive id returns false without dao lookup`() = runTest {
        val applied0 = handler.complete(0L)
        val appliedNeg = handler.complete(-5L)

        assertFalse(applied0)
        assertFalse(appliedNeg)
        assertEquals(0, widgetUpdater.updateCount)
    }

    @Test
    fun `rapid double-tap results in exactly one completion`() = runTest {
        // FR-12: second tap after the first persisted should be absorbed by the
        // isCompleted guard. Sequential here represents the realistic case where
        // the second callback observes already-persisted state.
        val task = TestTaskFactory.createTask(id = 1, title = "Once")
        repository.seed(task)

        val first = handler.complete(1L)
        val second = handler.complete(1L)

        assertTrue(first)
        assertFalse(second)
        // Reminder cancel and widget update each fire exactly once.
        assertEquals(1, scheduler.cancelledTaskIds.count { it == 1L })
        assertEquals(1, widgetUpdater.updateCount)
        assertTrue(repository.getTaskById(1L)!!.isCompleted)
    }

    // ── V13-06 additions ──────────────────────────────────────────────────────

    @Test
    fun `cancel reminder precedes persistence and widget update (ordering invariant)`() = runTest {
        val callLog = mutableListOf<String>()
        val trackingScheduler = object : FakeReminderScheduler() {
            override suspend fun cancel(taskId: Long) {
                callLog.add("cancel")
                super.cancel(taskId)
            }
        }
        val trackingUpdater = object : FakeWidgetUpdater() {
            override suspend fun requestUpdate() {
                callLog.add("update")
                super.requestUpdate()
            }
        }
        val trackingRepository = object : FakeTaskRepository() {
            override suspend fun updateTask(task: Task) {
                callLog.add("persist")
                super.updateTask(task)
            }
        }
        val task = TestTaskFactory.createTask(id = 1, title = "Ordered", reminderOffsetMinutes = 15)
        trackingRepository.seed(task)
        val trackingManager = TaskManager(
            trackingRepository,
            FakeSubtaskRepository(),
            trackingScheduler,
            trackingUpdater,
            fakeBreadcrumbLogger(),
            fakeAnalyticsLogger(),
        )
        val trackingHandler = WidgetCompleteHandler(trackingManager)

        trackingHandler.complete(1L)

        assertEquals(listOf("cancel", "persist", "update"), callLog)
    }

    @Test
    fun `timed double-tap with delay still results in exactly one completion`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Timed tap")
        repository.seed(task)

        val first = handler.complete(1L)
        advanceTimeBy(500)
        val second = handler.complete(1L)

        assertTrue(first)
        assertFalse(second)
        assertEquals(1, widgetUpdater.updateCount)
        assertTrue(repository.getTaskById(1L)!!.isCompleted)
    }

    @Test
    fun `no reminder is rescheduled after successful completion (NFR-08)`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "No reschedule",
            dueAt = System.currentTimeMillis() + 3_600_000L,
            reminderOffsetMinutes = 15,
        )
        repository.seed(task)

        handler.complete(1L)

        assertTrue(scheduler.scheduledReminders.isEmpty())
        assertTrue(scheduler.cancelledTaskIds.contains(1L))
    }
}
