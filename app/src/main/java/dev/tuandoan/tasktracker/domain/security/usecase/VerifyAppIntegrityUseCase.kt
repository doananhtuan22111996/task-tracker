package dev.tuandoan.tasktracker.domain.security.usecase

import dev.tuandoan.tasktracker.domain.security.model.IntegrityCheckResult
import dev.tuandoan.tasktracker.domain.security.repository.IntegrityRepository
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

/**
 * Domain use case coordinating integrity verification.
 * Generates high-entropy request hashes to prevent token replay attacks
 * and orchestrates verification through [IntegrityRepository].
 */
class VerifyAppIntegrityUseCase @Inject constructor(private val integrityRepository: IntegrityRepository) {
    /**
     * Executes an integrity check.
     * Generates a unique SHA-256 request hash bound to the check action.
     */
    suspend operator fun invoke(): IntegrityCheckResult {
        val requestHash = generateRequestHash()
        return integrityRepository.verifyIntegrity(requestHash)
    }

    private fun generateRequestHash(): String {
        val rawNonce = UUID.randomUUID().toString() + ":" + System.currentTimeMillis()
        val digest = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
