package dev.tuandoan.tasktracker.domain.backup

import android.content.Context
import android.net.Uri
import dev.tuandoan.tasktracker.data.backup.BackupFileProvider
import dev.tuandoan.tasktracker.data.backup.JsonBackupSerializer
import dev.tuandoan.tasktracker.data.backup.dto.SubtaskBackupDto
import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import dev.tuandoan.tasktracker.data.database.Subtask
import dev.tuandoan.tasktracker.domain.backup.model.BackupMetadata
import dev.tuandoan.tasktracker.domain.backup.model.ImportResult
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.fakeAnalyticsLogger
import dev.tuandoan.tasktracker.testutil.fakeBreadcrumbLogger
import dev.tuandoan.tasktracker.testutil.fakePerformanceLogger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end coverage for the subtask path through [ImportBackupUseCase]: JSON serializer
 * → DTO → validator → [FakeTaskRepository.subtaskSink]. Pins the invariant that subtasks
 * survive import and that blank-parent tasks drop their subtasks.
 */
class ImportBackupSubtaskTest {

    private lateinit var repository: FakeTaskRepository
    private lateinit var serializer: JsonBackupSerializer
    private lateinit var validator: TaskBackupValidator
    private lateinit var fileProvider: BackupFileProvider
    private lateinit var context: Context
    private lateinit var uri: Uri
    private lateinit var useCase: ImportBackupUseCase

    private var captured: List<Subtask> = emptyList()

    @Before
    fun setup() {
        repository = FakeTaskRepository().apply {
            subtaskSink = { captured = it }
        }
        serializer = JsonBackupSerializer()
        validator = TaskBackupValidator()
        fileProvider = mockk()
        context = mockk(relaxed = true) {
            every { getString(any(), *anyVararg()) } returns "error"
        }
        uri = mockk()
        useCase = ImportBackupUseCase(
            repository,
            serializer,
            fileProvider,
            validator,
            context,
            fakeBreadcrumbLogger(),
            fakeAnalyticsLogger(),
            fakePerformanceLogger(),
        )
    }

    @Test
    fun `import of v3 backup persists subtasks with parent task id`() = runTest {
        val dto = TaskBackupDto(
            id = 42L,
            title = "Plan trip",
            createdAt = 1_700_000_000_000L,
            subtasks = listOf(
                SubtaskBackupDto(id = 1L, title = "Book flight", isCompleted = true, sortOrder = 0),
                SubtaskBackupDto(id = 2L, title = "Reserve hotel", isCompleted = false, sortOrder = 1),
            ),
        )
        val json = serializer.serialize(listOf(dto), BackupMetadata.CURRENT_SCHEMA_VERSION, 0L, "test")
        coEvery { fileProvider.readFromUri(uri) } returns json

        val result = useCase.execute(uri)

        assertTrue(result is ImportResult.Success)
        assertEquals(2, captured.size)
        assertEquals(listOf("Book flight", "Reserve hotel"), captured.map { it.title })
        assertTrue(captured.all { it.taskId == 42L })
        // isCompleted on subtasks is preserved by import (reset is only applied by recurrence).
        assertEquals(listOf(true, false), captured.map { it.isCompleted })
        assertEquals(listOf(0, 1), captured.map { it.sortOrder })
    }

    @Test
    fun `import drops subtasks whose parent task has a blank title`() = runTest {
        val validDto = TaskBackupDto(
            id = 1L,
            title = "Kept",
            createdAt = 1_700_000_000_000L,
            subtasks = listOf(SubtaskBackupDto(id = 10L, title = "Kept sub", sortOrder = 0)),
        )
        val orphanedDto = TaskBackupDto(
            id = 2L,
            title = "   ", // blank — validator skips the whole task
            createdAt = 1_700_000_000_000L,
            subtasks = listOf(SubtaskBackupDto(id = 20L, title = "Orphan sub")),
        )
        val json = serializer.serialize(
            listOf(validDto, orphanedDto),
            BackupMetadata.CURRENT_SCHEMA_VERSION,
            0L,
            "test",
        )
        coEvery { fileProvider.readFromUri(uri) } returns json

        val result = useCase.execute(uri)

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.importedCount)
        assertEquals(1, success.skippedCount)
        // Only the subtask under the surviving parent should have been written.
        assertEquals(1, captured.size)
        assertEquals("Kept sub", captured.first().title)
        assertEquals(1L, captured.first().taskId)
    }

    @Test
    fun `import trims oversized subtask titles and drops blanks`() = runTest {
        val longTitle = "x".repeat(600) // over MAX_TITLE_LENGTH (500)
        val dto = TaskBackupDto(
            id = 1L,
            title = "Parent",
            createdAt = 1_700_000_000_000L,
            subtasks = listOf(
                SubtaskBackupDto(id = 10L, title = longTitle, sortOrder = 0),
                SubtaskBackupDto(id = 11L, title = "   ", sortOrder = 1), // blank — dropped
                SubtaskBackupDto(id = 12L, title = "kept", sortOrder = 2),
            ),
        )
        val json = serializer.serialize(listOf(dto), BackupMetadata.CURRENT_SCHEMA_VERSION, 0L, "test")
        coEvery { fileProvider.readFromUri(uri) } returns json

        useCase.execute(uri)

        assertEquals(2, captured.size)
        assertEquals(500, captured[0].title.length)
        assertEquals("kept", captured[1].title)
    }
}
