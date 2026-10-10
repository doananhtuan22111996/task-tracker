package dev.tuandoan.tasktracker.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.tuandoan.tasktracker.R

/**
 * Non-destructive warning dialog shown when an unverified or sideloaded APK is detected.
 * Informs the user of the security risks while respecting offline-first data integrity.
 */
@Composable
fun SideloadWarningDialog(
    onDismiss: () -> Unit,
    onGetOfficialApp: () -> Unit = {
        // Default implementation opens official Google Play Store page
    },
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(text = stringResource(R.string.security_sideload_dialog_title))
        },
        text = {
            Text(
                text = stringResource(R.string.security_sideload_dialog_message),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onGetOfficialApp()
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"),
                    )
                    runCatching { context.startActivity(intent) }
                    onDismiss()
                },
            ) {
                Text(text = stringResource(R.string.security_sideload_dialog_get_official))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.security_sideload_dialog_continue))
            }
        },
    )
}
