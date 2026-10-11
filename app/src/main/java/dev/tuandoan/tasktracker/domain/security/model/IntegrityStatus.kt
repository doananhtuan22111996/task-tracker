package dev.tuandoan.tasktracker.domain.security.model

/**
 * Licensing verdict reported by Google Play for the user and device.
 */
enum class AppLicensingStatus {
    /** App binary was installed or purchased through Google Play by the current user. */
    LICENSED,

    /** App binary was sideloaded or acquired outside Google Play. */
    UNLICENSED,

    /** Licensing could not be evaluated (e.g. offline, Play services unreached). */
    UNEVALUATED,
}

/**
 * Device integrity status indicating hardware/OS trustworthiness.
 */
enum class DeviceIntegrityStatus {
    /** Certified Android device backed by hardware-attested key/TEE. */
    MEETS_STRONG_INTEGRITY,

    /** Certified Android device passing Google Play Protect and CTS checks. */
    MEETS_DEVICE_INTEGRITY,

    /** Device runs Android with basic system integrity (may be uncertified or emulator). */
    MEETS_BASIC_INTEGRITY,

    /** Device environment is untrusted, rooted, hooked, or integrity is unrecognized. */
    UNKNOWN_OR_UNTRUSTED,
}

/**
 * Comprehensive integrity evaluation result for the app, device, and runtime environment.
 */
data class IntegrityVerdict(
    val licensingStatus: AppLicensingStatus,
    val deviceStatus: DeviceIntegrityStatus,
    val isPlayRecognized: Boolean,
    val hasAccessRisk: Boolean,
    val recommendedDialogCode: Int? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
)

/**
 * Result wrapper representing the integrity verification lifecycle.
 */
sealed interface IntegrityCheckResult {
    /** Successfully verified with a complete integrity verdict. */
    data class Success(val verdict: IntegrityVerdict) : IntegrityCheckResult

    /** Offline fallback evaluation based on local package signatures and installer info. */
    data class OfflineFallback(val isSignatureValid: Boolean, val installerPackage: String?) : IntegrityCheckResult

    /** Verification encountered an unrecoverable exception or timeout. */
    data class Error(val message: String, val cause: Throwable? = null) : IntegrityCheckResult
}
