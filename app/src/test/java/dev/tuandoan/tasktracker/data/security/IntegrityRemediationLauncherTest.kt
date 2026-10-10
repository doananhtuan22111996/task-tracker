package dev.tuandoan.tasktracker.data.security

import android.app.Activity
import android.util.Log
import dev.tuandoan.tasktracker.testutil.FakeIntegrityRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IntegrityRemediationLauncherTest {

    private lateinit var fakeRepository: FakeIntegrityRepository
    private lateinit var launcher: IntegrityRemediationLauncher
    private val mockActivity: Activity = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        fakeRepository = FakeIntegrityRepository()
        launcher = IntegrityRemediationLauncher(fakeRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `launchRemediation does not trigger fallback when dialog returns success zero`() = runTest {
        fakeRepository.remediationDialogResult = Result.success(0)
        var fallbackTriggered = false

        launcher.launchRemediation(mockActivity, IntegrityRemediationLauncher.DIALOG_TYPE_GET_LICENSED) {
            fallbackTriggered = true
        }

        assertFalse(fallbackTriggered)
        assertEquals(1, fakeRepository.remediationCallCount)
        assertEquals(IntegrityRemediationLauncher.DIALOG_TYPE_GET_LICENSED, fakeRepository.lastRemediationDialogCode)
    }

    @Test
    fun `launchRemediation triggers fallback when dialog returns non-zero result`() = runTest {
        fakeRepository.remediationDialogResult = Result.success(1) // User cancelled / unresolved
        var fallbackTriggered = false

        launcher.launchRemediation(mockActivity, IntegrityRemediationLauncher.DIALOG_TYPE_GET_LICENSED) {
            fallbackTriggered = true
        }

        assertTrue(fallbackTriggered)
        assertEquals(1, fakeRepository.remediationCallCount)
    }

    @Test
    fun `launchRemediation triggers fallback when repository returns failure`() = runTest {
        fakeRepository.remediationDialogResult = Result.failure(RuntimeException("Play Store unavailable"))
        var fallbackTriggered = false

        launcher.launchRemediation(mockActivity, IntegrityRemediationLauncher.DIALOG_TYPE_GET_LICENSED) {
            fallbackTriggered = true
        }

        assertTrue(fallbackTriggered)
        assertEquals(1, fakeRepository.remediationCallCount)
    }
}
