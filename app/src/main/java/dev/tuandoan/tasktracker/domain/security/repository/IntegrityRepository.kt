package dev.tuandoan.tasktracker.domain.security.repository

import dev.tuandoan.tasktracker.domain.security.model.IntegrityCheckResult

/**
 * Repository interface defining operations for app integrity verification
 * and Google Play remediation flows.
 */
interface IntegrityRepository {
    /**
     * Warmed up in background at app launch to minimize token request latency.
     * Non-blocking and safe to call on IO dispatchers.
     */
    suspend fun warmUp(): Result<Unit>

    /**
     * Requests an integrity token bound to [requestHash] and evaluates application integrity.
     * Falls back gracefully to local heuristics when offline.
     */
    suspend fun verifyIntegrity(requestHash: String): IntegrityCheckResult

    /**
     * Triggers Google Play's native remediation dialog (e.g. GET_LICENSED) using the active activity.
     * Returns Google Play's integer result code.
     */
    suspend fun launchRemediationDialog(activity: Any, dialogTypeCode: Int): Result<Int>
}
