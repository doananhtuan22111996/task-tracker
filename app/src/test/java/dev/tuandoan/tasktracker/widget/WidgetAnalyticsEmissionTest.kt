package dev.tuandoan.tasktracker.widget

import dev.tuandoan.tasktracker.diagnostics.AnalyticsEvent
import dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger
import dev.tuandoan.tasktracker.diagnostics.WidgetAnalyticsSource
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeSubtaskRepository
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.FakeWidgetUpdater
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.testutil.fakeAnalyticsLogger
import dev.tuandoan.tasktracker.testutil.fakeBreadcrumbLogger
import dev.tuandoan.tasktracker.widget.action.WidgetCompleteHandler
import dev.tuandoan.tasktracker.widget.model.WidgetSource
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Call-site emission tests for the V13-14 widget analytics events (V13-15).
 *
 * Mirrors the FB-14 pattern from [dev.tuandoan.tasktracker.domain.TaskManagerTest]:
 * - MockK [AnalyticsLogger] receives the expected event type and params.
 * - Negative invariants confirm no PII fields (title, tag name, description) leak.
 * - No-op paths (already-completed, missing task) confirm no spurious events.
 *
 * [WidgetConfigureViewModel] events are tested separately in
 * [WidgetConfigureViewModelAnalyticsTest].
 */
class WidgetAnalyticsEmissionTest {

    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    private val repository = FakeTaskRepository()
    private val taskManager = TaskManager(
        repository = repository,
        subtaskRepository = FakeSubtaskRepository(),
        reminderScheduler = FakeReminderScheduler(),
        widgetUpdater = FakeWidgetUpdater(),
        breadcrumbLogger = fakeBreadcrumbLogger(),
        analyticsLogger = fakeAnalyticsLogger(),
    )
    private val handler = WidgetCompleteHandler(taskManager, analyticsLogger)

    // ── WidgetCompleteHandler — happy path ────────────────────────────────────

    @Test
    fun `complete emits WidgetTaskCompleted on successful completion`() = runTest {
        repository.seed(TestTaskFactory.createTask(id = 1, title = "Sensitive title"))

        handler.complete(1L)

        verify(exactly = 1) { analyticsLogger.log(AnalyticsEvent.WidgetTaskCompleted) }
    }

    @Test
    fun `WidgetTaskCompleted params carry no PII`() {
        val event = AnalyticsEvent.WidgetTaskCompleted
        // Zero params — the event itself is the signal; no content to leak.
        assertFalse(event.params.containsKey("title"))
        assertFalse(event.params.containsKey("tag"))
        assertFalse(event.params.containsKey("description"))
        assertFalse(event.params.containsKey("task_id"))
    }

    // ── WidgetCompleteHandler — no-op paths ───────────────────────────────────

    @Test
    fun `complete does NOT emit WidgetTaskCompleted when task is already completed`() = runTest {
        repository.seed(TestTaskFactory.completedTask(id = 2))

        handler.complete(2L)

        verify(exactly = 0) { analyticsLogger.log(AnalyticsEvent.WidgetTaskCompleted) }
    }

    @Test
    fun `complete does NOT emit WidgetTaskCompleted when task id is not found`() = runTest {
        handler.complete(99L)

        verify(exactly = 0) { analyticsLogger.log(AnalyticsEvent.WidgetTaskCompleted) }
    }

    @Test
    fun `complete does NOT emit WidgetTaskCompleted when task id is zero`() = runTest {
        handler.complete(0L)

        verify(exactly = 0) { analyticsLogger.log(AnalyticsEvent.WidgetTaskCompleted) }
    }
}

