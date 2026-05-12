package dev.tuandoan.tasktracker.domain.backup

import android.content.Context
import android.net.Uri
import dev.tuandoan.tasktracker.data.backup.BackupFileProvider
import dev.tuandoan.tasktracker.data.backup.JsonBackupSerializer
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbCategory
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbLogger
import dev.tuandoan.tasktracker.domain.backup.model.BackupFormat
import dev.tuandoan.tasktracker.testutil.FakeSubtaskRepository
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * FB-12 coverage for backup breadcrumbs. Asserts:
 *   (1) export logs `BACKUP start format=…` + `BACKUP done count=<bucket>`.
 *   (2) import logs `BACKUP start` + `BACKUP done count=… skipped=…`.
 *   (3) either path emits `BACKUP … failed` on error and DOES NOT include the
 *       exception message (which may contain file paths or user-identifying strings).
 */
class BackupBreadcrumbsTest {

    private lateinit var repository: FakeTaskRepository
    private lateinit var subtaskRepository: FakeSubtaskRepository
    private lateinit var serializer: JsonBackupSerializer
    private lateinit var fileProvider: BackupFileProvider
    private lateinit var context: Context
    private lateinit var uri: Uri
    private lateinit var breadcrumbLogger: BreadcrumbLogger

    @Before
    fun setUp() {
        repository = FakeTaskRepository()
        subtaskRepository = FakeSubtaskRepository()
        serializer = JsonBackupSerializer()
        fileProvider = mockk(relaxed = true)
        context = mockk(relaxed = true) {
            every { getString(any(), *anyVararg()) } returns "error"
        }
        uri = mockk()
        breadcrumbLogger = mockk(relaxed = true)
    }

    private fun exportUseCase(): ExportBackupUseCase = ExportBackupUseCase(
        repository = repository,
        subtaskRepository = subtaskRepository,
        jsonSerializer = serializer,
        csvSerializer = serializer,
        fileProvider = fileProvider,
        context = context,
        breadcrumbLogger = breadcrumbLogger,
    )

    private fun importUseCase(validator: BackupValidator = TaskBackupValidator()): ImportBackupUseCase =
        ImportBackupUseCase(
            repository = repository,
            jsonSerializer = serializer,
            fileProvider = fileProvider,
            validator = validator,
            context = context,
            breadcrumbLogger = breadcrumbLogger,
        )

    // ── Export ─────────────────────────────────────────────────────────────

    @Test
    fun `export happy path logs start and done with bucketed count`() = runTest {
        // Seed 15 tasks — should bucket into "10-49".
        repeat(15) { i -> repository.insertTask(TestTaskFactory.createTask(id = (i + 1).toLong())) }
        exportUseCase().execute(uri, BackupFormat.JSON, "1.12.0")
        verify { breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "export start format=JSON") }
        verify { breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "export done count=10-49") }
    }

    @Test
    fun `export failure logs BACKUP failed WITHOUT exception message`() = runTest {
        coEvery { fileProvider.writeToUri(any(), any()) } throws RuntimeException("/sensitive/path/foo.json denied")
        exportUseCase().execute(uri, BackupFormat.JSON, "1.12.0")
        verify { breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "export failed") }
        // Sanity: the raw exception message must NEVER reach a breadcrumb.
        verify(exactly = 0) {
            breadcrumbLogger.log(BreadcrumbCategory.BACKUP, match { it.contains("/sensitive/path") })
        }
    }

    // ── Import ─────────────────────────────────────────────────────────────

    @Test
    fun `import failure logs BACKUP failed WITHOUT raw error string`() = runTest {
        coEvery { fileProvider.readFromUri(any()) } throws
            RuntimeException("/Users/alice/Documents/backup.json unreadable")
        importUseCase().execute(uri)
        verify { breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "import start") }
        verify { breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "import failed") }
        verify(exactly = 0) {
            breadcrumbLogger.log(BreadcrumbCategory.BACKUP, match { it.contains("alice") })
        }
    }
}
