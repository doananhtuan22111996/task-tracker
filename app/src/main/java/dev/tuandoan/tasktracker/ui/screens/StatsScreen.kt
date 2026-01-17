package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.ui.viewmodel.StatsViewModel

/**
 * Stats screen showing lightweight task statistics.
 *
 * Displays simple numeric counts:
 * - Active tasks
 * - Completed tasks (overall)
 * - Completed today
 * - Due today
 * - Overdue tasks
 *
 * All stats exclude archived tasks by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Active tasks
            StatCard(
                title = "Active Tasks",
                count = uiState.activeCount,
                description = "Tasks not yet completed",
                icon = Icons.Default.PlayArrow,
            )

            // Completed tasks (overall)
            StatCard(
                title = "Completed Tasks",
                count = uiState.completedCount,
                description = "All completed tasks",
                icon = Icons.Default.CheckCircle,
            )

            // Completed today
            StatCard(
                title = "Completed Today",
                count = uiState.completedTodayCount,
                description = "Tasks completed today",
                icon = Icons.Default.Today,
            )

            // Due today
            StatCard(
                title = "Due Today",
                count = uiState.dueTodayCount,
                description = "Active tasks due today",
                icon = Icons.Default.Schedule,
            )

            // Overdue tasks
            StatCard(
                title = "Overdue",
                count = uiState.overdueCount,
                description = "Active tasks past due date",
                icon = Icons.Default.Error,
            )

            // Note about archived tasks - improved styling
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Archived tasks are excluded from all statistics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Enhanced stat card with icon, more compact layout and better visual hierarchy
 */
@Composable
private fun StatCard(
    title: String,
    count: Int,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // More compact padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall, // Slightly smaller for compactness
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall, // Slightly smaller but still prominent
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
            )
        }
    }
}
