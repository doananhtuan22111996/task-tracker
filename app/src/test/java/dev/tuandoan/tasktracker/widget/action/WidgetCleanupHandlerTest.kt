package dev.tuandoan.tasktracker.widget.action

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dev.tuandoan.tasktracker.data.preferences.WidgetConfigurationRepository
import dev.tuandoan.tasktracker.widget.model.WidgetSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * V13-10: pins the receiver's bridge contract — `onDeleted(ids)` results in
 * `repository.removeConfigs(ids)`. The repository's own behavior (multi-widget
 * isolation, empty-array short-circuit, tag-key cleanup) is JVM-tested in
 * V13-07's `WidgetConfigurationRepositoryTest`. This test pins that the
 * handler doesn't filter, reorder, or drop ids on the way through.
 *
 * Uses an in-memory `DataStore<Preferences>` so we can wire a real
 * `WidgetConfigurationRepository` without a temp file or Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetCleanupHandlerTest {

    @Test
    fun `cleanup removes configs for the same ids that onDeleted received`() = runTest(UnconfinedTestDispatcher()) {
        val store = InMemoryDataStore()
        val repo = WidgetConfigurationRepository(store)
        repo.setSource(1, WidgetSource.Today)
        repo.setSource(2, WidgetSource.Tag("WORK"))
        repo.setSource(3, WidgetSource.Pinned)

        WidgetCleanupHandler(repo).cleanup(intArrayOf(1, 2))

        assertNull(repo.getSourceOnce(1))
        assertNull(repo.getSourceOnce(2))
        assertEquals(WidgetSource.Pinned, repo.getSourceOnce(3))
    }

    @Test
    fun `cleanup with empty array delegates to repository short-circuit`() = runTest(UnconfinedTestDispatcher()) {
        val store = InMemoryDataStore()
        val repo = WidgetConfigurationRepository(store)
        repo.setSource(1, WidgetSource.Today)

        WidgetCleanupHandler(repo).cleanup(intArrayOf())

        assertEquals(WidgetSource.Today, repo.getSourceOnce(1))
    }

    // Note: the handler also swallows repository exceptions so a DataStore write
    // failure can't propagate as "broadcast handler crashed" to the system. That
    // path isn't JVM-testable because `Log.e` isn't mocked in the unit-test JVM
    // (same constraint that keeps WidgetCompleteHandler's catch branch
    // implicit). It's exercised on real-device traffic; the production code
    // path is the documented contract.

    /**
     * Test double: a minimal in-memory `DataStore<Preferences>` that supports
     * both `data` reads and `updateData` writes. Keeps the handler test
     * Robolectric-free without dragging in `PreferenceDataStoreFactory`.
     */
    private class InMemoryDataStore : DataStore<Preferences> {
        private var prefs: Preferences = emptyPreferences()

        override val data: Flow<Preferences> = flow { emit(prefs) }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            prefs = transform(prefs)
            return prefs
        }
    }
}
