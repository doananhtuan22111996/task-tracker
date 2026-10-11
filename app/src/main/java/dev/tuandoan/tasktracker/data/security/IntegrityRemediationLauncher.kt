package dev.tuandoan.tasktracker.data.security

import android.app.Activity
import android.util.Log
import dev.tuandoan.tasktracker.domain.security.repository.IntegrityRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinator for launching Google Play remediation dialogs with graceful fallback
 * to in-app guidance if Google Play Services cannot present the native dialog.
 */
@Singleton
class IntegrityRemediationLauncher @Inject constructor(private val integrityRepository: IntegrityRepository) {
    companion object {
        private const val TAG = "IntegrityRemediation"
        const val DIALOG_TYPE_GET_LICENSED = 1
        const val DIALOG_TYPE_CLOSE_UNKNOWN_ACCESS_RISK = 2
        const val DIALOG_TYPE_CLOSE_ALL_ACCESS_RISK = 3
    }

    /**
     * Attempts to trigger Google Play's native remediation dialog (e.g. GET_LICENSED).
     * If the native dialog fails or Google Play Services is unavailable,
     * triggers [onShowFallbackDialog] to present in-app user guidance.
     */
    suspend fun launchRemediation(
        activity: Activity,
        dialogTypeCode: Int = DIALOG_TYPE_GET_LICENSED,
        onShowFallbackDialog: () -> Unit,
    ) {
        val result = integrityRepository.launchRemediationDialog(activity, dialogTypeCode)
        result.onSuccess { resultCode ->
            Log.d(TAG, "Native remediation completed with resultCode=$resultCode")
            // If the user cancelled or the native dialog could not resolve the license, trigger fallback guidance
            if (resultCode != 0) {
                onShowFallbackDialog()
            }
        }.onFailure { error ->
            Log.w(TAG, "Native remediation dialog failed: ${error.message}, falling back to in-app dialog")
            onShowFallbackDialog()
        }
    }
}
