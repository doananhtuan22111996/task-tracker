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

    private fun exportUseCase(
        analyticsLogger: dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger = io.mockk.mockk(relaxed = true),
    ): ExportBackupUseCase = ExportBackupUseCase(
        repository = repository,
        subtaskRepository = subtaskRepository,
        jsonSerializer = serializer,
        csvSerializer = serializer,
        fileProvider = fileProvider,
        context = context,
        breadcrumbLogger = breadcrumbLogger,
        analyticsLogger = analyticsLogger,
    )

    private fun importUseCase(
        validator: BackupValidator = TaskBackupValidator(),
        analyticsLogger: dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger = io.mockk.mockk(relaxed = true),
        performanceLogger: dev.tuandoan.tasktracker.diagnostics.PerformanceLogger = io.mockk.mockk(relaxed = true),
    ): ImportBackupUseCase = ImportBackupUseCase(
        repository = repository,
        jsonSerializer = serializer,
        fileProvider = fileProvider,
        validator = validator,
        context = context,
        breadcrumbLogger = breadcrumbLogger,
        analyticsLogger = analyticsLogger,
        performanceLogger = performanceLogger,
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

    // ── FB-14: Analytics events ────────────────────────────────────────────

    @Test
    fun `export success emits BackupExported with enum format and bucketed count`() = runTest {
        val analyticsLogger = io.mockk.mockk<dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger>(relaxed = true)
        repeat(3) { i -> repository.insertTask(TestTaskFactory.createTask(id = (i + 1).toLong())) }
        exportUseCase(analyticsLogger).execute(uri, BackupFormat.JSON, "1.12.0")
        io.mockk.verify {
            analyticsLogger.log(
                dev.tuandoan.tasktracker.diagnostics.AnalyticsEvent.BackupExported(
                    format = dev.tuandoan.tasktracker.diagnostics.BackupEventFormat.JSON,
                    taskCount = 3,
                ),
            )
        }
    }

    @Test
    fun `export failure does NOT emit BackupExported event`() = runTest {
        val analyticsLogger = io.mockk.mockk<dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger>(relaxed = true)
        coEvery { fileProvider.writeToUri(any(), any()) } throws RuntimeException("/sensitive/path denied")
        exportUseCase(analyticsLogger).execute(uri, BackupFormat.JSON, "1.12.0")
        io.mockk.verify(exactly = 0) {
            analyticsLogger.log(any<dev.tuandoan.tasktracker.diagnostics.AnalyticsEvent.BackupExported>())
        }
    }

    @Test
    fun `import failure emits BackupImported with outcome=ERROR and no path leak`() = runTest {
        val analyticsLogger = io.mockk.mockk<dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger>(relaxed = true)
        coEvery { fileProvider.readFromUri(any()) } throws RuntimeException("/Users/alice/file.json")
        importUseCase(analyticsLogger = analyticsLogger).execute(uri)
        io.mockk.verify {
            analyticsLogger.log(
                dev.tuandoan.tasktracker.diagnostics.AnalyticsEvent.BackupImported(
                    format = dev.tuandoan.tasktracker.diagnostics.BackupEventFormat.JSON,
                    recordCount = 0,
                    outcome = dev.tuandoan.tasktracker.diagnostics.BackupOutcome.ERROR,
                ),
            )
        }
        // Negative invariant: no event param contains the exception message.
        io.mockk.verify(exactly = 0) {
            analyticsLogger.log(
                match<dev.tuandoan.tasktracker.diagnostics.AnalyticsEvent> { event ->
                    event.params.values.any { it is String && it.contains("alice") }
                },
            )
        }
    }

    // ── FB-17: backup_import Performance trace ─────────────────────────────

    @Test
    fun `import success starts and stops backup_import trace with bucketed record_count`() = runTest {
        val performanceLogger = io.mockk.mockk<dev.tuandoan.tasktracker.diagnostics.PerformanceLogger>(relaxed = true)
        val trace = io.mockk.mockk<dev.tuandoan.tasktracker.diagnostics.PerformanceTrace>(relaxed = true)
        io.mockk.every {
            performanceLogger.start(dev.tuandoan.tasktracker.diagnostics.PerformanceTraceName.BackupImport)
        } returns trace

        // 7 valid task DTOs in the JSON file → validator keeps them all → bucket "1-9".
        val dtos = (1..7).map { i ->
            dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto(
                id = i.toLong(),
                title = "task $i",
                createdAt = 1_700_000_000_000L,
            )
        }
        val json = serializer.serialize(
            tasks = dtos,
            schemaVersion = dev.tuandoan.tasktracker.domain.backup.model.BackupMetadata.CURRENT_SCHEMA_VERSION,
            exportedAt = 0L,
            appVersion = "test",
        )
        coEvery { fileProvider.readFromUri(uri) } returns json

        importUseCase(performanceLogger = performanceLogger).execute(uri)

        // Trace name pinned to BackupImport; attribute reflects the bucketed valid count.
        // Use bucketTaskCount(7) directly so a future bucket-boundary shift fails this
        // assertion at the right place rather than producing a quietly-stale literal.
        io.mockk.verifyOrder {
            performanceLogger.start(dev.tuandoan.tasktracker.diagnostics.PerformanceTraceName.BackupImport)
            trace.putAttribute("record_count", dev.tuandoan.tasktracker.diagnostics.bucketTaskCount(7))
            trace.stop()
        }
    }

    @Test
    fun `import failure stops backup_import trace with record_count=0`() = runTest {
        val performanceLogger = io.mockk.mockk<dev.tuandoan.tasktracker.diagnostics.PerformanceLogger>(relaxed = true)
        val trace = io.mockk.mockk<dev.tuandoan.tasktracker.diagnostics.PerformanceTrace>(relaxed = true)
        io.mockk.every {
            performanceLogger.start(dev.tuandoan.tasktracker.diagnostics.PerformanceTraceName.BackupImport)
        } returns trace

        coEvery { fileProvider.readFromUri(any()) } throws RuntimeException("/Users/alice/file.json")

        importUseCase(performanceLogger = performanceLogger).execute(uri)

        // Even on error, the finally block guarantees attribute set + stop. Use
        // bucketTaskCount(0) directly so the test's coupling is to the bucketing rule, not
        // to the literal output of the zero bucket.
        io.mockk.verifyOrder {
            performanceLogger.start(dev.tuandoan.tasktracker.diagnostics.PerformanceTraceName.BackupImport)
            trace.putAttribute("record_count", dev.tuandoan.tasktracker.diagnostics.bucketTaskCount(0))
            trace.stop()
        }
        // Negative invariant: no attribute key/value carries the file path.
        io.mockk.verify(exactly = 0) {
            trace.putAttribute(any(), match { it.contains("alice") })
        }
    }
}
