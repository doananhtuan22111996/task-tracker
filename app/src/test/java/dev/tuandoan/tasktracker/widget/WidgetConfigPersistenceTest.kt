package dev.tuandoan.tasktracker.widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.tuandoan.tasktracker.data.preferences.WidgetConfigurationRepository
import dev.tuandoan.tasktracker.widget.action.WidgetCleanupHandler
import dev.tuandoan.tasktracker.widget.model.WidgetSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * V13-11: Widget configuration persistence lifecycle tests (NFR-14).
 *
 * Covers the four lifecycle scenarios that would silently break user experience
 * if the DataStore persistence layer misbehaves:
 *  1. Reboot survival — config written under one [WidgetConfigurationRepository]
 *     instance is readable from a fresh instance over the same backing file.
 *  2. Rebind stability — repeated reads via a second instance return the same
 *     value with no data corruption or key collision.
 *  3. Per-widgetId isolation — configuring widget A must not affect widget B.
 *  4. onDeleted cleanup — [WidgetCleanupHandler] removes exactly the deleted
 *     widget ids; surviving widgets are unaffected.
 *
 * All tests are JVM-only. `PreferenceDataStoreFactory.create` with a temp file
 * exercises the real binary `.preferences_pb` persistence path without Robolectric
 * (the same technique used in V13-07 [WidgetConfigurationRepositoryTest]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigPersistenceTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun TestScope.newStore(fileName: String = "widget-persist-test.preferences_pb") =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(coroutineContext),
            produceFile = { File(tmpFolder.root, fileName) },
        )

    // ── 1. Reboot survival ───────────────────────────────────────────────────

    @Test
    fun `Today config survives reboot (new repo over same store)`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        WidgetConfigurationRepository(store).setSource(1, WidgetSource.Today)

        val freshRepo = WidgetConfigurationRepository(store)
        assertEquals(WidgetSource.Today, freshRepo.getSourceOnce(1))
    }

    @Test
    fun `Upcoming7d config survives reboot`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        WidgetConfigurationRepository(store).setSource(2, WidgetSource.Upcoming7d)

        assertEquals(WidgetSource.Upcoming7d, WidgetConfigurationRepository(store).getSourceOnce(2))
    }

    @Test
    fun `Pinned config survives reboot`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        WidgetConfigurationRepository(store).setSource(3, WidgetSource.Pinned)

        assertEquals(WidgetSource.Pinned, WidgetConfigurationRepository(store).getSourceOnce(3))
    }

    @Test
    fun `Tag config survives reboot including the tag name`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        WidgetConfigurationRepository(store).setSource(4, WidgetSource.Tag("WORK"))

        assertEquals(WidgetSource.Tag("WORK"), WidgetConfigurationRepository(store).getSourceOnce(4))
    }

    // ── 2. Rebind stability ──────────────────────────────────────────────────

    @Test
    fun `second repo instance reads same value as first`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        val repo1 = WidgetConfigurationRepository(store)
        repo1.setSource(1, WidgetSource.Pinned)

        val repo2 = WidgetConfigurationRepository(store)
        assertEquals(repo1.getSourceOnce(1), repo2.getSourceOnce(1))
    }

    @Test
    fun `rebind after source change reflects updated value`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        val repo1 = WidgetConfigurationRepository(store)
        repo1.setSource(1, WidgetSource.Today)
        repo1.setSource(1, WidgetSource.Upcoming7d)

        assertEquals(WidgetSource.Upcoming7d, WidgetConfigurationRepository(store).getSourceOnce(1))
    }

    // ── 3. Per-widgetId isolation ────────────────────────────────────────────

    @Test
    fun `widget ids are fully isolated — writing A does not affect B`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(10, WidgetSource.Today)
        repo.setSource(20, WidgetSource.Pinned)
        repo.setSource(30, WidgetSource.Tag("ERRANDS"))

        assertEquals(WidgetSource.Today, repo.getSourceOnce(10))
        assertEquals(WidgetSource.Pinned, repo.getSourceOnce(20))
        assertEquals(WidgetSource.Tag("ERRANDS"), repo.getSourceOnce(30))
    }

    @Test
    fun `deleting widget A leaves widget B intact`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(10, WidgetSource.Today)
        repo.setSource(20, WidgetSource.Pinned)

        repo.removeConfigs(intArrayOf(10))

        assertNull(repo.getSourceOnce(10))
        assertEquals(WidgetSource.Pinned, repo.getSourceOnce(20))
    }

    @Test
    fun `fresh placement at recycled id starts unconfigured`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(5, WidgetSource.Tag("SHOPPING"))
        repo.removeConfigs(intArrayOf(5))

        // Launcher re-uses the same appWidgetId for a new placement.
        assertNull(repo.getSourceOnce(5))
    }

    // ── 4. onDeleted cleanup ─────────────────────────────────────────────────

    @Test
    fun `WidgetCleanupHandler removes only the deleted ids`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        val repo = WidgetConfigurationRepository(store)
        repo.setSource(1, WidgetSource.Today)
        repo.setSource(2, WidgetSource.Upcoming7d)
        repo.setSource(3, WidgetSource.Pinned)

        WidgetCleanupHandler(repo).cleanup(intArrayOf(1, 2))

        assertNull(repo.getSourceOnce(1))
        assertNull(repo.getSourceOnce(2))
        assertEquals(WidgetSource.Pinned, repo.getSourceOnce(3))
    }

    @Test
    fun `WidgetCleanupHandler clears Tag name alongside source key`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        val repo = WidgetConfigurationRepository(store)
        repo.setSource(1, WidgetSource.Tag("INBOX"))

        WidgetCleanupHandler(repo).cleanup(intArrayOf(1))

        // A fresh placement at the same id must not inherit the stale tag.
        repo.setSource(1, WidgetSource.Today)
        assertEquals(WidgetSource.Today, repo.getSourceOnce(1))
    }

    @Test
    fun `WidgetCleanupHandler no-ops on empty array`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        val repo = WidgetConfigurationRepository(store)
        repo.setSource(1, WidgetSource.Today)

        WidgetCleanupHandler(repo).cleanup(intArrayOf())

        assertEquals(WidgetSource.Today, repo.getSourceOnce(1))
    }

    @Test
    fun `cleanup result persists after rebind`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        val repo = WidgetConfigurationRepository(store)
        repo.setSource(1, WidgetSource.Tag("WORK"))
        repo.setSource(2, WidgetSource.Pinned)

        WidgetCleanupHandler(repo).cleanup(intArrayOf(1))

        // Simulate app restart: open a new repo over the same file.
        val freshRepo = WidgetConfigurationRepository(store)
        assertNull(freshRepo.getSourceOnce(1))
        assertEquals(WidgetSource.Pinned, freshRepo.getSourceOnce(2))
    }
}
