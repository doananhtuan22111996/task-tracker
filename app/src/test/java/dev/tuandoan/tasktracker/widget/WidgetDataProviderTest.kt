package dev.tuandoan.tasktracker.widget

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.testutil.TestTaskFactory.ONE_DAY_MS
import dev.tuandoan.tasktracker.widget.model.WidgetSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetDataProviderTest {

    private lateinit var fakeDao: FakeWidgetTaskDao
    private lateinit var dataProvider: WidgetDataProvider

    @Before
    fun setup() {
        fakeDao = FakeWidgetTaskDao()
        dataProvider = WidgetDataProvider(fakeDao, now = { TestTaskFactory.BASE_TIMESTAMP })
    }

    @Test
    fun `empty database returns empty list`() = runTest {
        val result = dataProvider.getWidgetTasks(WidgetSource.Today)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns active tasks only, excludes completed`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.completedTask(id = 2, title = "Completed"),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Today)

        assertEquals(1, result.size)
        assertEquals("Active", result[0].title)
    }

    @Test
    fun `excludes archived tasks`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.archivedTask(id = 2, title = "Archived"),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Today)

        assertEquals(1, result.size)
        assertEquals("Active", result[0].title)
    }

    @Test
    fun `returns max 5 tasks`() = runTest {
        fakeDao.tasks = (1..10).map { i ->
            TestTaskFactory.createTask(id = i.toLong(), title = "Task $i")
        }

        val result = dataProvider.getWidgetTasks(WidgetSource.Today)

        assertEquals(5, result.size)
    }

    @Test
    fun `pinned tasks appear first`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(
                id = 1,
                title = "Normal",
                dueAt = TestTaskFactory.BASE_TIMESTAMP + ONE_DAY_MS,
            ),
            TestTaskFactory.createTask(
                id = 2,
                title = "Pinned",
                isPinned = true,
                dueAt = TestTaskFactory.BASE_TIMESTAMP + 2 * ONE_DAY_MS,
            ),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Today)

        assertEquals("Pinned", result[0].title)
        assertTrue(result[0].isPinned)
    }

    @Test
    fun `tasks sorted by due date ascending after pin`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(
                id = 1,
                title = "Later",
                dueAt = TestTaskFactory.BASE_TIMESTAMP + 3 * ONE_DAY_MS,
            ),
            TestTaskFactory.createTask(
                id = 2,
                title = "Sooner",
                dueAt = TestTaskFactory.BASE_TIMESTAMP + ONE_DAY_MS,
            ),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Today)

        assertEquals("Sooner", result[0].title)
        assertEquals("Later", result[1].title)
    }

    @Test
    fun `tasks without due date appear last`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "No date"),
            TestTaskFactory.createTask(
                id = 2,
                title = "Has date",
                dueAt = TestTaskFactory.BASE_TIMESTAMP + ONE_DAY_MS,
            ),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Today)

        assertEquals("Has date", result[0].title)
        assertEquals("No date", result[1].title)
    }

    @Test
    fun `maps task fields correctly to WidgetTask`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(
                id = 42,
                title = "Buy groceries",
                dueAt = TestTaskFactory.BASE_TIMESTAMP + ONE_DAY_MS,
                dueAtHasTime = true,
                priority = 2,
                isPinned = true,
            ),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Today)

        assertEquals(1, result.size)
        val task = result[0]
        assertEquals(42L, task.id)
        assertEquals("Buy groceries", task.title)
        assertEquals(TestTaskFactory.BASE_TIMESTAMP + ONE_DAY_MS, task.dueAt)
        assertTrue(task.dueAtHasTime)
        assertEquals(2, task.priority)
        assertTrue(task.isPinned)
    }

    @Test
    fun `custom limit is respected`() = runTest {
        fakeDao.tasks = (1..10).map { i ->
            TestTaskFactory.createTask(id = i.toLong(), title = "Task $i")
        }

        val result = dataProvider.getWidgetTasks(WidgetSource.Today, limit = 3)

        assertEquals(3, result.size)
    }

    // ── V13-03: WidgetSource dispatch smoke tests ────────────────────────────
    // V13-13 owns the deeper query-semantic coverage for each source. These
    // tests just pin that the provider routes each WidgetSource variant to the
    // matching DAO method (no fall-through, no crossed wires).

    @Test
    fun `Upcoming7d source routes to getWidgetTasksUpcoming with now and now+week window`() = runTest {
        dataProvider.getWidgetTasks(WidgetSource.Upcoming7d, limit = 4)

        val call = fakeDao.upcomingCalls.single()
        assertEquals(TestTaskFactory.BASE_TIMESTAMP, call.now)
        assertEquals(TestTaskFactory.BASE_TIMESTAMP + 7L * 24 * 60 * 60 * 1000, call.until)
        assertEquals(4, call.limit)
    }

    @Test
    fun `Pinned source routes to getWidgetTasksPinned`() = runTest {
        dataProvider.getWidgetTasks(WidgetSource.Pinned, limit = 7)

        assertEquals(listOf(7), fakeDao.pinnedLimits)
    }

    @Test
    fun `Tag source routes to getWidgetTasksByTag with the normalized name`() = runTest {
        dataProvider.getWidgetTasks(WidgetSource.Tag("WORK"), limit = 5)

        assertEquals(listOf("WORK" to 5), fakeDao.tagCalls)
    }

    @Test
    fun `Tag source with blank name returns empty without touching dao`() = runTest {
        val result = dataProvider.getWidgetTasks(WidgetSource.Tag(""), limit = 5)

        assertTrue(result.isEmpty())
        assertTrue(fakeDao.tagCalls.isEmpty())
    }
}

/**
 * Fake TaskDao that simulates the widget query behavior:
 * filters active (non-completed, non-archived), sorts by isPinned DESC then dueAt ASC (nulls last),
 * and limits results.
 */
private class FakeWidgetTaskDao : dev.tuandoan.tasktracker.data.database.TaskDao {

    var tasks: List<Task> = emptyList()

    /** V13-03 dispatch tracking — V13-13 will replace these with real query simulation. */
    data class UpcomingCall(val now: Long, val until: Long, val limit: Int)
    val upcomingCalls = mutableListOf<UpcomingCall>()
    val pinnedLimits = mutableListOf<Int>()
    val tagCalls = mutableListOf<Pair<String, Int>>()

    override suspend fun getWidgetTasks(limit: Int): List<Task> = tasks
        .filter { !it.isCompleted && !it.isArchived }
        .sortedWith(
            compareByDescending<Task> { it.isPinned }
                .thenBy(nullsLast()) { it.dueAt },
        )
        .take(limit)

    override suspend fun getWidgetTasksUpcoming(nowMillis: Long, untilMillis: Long, limit: Int): List<Task> {
        upcomingCalls.add(UpcomingCall(nowMillis, untilMillis, limit))
        return emptyList()
    }

    override suspend fun getWidgetTasksPinned(limit: Int): List<Task> {
        pinnedLimits.add(limit)
        return emptyList()
    }

    override suspend fun getWidgetTasksByTag(tag: String, limit: Int): List<Task> {
        tagCalls.add(tag to limit)
        return emptyList()
    }

    // Unused stubs — only getWidgetTasks matters for this test
    override fun getAllTasks() = throw UnsupportedOperationException()
    override suspend fun getTaskById(id: Long) = throw UnsupportedOperationException()
    override suspend fun insertTask(task: Task) = throw UnsupportedOperationException()
    override suspend fun updateTask(task: Task) = throw UnsupportedOperationException()
    override suspend fun deleteTask(task: Task) = throw UnsupportedOperationException()
    override suspend fun upsert(task: Task) = throw UnsupportedOperationException()
    override fun getActiveTasks() = throw UnsupportedOperationException()
    override fun getCompletedTasks() = throw UnsupportedOperationException()
    override fun getArchivedTasks() = throw UnsupportedOperationException()
    override suspend fun markCompleted(ids: List<Long>, completedAt: Long) = throw UnsupportedOperationException()
    override suspend fun markActive(ids: List<Long>) = throw UnsupportedOperationException()
    override suspend fun deleteByIds(ids: List<Long>) = throw UnsupportedOperationException()
    override suspend fun getTasksByIds(ids: List<Long>) = throw UnsupportedOperationException()
    override suspend fun upsertAll(tasks: List<Task>) = throw UnsupportedOperationException()
    override suspend fun setArchived(id: Long, archived: Boolean, archivedAt: Long?) =
        throw UnsupportedOperationException()
    override suspend fun setArchivedBulk(ids: List<Long>, archived: Boolean, archivedAt: Long?) =
        throw UnsupportedOperationException()
    override suspend fun hardDeleteById(id: Long) = throw UnsupportedOperationException()
    override suspend fun hardDeleteByIds(ids: List<Long>) = throw UnsupportedOperationException()
    override suspend fun setPinned(id: Long, pinned: Boolean) = throw UnsupportedOperationException()
    override suspend fun setPriority(id: Long, priority: Int) = throw UnsupportedOperationException()
    override suspend fun setPriorityBulk(ids: List<Long>, priority: Int) = throw UnsupportedOperationException()
    override suspend fun setTagBulk(ids: List<Long>, tag: String?, tagColor: String?) =
        throw UnsupportedOperationException()
    override fun observeActiveCount() = throw UnsupportedOperationException()
    override fun observeCompletedCount() = throw UnsupportedOperationException()
    override fun observeCompletedTodayCount(startOfDayMillis: Long, endOfDayMillis: Long) =
        throw UnsupportedOperationException()
    override fun observeDueTodayCount(startOfDayMillis: Long, endOfDayMillis: Long) =
        throw UnsupportedOperationException()
    override fun observeOverdueCount(nowMillis: Long) = throw UnsupportedOperationException()
    override fun observeCompletedCountPerDay(startMillis: Long, endMillis: Long) = throw UnsupportedOperationException()
    override fun observeTasksInRange(startMillis: Long, endMillis: Long) = throw UnsupportedOperationException()
    override fun observeDatedTaskCount() = throw UnsupportedOperationException()
    override suspend fun getLatestGeneratedTask(parentId: Long) = throw UnsupportedOperationException()
    override suspend fun getCompletedTasksByChain(rootId: Long) = throw UnsupportedOperationException()
    override suspend fun getCompletedTasksForChains(rootIds: List<Long>) = throw UnsupportedOperationException()
    override suspend fun getActiveRecurringRootIds() = throw UnsupportedOperationException()
    override suspend fun findChainTaskOnDate(rootId: Long, startMillis: Long, endMillis: Long) =
        throw UnsupportedOperationException()
    override fun getDistinctTagsWithCount() = throw UnsupportedOperationException()
    override suspend fun updateTagName(oldName: String, newName: String) = throw UnsupportedOperationException()
    override suspend fun clearTag(tagName: String) = throw UnsupportedOperationException()
    override suspend fun updateTagColor(tagName: String, color: String?) = throw UnsupportedOperationException()
    override suspend fun getTagColor(tagName: String) = throw UnsupportedOperationException()
    override suspend fun getAllTasksIncludingArchived() = throw UnsupportedOperationException()
    override suspend fun deleteAllTasks() = throw UnsupportedOperationException()
}
