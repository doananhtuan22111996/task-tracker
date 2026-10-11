package dev.tuandoan.tasktracker.domain.security

import dev.tuandoan.tasktracker.domain.security.model.AppLicensingStatus
import dev.tuandoan.tasktracker.domain.security.model.DeviceIntegrityStatus
import dev.tuandoan.tasktracker.domain.security.model.IntegrityCheckResult
import dev.tuandoan.tasktracker.domain.security.model.IntegrityVerdict
import dev.tuandoan.tasktracker.domain.security.usecase.VerifyAppIntegrityUseCase
import dev.tuandoan.tasktracker.testutil.FakeIntegrityRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VerifyAppIntegrityUseCaseTest {

    private lateinit var fakeRepository: FakeIntegrityRepository
    private lateinit var useCase: VerifyAppIntegrityUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeIntegrityRepository()
        useCase = VerifyAppIntegrityUseCase(fakeRepository)
    }

    @Test
    fun `invoke delegates to repository and returns success verdict`() = runTest {
        val expectedVerdict = IntegrityVerdict(
            licensingStatus = AppLicensingStatus.LICENSED,
            deviceStatus = DeviceIntegrityStatus.MEETS_STRONG_INTEGRITY,
            isPlayRecognized = true,
            hasAccessRisk = false,
        )
        fakeRepository.verifyResult = IntegrityCheckResult.Success(expectedVerdict)

        val result = useCase()

        assertTrue(result is IntegrityCheckResult.Success)
        val actualVerdict = (result as IntegrityCheckResult.Success).verdict
        assertEquals(AppLicensingStatus.LICENSED, actualVerdict.licensingStatus)
        assertEquals(DeviceIntegrityStatus.MEETS_STRONG_INTEGRITY, actualVerdict.deviceStatus)
        assertTrue(actualVerdict.isPlayRecognized)
        assertEquals(1, fakeRepository.verifyCallCount)
        assertNotNull(fakeRepository.lastRequestHash)
        assertEquals(64, fakeRepository.lastRequestHash?.length) // SHA-256 hex length
    }

    @Test
    fun `invoke returns offline fallback when repository returns offline result`() = runTest {
        fakeRepository.verifyResult = IntegrityCheckResult.OfflineFallback(
            isSignatureValid = true,
            installerPackage = "com.android.vending",
        )

        val result = useCase()

        assertTrue(result is IntegrityCheckResult.OfflineFallback)
        val fallback = result as IntegrityCheckResult.OfflineFallback
        assertTrue(fallback.isSignatureValid)
        assertEquals("com.android.vending", fallback.installerPackage)
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest {
        fakeRepository.verifyResult = IntegrityCheckResult.Error("Play Services unavailable")

        val result = useCase()

        assertTrue(result is IntegrityCheckResult.Error)
        assertEquals("Play Services unavailable", (result as IntegrityCheckResult.Error).message)
    }

    @Test
    fun `repeated invocations generate distinct request hashes to prevent replay`() = runTest {
        useCase()
        val hash1 = fakeRepository.lastRequestHash

        useCase()
        val hash2 = fakeRepository.lastRequestHash

        assertNotNull(hash1)
        assertNotNull(hash2)
        assertNotEquals(hash1, hash2)
    }
}
