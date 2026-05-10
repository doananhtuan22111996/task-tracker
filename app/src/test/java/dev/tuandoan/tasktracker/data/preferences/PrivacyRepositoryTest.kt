package dev.tuandoan.tasktracker.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Contract test for [PrivacyRepository] (FB-04).
 *
 * Backs the DataStore with a temp file via [PreferenceDataStoreFactory.create], which
 * exercises the real persistence path (binary `.preferences_pb` encoding + atomic file
 * replace) without requiring Robolectric. The repository's internal constructor takes a
 * `DataStore<Preferences>` directly so JVM tests can inject this test-local store.
 *
 * DataStore's internal actor needs a coroutine scope; we hand it the same dispatcher that
 * `runTest` drives so file I/O progresses deterministically with the test body.
 *
 * The static [PrivacyRepository.readOptInOnce] path is not covered here — it requires an
 * Android `Context` to resolve the `privacyDataStore` delegate. FB-06's integration test
 * will exercise that path on a real `TaskTrackerApplication` instance.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun TestScope.newStore(fileName: String = "privacy-test.preferences_pb"): DataStore<Preferences> {
        val file = File(tmpFolder.root, fileName)
        // Key move: use the test scope (with the UnconfinedTestDispatcher passed to runTest)
        // for DataStore's internal actor. This keeps writes synchronous under the test.
        return PreferenceDataStoreFactory.create(
            scope = CoroutineScope(coroutineContext),
            produceFile = { file },
        )
    }

    @Test
    fun `default is false on fresh DataStore`() = runTest(UnconfinedTestDispatcher()) {
        // Fresh install: no key written yet. The PRD's structural opt-out guarantee
        // depends on this being false, not null. If a future DataStore default changed
        // to null-preserving semantics, this test catches the regression.
        val repo = PrivacyRepository(newStore())
        assertFalse(repo.getDiagnosticsOptInOnce())
    }

    @Test
    fun `setDiagnosticsOptIn persists true`() = runTest(UnconfinedTestDispatcher()) {
        val repo = PrivacyRepository(newStore())
        repo.setDiagnosticsOptIn(true)
        assertTrue(repo.getDiagnosticsOptInOnce())
    }

    @Test
    fun `setDiagnosticsOptIn can toggle back to false`() = runTest(UnconfinedTestDispatcher()) {
        val repo = PrivacyRepository(newStore())
        repo.setDiagnosticsOptIn(true)
        repo.setDiagnosticsOptIn(false)
        assertFalse(repo.getDiagnosticsOptInOnce())
    }

    @Test
    fun `diagnosticsOptIn Flow emits default immediately`() = runTest(UnconfinedTestDispatcher()) {
        val repo = PrivacyRepository(newStore())
        repo.diagnosticsOptIn.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `diagnosticsOptIn Flow emits on change`() = runTest(UnconfinedTestDispatcher()) {
        val repo = PrivacyRepository(newStore())
        repo.diagnosticsOptIn.test {
            assertFalse(awaitItem())

            repo.setDiagnosticsOptIn(true)
            assertTrue(awaitItem())

            repo.setDiagnosticsOptIn(false)
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `diagnosticsOptIn Flow does not re-emit on duplicate write`() = runTest(UnconfinedTestDispatcher()) {
        // distinctUntilChanged contract: downstream consumers like PrivacyManager shouldn't
        // refire Firebase setXxxCollectionEnabled calls on no-op writes.
        val repo = PrivacyRepository(newStore())
        repo.diagnosticsOptIn.test {
            assertFalse(awaitItem())

            repo.setDiagnosticsOptIn(false) // duplicate of the initial default
            repo.setDiagnosticsOptIn(false) // another duplicate
            expectNoEvents()

            // Prove the Flow is alive by forcing a real change.
            repo.setDiagnosticsOptIn(true)
            assertTrue(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `persistence survives new repository over same store`() = runTest(UnconfinedTestDispatcher()) {
        // Simulates app restart: the opt-in state must not rely on in-memory caching —
        // FB-06's runBlocking read depends on file-based persistence through a fresh
        // repository instance. Reusing the same underlying store is intentional here;
        // creating a second DataStore over the same file while the first is live would
        // throw MultipleSingletonException, which is exactly the guarantee FB-06 relies on.
        val store = newStore()
        val repo = PrivacyRepository(store)
        repo.setDiagnosticsOptIn(true)

        val freshRepo = PrivacyRepository(store)
        assertEquals(true, freshRepo.getDiagnosticsOptInOnce())
    }
}
