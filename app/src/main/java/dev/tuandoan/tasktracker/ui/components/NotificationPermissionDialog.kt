package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Permission rationale dialog for notification permission
 * Explains to users why the app needs notification permission for reminders
 */
@Composable
fun NotificationPermissionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notification permission",
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "Enable Notifications",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    text = "Task Tracker needs notification permission to send you reminders when your tasks are due.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "With notifications enabled, you can:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.padding(start = 16.dp)) {
                    BulletPoint(text = "Receive reminders 1 minute, 5 minutes, 1 hour, or 1 day before tasks are due")
                    BulletPoint(text = "Never miss important deadlines")
                    BulletPoint(text = "Stay organized and productive")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "You can always change this setting later in your device's notification settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Enable Notifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        },
        modifier = modifier,
    )
}

/**
 * Simple bullet point component for list items
 */
@Composable
private fun BulletPoint(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Permission denied dialog - shown when permission is permanently denied
 * Guides users to app settings to enable notifications manually
 */
@Composable
fun PermissionDeniedDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notification permission denied",
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = "Notifications Disabled",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    text = "To enable task reminders, please allow notifications in your device settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "How to enable notifications:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.padding(start = 16.dp)) {
                    BulletPoint(text = "Tap 'Open Settings' below")
                    BulletPoint(text = "Find 'Notifications' or 'App notifications'")
                    BulletPoint(text = "Toggle notifications ON for Task Tracker")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Without notifications, reminders will not work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        },
        modifier = modifier,
    )
}
