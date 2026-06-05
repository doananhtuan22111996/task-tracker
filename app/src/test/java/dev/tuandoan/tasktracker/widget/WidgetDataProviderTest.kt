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

    private companion object {
        // Matches WidgetSizeResolver.FETCH_LIMIT — production callers always pass
        // that. Tests use it as the implicit-default replacement after V13-03
        // dropped the misleading MAX_WIDGET_TASKS=5 default.
        const val DEFAULT_LIMIT = 10
        val ONE_DAY_MS = TestTaskFactory.ONE_DAY_MS
    }

    @Test
    fun `empty database returns empty list`() = runTest {
        val result = dataProvider.getWidgetTasks(WidgetSource.Today, limit = DEFAULT_LIMIT)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns active tasks only, excludes completed`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.completedTask(id = 2, title = "Completed"),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Today, limit = DEFAULT_LIMIT)

        assertEquals(1, result.size)
        assertEquals("Active", result[0].title)
    }

    @Test
    fun `excludes archived tasks`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.archivedTask(id = 2, title = "Archived"),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Today, limit = DEFAULT_LIMIT)

        assertEquals(1, result.size)
        assertEquals("Active", result[0].title)
    }

    @Test
    fun `respects the passed limit when more tasks exist`() = runTest {
        // Seed 20 (twice DEFAULT_LIMIT) to verify capping kicks in.
        fakeDao.tasks = (1..20).map { i ->
            TestTaskFactory.createTask(id = i.toLong(), title = "Task $i")
        }

        val result = dataProvider.getWidgetTasks(WidgetSource.Today, limit = DEFAULT_LIMIT)

        assertEquals(DEFAULT_LIMIT, result.size)
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

        val result = dataProvider.getWidgetTasks(WidgetSource.Today, limit = DEFAULT_LIMIT)

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

        val result = dataProvider.getWidgetTasks(WidgetSource.Today, limit = DEFAULT_LIMIT)

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

        val result = dataProvider.getWidgetTasks(WidgetSource.Today, limit = DEFAULT_LIMIT)

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

        val result = dataProvider.getWidgetTasks(WidgetSource.Today, limit = DEFAULT_LIMIT)

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

    // ── V13-13: WidgetSource query-semantic tests ─────────────────────────────
    // These replace the empty-stub dispatches with real query simulation and
    // pin the semantic edges documented in the PRD + V13-03 notes.

    // ── Upcoming7d ────────────────────────────────────────────────────────────

    @Test
    fun `Upcoming7d returns only tasks with dueAt inside the 7-day window`() = runTest {
        val now = TestTaskFactory.BASE_TIMESTAMP
        val week = 7L * ONE_DAY_MS
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "In window", dueAt = now + ONE_DAY_MS),
            TestTaskFactory.createTask(id = 2, title = "Exactly now", dueAt = now),
            TestTaskFactory.createTask(id = 3, title = "Just before end", dueAt = now + week - 1),
            TestTaskFactory.createTask(id = 4, title = "At end (exclusive)", dueAt = now + week),
            TestTaskFactory.createTask(id = 5, title = "Past window", dueAt = now + week + ONE_DAY_MS),
            TestTaskFactory.createTask(id = 6, title = "No due date"),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Upcoming7d, limit = DEFAULT_LIMIT)

        val titles = result.map { it.title }
        assertTrue("In window" in titles)
        assertTrue("Exactly now" in titles)
        assertTrue("Just before end" in titles)
        assertTrue("At end (exclusive)" !in titles)
        assertTrue("Past window" !in titles)
        assertTrue("No due date" !in titles)
    }

    @Test
    fun `Upcoming7d excludes completed tasks`() = runTest {
        val now = TestTaskFactory.BASE_TIMESTAMP
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active", dueAt = now + ONE_DAY_MS),
            TestTaskFactory.completedTask(id = 2, title = "Completed").copy(dueAt = now + ONE_DAY_MS),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Upcoming7d, limit = DEFAULT_LIMIT)

        assertEquals(1, result.size)
        assertEquals("Active", result[0].title)
    }

    @Test
    fun `Upcoming7d excludes archived tasks`() = runTest {
        val now = TestTaskFactory.BASE_TIMESTAMP
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active", dueAt = now + ONE_DAY_MS),
            TestTaskFactory.archivedTask(id = 2, title = "Archived").copy(dueAt = now + ONE_DAY_MS),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Upcoming7d, limit = DEFAULT_LIMIT)

        assertEquals(1, result.size)
        assertEquals("Active", result[0].title)
    }

    @Test
    fun `Upcoming7d pinned tasks float to top within window`() = runTest {
        val now = TestTaskFactory.BASE_TIMESTAMP
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Normal", dueAt = now + ONE_DAY_MS),
            TestTaskFactory.createTask(id = 2, title = "Pinned later", dueAt = now + 2 * ONE_DAY_MS, isPinned = true),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Upcoming7d, limit = DEFAULT_LIMIT)

        assertEquals("Pinned later", result[0].title)
        assertEquals("Normal", result[1].title)
    }

    @Test
    fun `Upcoming7d respects limit`() = runTest {
        val now = TestTaskFactory.BASE_TIMESTAMP
        fakeDao.tasks = (1..10).map { i ->
            TestTaskFactory.createTask(id = i.toLong(), title = "Task $i", dueAt = now + i * ONE_DAY_MS)
        }

        val result = dataProvider.getWidgetTasks(WidgetSource.Upcoming7d, limit = 3)

        assertEquals(3, result.size)
    }

    // ── Pinned ────────────────────────────────────────────────────────────────

    @Test
    fun `Pinned returns only pinned active tasks`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Pinned", isPinned = true),
            TestTaskFactory.createTask(id = 2, title = "Not pinned", isPinned = false),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Pinned, limit = DEFAULT_LIMIT)

        assertEquals(1, result.size)
        assertEquals("Pinned", result[0].title)
    }

    @Test
    fun `Pinned excludes completed tasks`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active pinned", isPinned = true),
            TestTaskFactory.completedTask(id = 2, title = "Completed pinned").copy(isPinned = true),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Pinned, limit = DEFAULT_LIMIT)

        assertEquals(1, result.size)
        assertEquals("Active pinned", result[0].title)
    }

    @Test
    fun `Pinned pinned undated tasks appear after pinned dated tasks`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Pinned undated", isPinned = true),
            TestTaskFactory.createTask(
                id = 2,
                title = "Pinned dated",
                isPinned = true,
                dueAt = TestTaskFactory.BASE_TIMESTAMP + ONE_DAY_MS,
            ),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Pinned, limit = DEFAULT_LIMIT)

        assertEquals("Pinned dated", result[0].title)
        assertEquals("Pinned undated", result[1].title)
    }

    @Test
    fun `Pinned respects limit`() = runTest {
        fakeDao.tasks = (1..10).map { i ->
            TestTaskFactory.createTask(id = i.toLong(), title = "Pinned $i", isPinned = true)
        }

        val result = dataProvider.getWidgetTasks(WidgetSource.Pinned, limit = 4)

        assertEquals(4, result.size)
    }

    // ── Tag ───────────────────────────────────────────────────────────────────

    @Test
    fun `Tag returns only tasks matching the exact tag name`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Work task", tag = "work"),
            TestTaskFactory.createTask(id = 2, title = "Personal task", tag = "personal"),
            TestTaskFactory.createTask(id = 3, title = "No tag"),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Tag("work"), limit = DEFAULT_LIMIT)

        assertEquals(1, result.size)
        assertEquals("Work task", result[0].title)
    }

    @Test
    fun `Tag excludes completed and archived tasks`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active", tag = "work"),
            TestTaskFactory.completedTask(id = 2, title = "Completed").copy(tag = "work"),
            TestTaskFactory.archivedTask(id = 3, title = "Archived").copy(tag = "work"),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Tag("work"), limit = DEFAULT_LIMIT)

        assertEquals(1, result.size)
        assertEquals("Active", result[0].title)
    }

    @Test
    fun `Tag pinned tasks float to top within tag`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Normal work", tag = "work"),
            TestTaskFactory.createTask(id = 2, title = "Pinned work", tag = "work", isPinned = true),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Tag("work"), limit = DEFAULT_LIMIT)

        assertEquals("Pinned work", result[0].title)
        assertEquals("Normal work", result[1].title)
    }

    @Test
    fun `Tag undated tasks appear last within tag`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Tag no date", tag = "work"),
            TestTaskFactory.createTask(
                id = 2,
                title = "Tag with date",
                tag = "work",
                dueAt = TestTaskFactory.BASE_TIMESTAMP + ONE_DAY_MS,
            ),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Tag("work"), limit = DEFAULT_LIMIT)

        assertEquals("Tag with date", result[0].title)
        assertEquals("Tag no date", result[1].title)
    }

    @Test
    fun `Tag tag comparison is exact — different case does not match`() = runTest {
        fakeDao.tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Lowercase", tag = "work"),
            TestTaskFactory.createTask(id = 2, title = "Uppercase", tag = "WORK"),
        )

        val result = dataProvider.getWidgetTasks(WidgetSource.Tag("work"), limit = DEFAULT_LIMIT)

        assertEquals(1, result.size)
        assertEquals("Lowercase", result[0].title)
    }

    @Test
    fun `Tag respects limit`() = runTest {
        fakeDao.tasks = (1..10).map { i ->
            TestTaskFactory.createTask(id = i.toLong(), title = "Work $i", tag = "work")
        }

        val result = dataProvider.getWidgetTasks(WidgetSource.Tag("work"), limit = 5)

        assertEquals(5, result.size)
    }
}

/**
 * Fake TaskDao that simulates all four widget query semantics (V13-13).
 * Each method mirrors the SQL logic in TaskDao exactly so test failures
 * reflect real behavioral regressions, not stub gaps.
 */
private class FakeWidgetTaskDao : dev.tuandoan.tasktracker.data.database.TaskDao {

    var tasks: List<Task> = emptyList()

    // Dispatch call-tracking kept for V13-03 smoke tests above.
    data class UpcomingCall(val now: Long, val until: Long, val limit: Int)
    val upcomingCalls = mutableListOf<UpcomingCall>()
    val pinnedLimits = mutableListOf<Int>()
    val tagCalls = mutableListOf<Pair<String, Int>>()

    private fun List<Task>.activeOnly() = filter { !it.isCompleted && !it.isArchived }

    private val pinnedThenDueAscNullsLast: Comparator<Task> =
        compareByDescending<Task> { it.isPinned }
            .thenBy(nullsLast()) { it.dueAt }

    override suspend fun getWidgetTasks(limit: Int): List<Task> = tasks
        .activeOnly()
        .sortedWith(pinnedThenDueAscNullsLast)
        .take(limit)

    override suspend fun getWidgetTasksUpcoming(nowMillis: Long, untilMillis: Long, limit: Int): List<Task> {
        upcomingCalls.add(UpcomingCall(nowMillis, untilMillis, limit))
        return tasks
            .activeOnly()
            .filter { it.dueAt != null && it.dueAt >= nowMillis && it.dueAt < untilMillis }
            .sortedWith(compareByDescending<Task> { it.isPinned }.thenBy { it.dueAt })
            .take(limit)
    }

    override suspend fun getWidgetTasksPinned(limit: Int): List<Task> {
        pinnedLimits.add(limit)
        return tasks
            .activeOnly()
            .filter { it.isPinned }
            .sortedWith(compareBy(nullsLast()) { it.dueAt })
            .take(limit)
    }

    override suspend fun getWidgetTasksByTag(tag: String, limit: Int): List<Task> {
        tagCalls.add(tag to limit)
        return tasks
            .activeOnly()
            .filter { it.tag == tag }
            .sortedWith(pinnedThenDueAscNullsLast)
            .take(limit)
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