/**
 * Call-site emission tests for [dev.tuandoan.tasktracker.widget.ui.WidgetConfigureViewModel]
 * analytics events (V13-15).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigureViewModelAnalyticsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    private fun buildViewModel(): dev.tuandoan.tasktracker.widget.ui.WidgetConfigureViewModel {
        val configRepository = dev.tuandoan.tasktracker.data.preferences.WidgetConfigurationRepository(
            InMemoryDataStore(),
        )
        val tagUseCase = dev.tuandoan.tasktracker.domain.usecase.TagManagementUseCase(
            FakeTaskRepository(),
        )
        return dev.tuandoan.tasktracker.widget.ui.WidgetConfigureViewModel(
            configRepository = configRepository,
            analyticsLogger = analyticsLogger,
            tagManagementUseCase = tagUseCase,
        )
    }

    // ── widget_added + widget_configured on confirm ───────────────────────────

    @Test
    fun `confirm emits WidgetAdded on success`() = runTest {
        buildViewModel().confirm(appWidgetId = 1)

        verify(exactly = 1) { analyticsLogger.log(AnalyticsEvent.WidgetAdded) }
    }

    @Test
    fun `confirm emits WidgetConfigured with Today source by default`() = runTest {
        buildViewModel().confirm(appWidgetId = 1)

        verify(exactly = 1) {
            analyticsLogger.log(AnalyticsEvent.WidgetConfigured(WidgetAnalyticsSource.TODAY))
        }
    }

    @Test
    fun `confirm emits WidgetConfigured with Upcoming7d source`() = runTest {
        val vm = buildViewModel()
        vm.setSelection(WidgetSource.Upcoming7d)
        vm.confirm(appWidgetId = 1)

        verify(exactly = 1) {
            analyticsLogger.log(AnalyticsEvent.WidgetConfigured(WidgetAnalyticsSource.UPCOMING_7D))
        }
    }

    @Test
    fun `confirm emits WidgetConfigured with Pinned source`() = runTest {
        val vm = buildViewModel()
        vm.setSelection(WidgetSource.Pinned)
        vm.confirm(appWidgetId = 1)

        verify(exactly = 1) {
            analyticsLogger.log(AnalyticsEvent.WidgetConfigured(WidgetAnalyticsSource.PINNED))
        }
    }

    @Test
    fun `confirm emits WidgetConfigured with Tag source — tag name collapsed to enum`() = runTest {
        val vm = buildViewModel()
        vm.setSelection(WidgetSource.Tag("WORK"))
        vm.confirm(appWidgetId = 1)

        // The tag name "WORK" must NOT appear in any param; source collapses to "tag".
        verify(exactly = 1) {
            analyticsLogger.log(AnalyticsEvent.WidgetConfigured(WidgetAnalyticsSource.TAG))
        }
    }

    @Test
    fun `WidgetConfigured params carry no PII — tag name never leaks`() {
        val event = AnalyticsEvent.WidgetConfigured(WidgetAnalyticsSource.TAG)
        // Only PARAM_WIDGET_SOURCE ("source") is present; no tag name or raw string.
        assertFalse(event.params.containsKey("tag"))
        assertFalse(event.params.containsKey("name"))
        assertFalse(event.params.containsKey("tag_name"))
    }

    @Test
    fun `WidgetAdded params carry no PII`() {
        assertFalse(AnalyticsEvent.WidgetAdded.params.containsKey("widget_id"))
        assertFalse(AnalyticsEvent.WidgetAdded.params.containsKey("source"))
    }
}

// Minimal in-memory DataStore for test isolation (mirrors WidgetCleanupHandlerTest pattern).
private class InMemoryDataStore :
    androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {

    private val _data = kotlinx.coroutines.flow.MutableStateFlow(
        androidx.datastore.preferences.core.emptyPreferences(),
    )
    override val data: kotlinx.coroutines.flow.Flow<androidx.datastore.preferences.core.Preferences> = _data

    override suspend fun updateData(
        transform:
        suspend (t: androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences,
    ): androidx.datastore.preferences.core.Preferences {
        val updated = transform(_data.value)
        _data.value = updated
        return updated
    }
}
