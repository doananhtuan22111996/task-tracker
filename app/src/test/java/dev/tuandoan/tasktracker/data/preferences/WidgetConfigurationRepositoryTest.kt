package dev.tuandoan.tasktracker.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import dev.tuandoan.tasktracker.widget.model.WidgetSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * Contract test for [WidgetConfigurationRepository] (V13-07).
 *
 * Same shape as `PrivacyRepositoryTest`: temp-file `PreferenceDataStoreFactory.create`
 * exercises the real persistence path (binary `.preferences_pb` + atomic file replace)
 * without Robolectric. The internal constructor takes a `DataStore<Preferences>` so
 * the test scope's dispatcher drives DataStore's actor deterministically.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigurationRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun TestScope.newStore(fileName: String = "widget-test.preferences_pb"): DataStore<Preferences> {
        val file = File(tmpFolder.root, fileName)
        return PreferenceDataStoreFactory.create(
            scope = CoroutineScope(coroutineContext),
            produceFile = { file },
        )
    }

    // ── Reads / defaults ────────────────────────────────────────────────────

    @Test
    fun `unconfigured widget id reads as null`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        assertNull(repo.getSourceOnce(appWidgetId = 1))
    }

    @Test
    fun `Flow emits null immediately for unconfigured id`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.observeSource(1).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Round-trips per WidgetSource variant ────────────────────────────────

    @Test
    fun `Today round-trips`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(1, WidgetSource.Today)
        assertEquals(WidgetSource.Today, repo.getSourceOnce(1))
    }

    @Test
    fun `Upcoming7d round-trips`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(1, WidgetSource.Upcoming7d)
        assertEquals(WidgetSource.Upcoming7d, repo.getSourceOnce(1))
    }

    @Test
    fun `Pinned round-trips`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(1, WidgetSource.Pinned)
        assertEquals(WidgetSource.Pinned, repo.getSourceOnce(1))
    }

    @Test
    fun `Tag round-trips with the same name`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(1, WidgetSource.Tag("WORK"))
        assertEquals(WidgetSource.Tag("WORK"), repo.getSourceOnce(1))
    }

    // ── Defensive contracts ─────────────────────────────────────────────────

    @Test
    fun `setSource with blank Tag is rejected silently`() = runTest(UnconfinedTestDispatcher()) {
        // Configure activity (V13-08) is responsible for normalization, but the
        // store guards against a blank Tag making it through anyway.
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(1, WidgetSource.Tag(""))
        assertNull(repo.getSourceOnce(1))
    }

    @Test
    fun `switching from Tag to Today clears the stale tag value`() = runTest(UnconfinedTestDispatcher()) {
        // Otherwise a leftover `widget_{id}_tag` could be misread by future code.
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(1, WidgetSource.Tag("ERRANDS"))
        repo.setSource(1, WidgetSource.Today)
        assertEquals(WidgetSource.Today, repo.getSourceOnce(1))
    }

    @Test
    fun `unknown stored source string reads as null for forward-compat`() = runTest(UnconfinedTestDispatcher()) {
        // v1.13.1 might add a "completed_today" variant; v1.13.0 must not crash on it.
        val sourceKey = stringPreferencesKey("widget_1_source")
        val store = StaticDataStore(preferencesOf(sourceKey to "completed_today"))
        val repo = WidgetConfigurationRepository(store)
        assertNull(repo.getSourceOnce(1))
    }

    // ── Multi-widget isolation ──────────────────────────────────────────────

    @Test
    fun `configs for different widget ids are isolated`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(1, WidgetSource.Today)
        repo.setSource(2, WidgetSource.Pinned)
        repo.setSource(3, WidgetSource.Tag("WORK"))

        assertEquals(WidgetSource.Today, repo.getSourceOnce(1))
        assertEquals(WidgetSource.Pinned, repo.getSourceOnce(2))
        assertEquals(WidgetSource.Tag("WORK"), repo.getSourceOnce(3))
    }

    // ── removeConfigs (V13-10 onDeleted hook contract) ──────────────────────

    @Test
    fun `removeConfigs deletes only the requested ids`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(1, WidgetSource.Today)
        repo.setSource(2, WidgetSource.Tag("WORK"))
        repo.setSource(3, WidgetSource.Pinned)

        repo.removeConfigs(intArrayOf(1, 2))

        assertNull(repo.getSourceOnce(1))
        assertNull(repo.getSourceOnce(2))
        assertEquals(WidgetSource.Pinned, repo.getSourceOnce(3))
    }

    @Test
    fun `removeConfigs with empty array is a no-op`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(1, WidgetSource.Today)
        repo.removeConfigs(intArrayOf())
        assertEquals(WidgetSource.Today, repo.getSourceOnce(1))
    }

    @Test
    fun `removeConfigs clears the tag value as well as the source`() = runTest(UnconfinedTestDispatcher()) {
        // Otherwise a fresh widget placed at the same id could inherit the old tag.
        val repo = WidgetConfigurationRepository(newStore())
        repo.setSource(1, WidgetSource.Tag("ERRANDS"))
        repo.removeConfigs(intArrayOf(1))
        repo.setSource(1, WidgetSource.Today)
        assertEquals(WidgetSource.Today, repo.getSourceOnce(1))
    }

    // ── Reactivity / dedupe ─────────────────────────────────────────────────

    @Test
    fun `Flow emits on change`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.observeSource(1).test {
            assertNull(awaitItem())

            repo.setSource(1, WidgetSource.Today)
            assertEquals(WidgetSource.Today, awaitItem())

            repo.setSource(1, WidgetSource.Pinned)
            assertEquals(WidgetSource.Pinned, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Flow does not re-emit on duplicate source write`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(newStore())
        repo.observeSource(1).test {
            assertNull(awaitItem())

            repo.setSource(1, WidgetSource.Today)
            assertEquals(WidgetSource.Today, awaitItem())

            repo.setSource(1, WidgetSource.Today)
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── IOException recovery ────────────────────────────────────────────────

    @Test
    fun `Flow falls back to null when DataStore throws IOException`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(IoThrowingDataStore)
        repo.observeSource(1).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Flow rethrows non-IO exceptions to surface real bugs`() = runTest(UnconfinedTestDispatcher()) {
        val repo = WidgetConfigurationRepository(RuntimeThrowingDataStore)
        repo.observeSource(1).test {
            val error = awaitError()
            assertTrue(
                "Expected IllegalStateException to surface, got: $error",
                error is IllegalStateException,
            )
        }
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    @Test
    fun `persistence survives new repository over same store`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        val repo = WidgetConfigurationRepository(store)
        repo.setSource(1, WidgetSource.Tag("WORK"))

        val freshRepo = WidgetConfigurationRepository(store)
        assertEquals(WidgetSource.Tag("WORK"), freshRepo.getSourceOnce(1))
    }

    // ── Test doubles ────────────────────────────────────────────────────────

    /** Mirrors PrivacyRepositoryTest's IoThrowingDataStore. */
    private object IoThrowingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow {
            throw IOException("simulated corrupt widget.preferences_pb")
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            emptyPreferences()
    }

    private object RuntimeThrowingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow {
            throw IllegalStateException("simulated non-IO bug")
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            emptyPreferences()
    }

    /** Static fixture for testing forward-compat unknown source strings. */
    private class StaticDataStore(private val prefs: Preferences) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { emit(prefs) }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = transform(prefs)
    }
}
