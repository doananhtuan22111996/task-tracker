package dev.tuandoan.tasktracker.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
import java.io.IOException

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
    fun `diagnosticsOptIn falls back to false when DataStore throws IOException`() =
        runTest(UnconfinedTestDispatcher()) {
            // If the `.preferences_pb` file is corrupt, the disk fills up, or the FS
            // permissions drift, DataStore's `data` Flow surfaces an IOException. The
            // repository's `.catch` contract must degrade to the opt-out default rather
            // than propagate — otherwise Application.onCreate crashes before any other
            // startup code runs, and the user loses access to the app over a diagnostics-
            // layer issue.
            val repo = PrivacyRepository(IoThrowingDataStore)
            repo.diagnosticsOptIn.test {
                assertFalse(awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `getDiagnosticsOptInOnce returns false when DataStore throws IOException`() =
        runTest(UnconfinedTestDispatcher()) {
            // Mirror check for the suspend one-shot path used by Settings and tests.
            val repo = PrivacyRepository(IoThrowingDataStore)
            assertFalse(repo.getDiagnosticsOptInOnce())
        }

    @Test
    fun `diagnosticsOptIn rethrows non-IO exceptions to surface real bugs`() = runTest(UnconfinedTestDispatcher()) {
        // A ClassCastException (e.g. future schema bug reading a boolean key as int)
        // or any non-IOException must propagate. Silently defaulting would mask
        // programmer errors indefinitely.
        val repo = PrivacyRepository(RuntimeThrowingDataStore)
        repo.diagnosticsOptIn.test {
            val error = awaitError()
            assertTrue(
                "Expected IllegalStateException to surface, got: $error",
                error is IllegalStateException,
            )
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

    /**
     * Test double: a `DataStore<Preferences>` whose `data` Flow throws [IOException]
     * on first collection. Exercises the read-failure recovery path.
     */
    private object IoThrowingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow {
            throw IOException("simulated corrupt privacy.preferences_pb")
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            emptyPreferences()
    }

    /**
     * Test double: throws a non-IO runtime exception on read, used to assert that
     * real programmer errors are not silently swallowed by the `.catch` contract.
     */
    private object RuntimeThrowingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow {
            throw IllegalStateException("simulated non-IO bug")
        }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            emptyPreferences()
    }
}
