package dev.tuandoan.tasktracker.ui.viewmodel

import app.cash.turbine.test
import dev.tuandoan.tasktracker.data.database.DailyCount
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.data.preferences.UserPreferences
import dev.tuandoan.tasktracker.domain.ITaskManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeTaskManager: FakeStatsTaskManager
    private lateinit var viewModel: StatsViewModel
    private val fakeSettingsRepository: SettingsRepository = mockk(relaxed = true) {
        every { userPreferences } returns MutableStateFlow(UserPreferences())
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskManager = FakeStatsTaskManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): StatsViewModel = StatsViewModel(fakeTaskManager, fakeSettingsRepository)

    // SPEC-S03: Daily progress tests
    @Test
    fun `dailyProgress is 0_5 when completedToday 3 and dueToday 6`() = runTest {
        fakeTaskManager.setStats(completedTodayCount = 3, dueTodayCount = 6)
        viewModel = createViewModel()

        viewModel.dailyProgress.test {
            assertEquals(0.5f, awaitItem(), 0.01f)
        }
    }

    @Test
    fun `dailyProgress is 1_0 when dueTodayCount is 0`() = runTest {
        fakeTaskManager.setStats(completedTodayCount = 0, dueTodayCount = 0)
        viewModel = createViewModel()

        viewModel.dailyProgress.test {
            assertEquals(1.0f, awaitItem(), 0.01f)
        }
    }

    @Test
    fun `dailyProgress is clamped to 1_0 when completedToday exceeds dueToday`() = runTest {
        fakeTaskManager.setStats(completedTodayCount = 10, dueTodayCount = 5)
        viewModel = createViewModel()

        viewModel.dailyProgress.test {
            assertEquals(1.0f, awaitItem(), 0.01f)
        }
    }

    // SPEC-S05: Completion rate tests
    @Test
    fun `completionRate is 75 when completedCount 3 and activeCount 1`() = runTest {
        fakeTaskManager.setStats(completedCount = 3, activeCount = 1)
        viewModel = createViewModel()

        viewModel.completionRate.test {
            assertEquals(75, awaitItem())
        }
    }

    @Test
    fun `completionRate is null when both counts are 0`() = runTest {
        fakeTaskManager.setStats(completedCount = 0, activeCount = 0)
        viewModel = createViewModel()

        viewModel.completionRate.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `completionRate is 100 when activeCount is 0 and completedCount greater than 0`() = runTest {
        fakeTaskManager.setStats(completedCount = 5, activeCount = 0)
        viewModel = createViewModel()

        viewModel.completionRate.test {
            assertEquals(100, awaitItem())
        }
    }

    // SPEC-S07: Empty state tests
    @Test
    fun `isEmpty is true when both activeCount and completedCount are 0`() = runTest {
        fakeTaskManager.setStats(activeCount = 0, completedCount = 0)
        viewModel = createViewModel()

        viewModel.isEmpty.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `isEmpty is false when activeCount greater than 0`() = runTest {
        fakeTaskManager.setStats(activeCount = 3, completedCount = 0)
        viewModel = createViewModel()

        viewModel.isEmpty.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `isEmpty is false when completedCount greater than 0 and activeCount is 0`() = runTest {
        fakeTaskManager.setStats(activeCount = 0, completedCount = 5)
        viewModel = createViewModel()

        viewModel.isEmpty.test {
            assertFalse(awaitItem())
        }
    }

    // uiState tests
    @Test
    fun `uiState combines all stat counts correctly`() = runTest {
        fakeTaskManager.setStats(
            activeCount = 10,
            completedCount = 5,
            completedTodayCount = 2,
            dueTodayCount = 3,
            overdueCount = 1,
        )
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(10, state.activeCount)
            assertEquals(5, state.completedCount)
            assertEquals(2, state.completedTodayCount)
            assertEquals(3, state.dueTodayCount)
            assertEquals(1, state.overdueCount)
        }
    }

    @Test
    fun `completionRate is 50 when completedCount equals activeCount`() = runTest {
        fakeTaskManager.setStats(completedCount = 4, activeCount = 4)
        viewModel = createViewModel()

        viewModel.completionRate.test {
            assertEquals(50, awaitItem())
        }
    }

    @Test
    fun `dailyProgress is 0 when completedToday is 0 and dueToday is positive`() = runTest {
        fakeTaskManager.setStats(completedTodayCount = 0, dueTodayCount = 10)
        viewModel = createViewModel()

        viewModel.dailyProgress.test {
            assertEquals(0f, awaitItem(), 0.01f)
        }
    }
}

private class FakeStatsTaskManager : ITaskManager {

    private val activeCountFlow = MutableStateFlow(0)
    private val completedCountFlow = MutableStateFlow(0)
    private val completedTodayCountFlow = MutableStateFlow(0)
    private val dueTodayCountFlow = MutableStateFlow(0)
    private val overdueCountFlow = MutableStateFlow(0)
    private val completedPerDayFlow = MutableStateFlow<List<DailyCount>>(emptyList())

    fun setStats(
        activeCount: Int = activeCountFlow.value,
        completedCount: Int = completedCountFlow.value,
        completedTodayCount: Int = completedTodayCountFlow.value,
        dueTodayCount: Int = dueTodayCountFlow.value,
        overdueCount: Int = overdueCountFlow.value,
    ) {
        activeCountFlow.value = activeCount
        completedCountFlow.value = completedCount
        completedTodayCountFlow.value = completedTodayCount
        dueTodayCountFlow.value = dueTodayCount
        overdueCountFlow.value = overdueCount
    }

    override fun observeActiveCount(): Flow<Int> = activeCountFlow
    override fun observeCompletedCount(): Flow<Int> = completedCountFlow
    override fun observeCompletedTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> =
        completedTodayCountFlow
    override fun observeDueTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> = dueTodayCountFlow
    override fun observeOverdueCount(nowMillis: Long): Flow<Int> = overdueCountFlow
    override fun observeCompletedCountPerDay(startMillis: Long, endMillis: Long): Flow<List<DailyCount>> =
        completedPerDayFlow

    override fun getAllTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override suspend fun getTaskById(id: Long): Task? = null
    override suspend fun createTask(title: String, description: String): Long = 0
    override suspend fun createTask(
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
    ): Long = 0
    override suspend fun createTask(
        title: String,
        description: String,
        dueAt: Long?,
        dueAtHasTime: Boolean,
        reminderOffsetMinutes: Int?,
        tag: String?,
    ): Long = 0
    override suspend fun updateTask(task: Task) {}
    override suspend fun updateTaskContent(taskId: Long, title: String, description: String) {}
    override suspend fun updateTaskContent(
        taskId: Long,
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
    ): Boolean = true
    override suspend fun updateTaskContent(
        taskId: Long,
        title: String,
        description: String,
        dueAt: Long?,
        dueAtHasTime: Boolean,
        reminderOffsetMinutes: Int?,
        tag: String?,
    ): Boolean = true
    override suspend fun deleteTask(task: Task) {}
    override suspend fun restoreTask(task: Task): Result<Unit> = Result.success(Unit)
    override suspend fun toggleTaskCompletion(task: Task) {}
    override suspend fun markTaskComplete(task: Task) {}
    override suspend fun markTaskIncomplete(task: Task) {}
    override suspend fun setCompletedBulk(ids: List<Long>, completed: Boolean) {}
    override suspend fun deleteTasksByIds(ids: List<Long>) {}
    override suspend fun restoreTasks(tasks: List<Task>): Result<Unit> = Result.success(Unit)
    override fun getActiveTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override fun getCompletedTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override suspend fun setPinned(taskId: Long, pinned: Boolean) {}
    override suspend fun setPriority(taskId: Long, priority: Int) {}
    override fun getArchivedTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override suspend fun archiveTask(taskId: Long) {}
    override suspend fun unarchiveTask(taskId: Long) {}
    override suspend fun archiveTasks(ids: List<Long>) {}
    override suspend fun unarchiveTasks(ids: List<Long>) {}
    override suspend fun hardDeleteTask(taskId: Long) {}
    override suspend fun hardDeleteTasks(ids: List<Long>) {}
}
