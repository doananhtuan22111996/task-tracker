package dev.tuandoan.tasktracker.testutil

import dev.tuandoan.tasktracker.domain.security.model.AppLicensingStatus
import dev.tuandoan.tasktracker.domain.security.model.DeviceIntegrityStatus
import dev.tuandoan.tasktracker.domain.security.model.IntegrityCheckResult
import dev.tuandoan.tasktracker.domain.security.model.IntegrityVerdict
import dev.tuandoan.tasktracker.domain.security.repository.IntegrityRepository

/**
 * In-memory test fake implementing [IntegrityRepository] for JVM unit tests.
 */
class FakeIntegrityRepository : IntegrityRepository {

    var warmUpResult: Result<Unit> = Result.success(Unit)
    var warmUpCallCount: Int = 0
        private set

    var verifyResult: IntegrityCheckResult = IntegrityCheckResult.Success(
        IntegrityVerdict(
            licensingStatus = AppLicensingStatus.LICENSED,
            deviceStatus = DeviceIntegrityStatus.MEETS_DEVICE_INTEGRITY,
            isPlayRecognized = true,
            hasAccessRisk = false,
        ),
    )
    var lastRequestHash: String? = null
        private set
    var verifyCallCount: Int = 0
        private set

    var remediationDialogResult: Result<Int> = Result.success(0)
    var lastRemediationDialogCode: Int? = null
        private set
    var remediationCallCount: Int = 0
        private set

    override suspend fun warmUp(): Result<Unit> {
        warmUpCallCount++
        return warmUpResult
    }

    override suspend fun verifyIntegrity(requestHash: String): IntegrityCheckResult {
        verifyCallCount++
        lastRequestHash = requestHash
        return verifyResult
    }

    override suspend fun launchRemediationDialog(activity: Any, dialogTypeCode: Int): Result<Int> {
        remediationCallCount++
        lastRemediationDialogCode = dialogTypeCode
        return remediationDialogResult
    }

    fun reset() {
        warmUpCallCount = 0
        verifyCallCount = 0
        remediationCallCount = 0
        lastRequestHash = null
        lastRemediationDialogCode = null
    }
}
