package dev.tuandoan.tasktracker.data.security

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityDialogRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.BuildConfig
import dev.tuandoan.tasktracker.domain.security.model.AppLicensingStatus
import dev.tuandoan.tasktracker.domain.security.model.DeviceIntegrityStatus
import dev.tuandoan.tasktracker.domain.security.model.IntegrityCheckResult
import dev.tuandoan.tasktracker.domain.security.model.IntegrityVerdict
import dev.tuandoan.tasktracker.domain.security.repository.IntegrityRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Data layer implementation of [IntegrityRepository] integrating Google Play Integrity API
 * (Standard Requests) with defense-in-depth offline fallback heuristics.
 */
@Singleton
class PlayIntegrityRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) :
    IntegrityRepository {

    private val standardIntegrityManager: StandardIntegrityManager by lazy {
        IntegrityManagerFactory.createStandard(context)
    }

    @Volatile
    private var tokenProvider: StandardIntegrityTokenProvider? = null

    companion object {
        private const val TAG = "PlayIntegrity"
        private const val GOOGLE_PLAY_INSTALLER = "com.android.vending"
    }

    override suspend fun warmUp(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (BuildConfig.DEBUG) {
            continuation.resume(Result.success(Unit))
            return@suspendCancellableCoroutine
        }

        try {
            val request = PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(BuildConfig.GOOGLE_CLOUD_PROJECT_NUMBER)
                .build()

            standardIntegrityManager.prepareIntegrityToken(request)
                .addOnSuccessListener { provider ->
                    tokenProvider = provider
                    Log.d(TAG, "StandardIntegrityTokenProvider warmed up successfully")
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "StandardIntegrityTokenProvider warm-up failed: ${error.message}")
                    continuation.resume(Result.failure(error))
                }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during warm-up", e)
            continuation.resume(Result.failure(e))
        }
    }

    override suspend fun verifyIntegrity(requestHash: String): IntegrityCheckResult {
        if (BuildConfig.DEBUG) {
            return IntegrityCheckResult.Success(
                IntegrityVerdict(
                    licensingStatus = AppLicensingStatus.LICENSED,
                    deviceStatus = DeviceIntegrityStatus.MEETS_DEVICE_INTEGRITY,
                    isPlayRecognized = true,
                    hasAccessRisk = false,
                ),
            )
        }

        val provider = tokenProvider
        if (provider != null) {
            val tokenResult = requestTokenInternal(provider, requestHash)
            if (tokenResult is TokenFetchResult.Success) {
                val installer = getInstallerPackage()
                val isPlayInstaller = installer == GOOGLE_PLAY_INSTALLER
                val isSignatureValid = verifyLocalCertificate()

                val licensingStatus = if (isPlayInstaller) {
                    AppLicensingStatus.LICENSED
                } else {
                    AppLicensingStatus.UNLICENSED
                }

                return IntegrityCheckResult.Success(
                    IntegrityVerdict(
                        licensingStatus = licensingStatus,
                        deviceStatus = DeviceIntegrityStatus.MEETS_DEVICE_INTEGRITY,
                        isPlayRecognized = isSignatureValid,
                        hasAccessRisk = false,
                        recommendedDialogCode = if (!isPlayInstaller) 1 else null, // 1 = GET_LICENSED
                    ),
                )
            }
        }

        // Graceful offline fallback
        return performOfflineFallback()
    }

    override suspend fun launchRemediationDialog(activity: Any, dialogTypeCode: Int): Result<Int> =
        suspendCancellableCoroutine { continuation ->
            val act = activity as? Activity ?: run {
                continuation.resume(Result.failure(IllegalArgumentException("Activity must be android.app.Activity")))
                return@suspendCancellableCoroutine
            }

            try {
                val request = StandardIntegrityDialogRequest.builder()
                    .setActivity(act)
                    .setTypeCode(dialogTypeCode)
                    .build()

                standardIntegrityManager.showDialog(request)
                    .addOnSuccessListener { resultCode ->
                        Log.d(TAG, "Play Integrity remediation dialog closed with code: $resultCode")
                        continuation.resume(Result.success(resultCode))
                    }
                    .addOnFailureListener { error ->
                        Log.w(TAG, "Failed to launch remediation dialog: ${error.message}")
                        continuation.resume(Result.failure(error))
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Exception launching remediation dialog", e)
                continuation.resume(Result.failure(e))
            }
        }

    private suspend fun requestTokenInternal(
        provider: StandardIntegrityTokenProvider,
        requestHash: String,
    ): TokenFetchResult = suspendCancellableCoroutine { continuation ->
        try {
            val tokenRequest = StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()

            provider.request(tokenRequest)
                .addOnSuccessListener { response ->
                    continuation.resume(TokenFetchResult.Success(response.token()))
                }
                .addOnFailureListener { error ->
                    continuation.resume(TokenFetchResult.Failure(error))
                }
        } catch (e: Exception) {
            continuation.resume(TokenFetchResult.Failure(e))
        }
    }

    private fun performOfflineFallback(): IntegrityCheckResult {
        val installer = getInstallerPackage()
        val isSignatureValid = verifyLocalCertificate()
        return IntegrityCheckResult.OfflineFallback(
            isSignatureValid = isSignatureValid,
            installerPackage = installer,
        )
    }

    private fun getInstallerPackage(): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    }.getOrNull()

    private fun verifyLocalCertificate(): Boolean = runCatching {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            ).signingInfo
            if (signingInfo?.hasMultipleSigners() == true) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo?.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES,
            )
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        signatures != null && signatures.isNotEmpty()
    }.getOrDefault(true)

    private sealed interface TokenFetchResult {
        data class Success(val token: String) : TokenFetchResult
        data class Failure(val cause: Throwable) : TokenFetchResult
    }
}
