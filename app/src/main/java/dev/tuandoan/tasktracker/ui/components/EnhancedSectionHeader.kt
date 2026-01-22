package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.ui.theme.TaskTrackerTheme

@Composable
fun SectionHeader(dateKey: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SectionHeaderPreview() {
    TaskTrackerTheme {
        Column {
            SectionHeader(
                dateKey = "2024-01-21",
                label = "Today",
            )
            SectionHeader(
                dateKey = "2024-01-20",
                label = "Yesterday",
            )
            SectionHeader(
                dateKey = "2024-01-19",
                label = "January 19, 2024",
            )
        }
    }
}
