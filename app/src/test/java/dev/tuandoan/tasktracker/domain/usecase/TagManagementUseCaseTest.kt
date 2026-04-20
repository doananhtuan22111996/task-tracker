package dev.tuandoan.tasktracker.domain.usecase

import app.cash.turbine.test
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TagManagementUseCaseTest {

    private lateinit var repository: FakeTaskRepository
    private lateinit var useCase: TagManagementUseCase

    @Before
    fun setup() {
        repository = FakeTaskRepository()
        useCase = TagManagementUseCase(repository)
    }

    @Test
    fun `observeTags returns distinct tags with counts`() = runTest {
        repository.seed(
            Task(id = 1, title = "A", tag = "work", tagColor = "blue"),
            Task(id = 2, title = "B", tag = "work", tagColor = "blue"),
            Task(id = 3, title = "C", tag = "personal"),
        )

        useCase.observeTags().test {
            val tags = awaitItem()
            assertEquals(2, tags.size)
            val personal = tags.find { it.name == "personal" }!!
            val work = tags.find { it.name == "work" }!!
            assertEquals(1, personal.taskCount)
            assertEquals(2, work.taskCount)
            assertEquals("blue", work.color)
            assertNull(personal.color)
        }
    }

    @Test
    fun `observeTags excludes archived tasks`() = runTest {
        repository.seed(
            Task(id = 1, title = "A", tag = "work"),
            Task(id = 2, title = "B", tag = "work", isArchived = true),
        )

        useCase.observeTags().test {
            val tags = awaitItem()
            assertEquals(1, tags.size)
            assertEquals(1, tags[0].taskCount)
        }
    }

    @Test
    fun `renameTag updates all tasks with that tag`() = runTest {
        repository.seed(
            Task(id = 1, title = "A", tag = "old"),
            Task(id = 2, title = "B", tag = "old"),
            Task(id = 3, title = "C", tag = "other"),
        )

        val result = useCase.renameTag("old", "new")
        assertTrue(result.isSuccess)

        val tasks = repository.getAllTasksSnapshot()
        assertEquals("NEW", tasks.find { it.id == 1L }!!.tag)
        assertEquals("NEW", tasks.find { it.id == 2L }!!.tag)
        assertEquals("other", tasks.find { it.id == 3L }!!.tag)
    }

    @Test
    fun `renameTag trims whitespace and uppercases`() = runTest {
        repository.seed(Task(id = 1, title = "A", tag = "old"))

        val result = useCase.renameTag("old", "  new  ")
        assertTrue(result.isSuccess)
        assertEquals("NEW", repository.getAllTasksSnapshot()[0].tag)
    }

    @Test
    fun `renameTag fails for blank name`() = runTest {
        repository.seed(Task(id = 1, title = "A", tag = "old"))

        val result = useCase.renameTag("old", "   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun `renameTag fails for name exceeding max length`() = runTest {
        repository.seed(Task(id = 1, title = "A", tag = "old"))

        val longName = "a".repeat(31)
        val result = useCase.renameTag("old", longName)
        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteTag clears tag and color from all tasks`() = runTest {
        repository.seed(
            Task(id = 1, title = "A", tag = "work", tagColor = "blue"),
            Task(id = 2, title = "B", tag = "work", tagColor = "blue"),
            Task(id = 3, title = "C", tag = "personal"),
        )

        val result = useCase.deleteTag("work")
        assertTrue(result.isSuccess)

        val tasks = repository.getAllTasksSnapshot()
        assertNull(tasks.find { it.id == 1L }!!.tag)
        assertNull(tasks.find { it.id == 1L }!!.tagColor)
        assertNull(tasks.find { it.id == 2L }!!.tag)
        assertEquals("personal", tasks.find { it.id == 3L }!!.tag)
    }

    @Test
    fun `updateTagColor sets color on all tasks with that tag`() = runTest {
        repository.seed(
            Task(id = 1, title = "A", tag = "work"),
            Task(id = 2, title = "B", tag = "work"),
            Task(id = 3, title = "C", tag = "personal"),
        )

        val result = useCase.updateTagColor("work", "red")
        assertTrue(result.isSuccess)

        val tasks = repository.getAllTasksSnapshot()
        assertEquals("red", tasks.find { it.id == 1L }!!.tagColor)
        assertEquals("red", tasks.find { it.id == 2L }!!.tagColor)
        assertNull(tasks.find { it.id == 3L }!!.tagColor)
    }

    @Test
    fun `updateTagColor clears color when null`() = runTest {
        repository.seed(
            Task(id = 1, title = "A", tag = "work", tagColor = "blue"),
        )

        val result = useCase.updateTagColor("work", null)
        assertTrue(result.isSuccess)
        assertNull(repository.getAllTasksSnapshot()[0].tagColor)
    }

    @Test
    fun `getTagColor returns color from existing tasks`() = runTest {
        repository.seed(
            Task(id = 1, title = "A", tag = "work", tagColor = "blue"),
            Task(id = 2, title = "B", tag = "work"),
        )

        val color = useCase.getTagColor("work")
        assertEquals("blue", color)
    }

    @Test
    fun `getTagColor returns null when no tasks have color`() = runTest {
        repository.seed(Task(id = 1, title = "A", tag = "work"))

        val color = useCase.getTagColor("work")
        assertNull(color)
    }
}
