package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.database.DailyCount
import dev.tuandoan.tasktracker.navigation.StatsFilter
import dev.tuandoan.tasktracker.ui.components.FeatureTip
import dev.tuandoan.tasktracker.ui.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFilteredList: (StatsFilter) -> Unit = {},
    onNavigateToCreateTask: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dailyProgress by viewModel.dailyProgress.collectAsStateWithLifecycle()
    val weeklyBreakdown by viewModel.weeklyBreakdown.collectAsStateWithLifecycle()
    val completionRate by viewModel.completionRate.collectAsStateWithLifecycle()
    val isEmpty by viewModel.isEmpty.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_stats)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (isEmpty) {
            StatsEmptyState(
                onCreateTask = onNavigateToCreateTask,
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Feature tip (SPEC-O02)
                FeatureTip(
                    text = stringResource(R.string.tip_stats_cards),
                    visible = !userPrefs.tipStatsCardsShown,
                    onDismiss = viewModel::setTipStatsCardsShown,
                )

                // TODAY section (SPEC-S06)
                Text(
                    text = stringResource(R.string.stat_section_today),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                HorizontalDivider()

                // Completed today
                StatCard(
                    title = stringResource(R.string.stat_completed_today),
                    count = uiState.completedTodayCount,
                    description = stringResource(R.string.stat_desc_completed_today),
                    icon = Icons.Default.Today,
                    contentDescriptionText = stringResource(
                        R.string.stat_cd_completed_today,
                        uiState.completedTodayCount,
                    ),
                    onClick = { onNavigateToFilteredList(StatsFilter.COMPLETED_TODAY) },
                )

                // Daily progress bar (SPEC-S03)
                DailyProgressSection(
                    completedToday = uiState.completedTodayCount,
                    dueToday = uiState.dueTodayCount,
                    progress = dailyProgress,
                )

                // Due today
                StatCard(
                    title = stringResource(R.string.stat_due_today),
                    count = uiState.dueTodayCount,
                    description = stringResource(R.string.stat_desc_due_today),
                    icon = Icons.Default.Schedule,
                    contentDescriptionText = stringResource(
                        R.string.stat_cd_due_today,
                        uiState.dueTodayCount,
                    ),
                    onClick = { onNavigateToFilteredList(StatsFilter.DUE_TODAY) },
                )

                // Overdue (SPEC-S02: urgency styling)
                StatCard(
                    title = stringResource(R.string.stat_overdue),
                    count = uiState.overdueCount,
                    description = stringResource(R.string.stat_desc_overdue),
                    icon = Icons.Default.Error,
                    isUrgent = uiState.overdueCount > 0,
                    contentDescriptionText = if (uiState.overdueCount > 0) {
                        stringResource(R.string.stat_cd_overdue, uiState.overdueCount)
                    } else {
                        stringResource(R.string.stat_cd_overdue_clear)
                    },
                    onClick = { onNavigateToFilteredList(StatsFilter.OVERDUE) },
                )

                Spacer(Modifier.height(8.dp))

                // ALL TIME section (SPEC-S06)
                Text(
                    text = stringResource(R.string.stat_section_all_time),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                HorizontalDivider()

                // Active tasks
                StatCard(
                    title = stringResource(R.string.stat_active_tasks),
                    count = uiState.activeCount,
                    description = stringResource(R.string.stat_desc_active),
                    icon = Icons.Default.PlayArrow,
                    contentDescriptionText = stringResource(
                        R.string.stat_cd_active,
                        uiState.activeCount,
                    ),
                    onClick = { onNavigateToFilteredList(StatsFilter.ACTIVE) },
                )

                // Completed tasks (overall)
                StatCard(
                    title = stringResource(R.string.stat_completed_tasks),
                    count = uiState.completedCount,
                    description = stringResource(R.string.stat_desc_completed),
                    icon = Icons.Default.CheckCircle,
                    contentDescriptionText = stringResource(
                        R.string.stat_cd_completed,
                        uiState.completedCount,
                    ),
                    onClick = { onNavigateToFilteredList(StatsFilter.COMPLETED) },
                )

                // Completion rate card (SPEC-S05)
                CompletionRateCard(completionRate = completionRate)

                Spacer(Modifier.height(8.dp))

                // Weekly breakdown (SPEC-S04)
                if (weeklyBreakdown.isNotEmpty()) {
                    WeeklyBreakdownSection(weeklyBreakdown = weeklyBreakdown)
                }

                Spacer(Modifier.height(4.dp))

                // Note about archived tasks
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
                            text = stringResource(R.string.stat_archived_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// SPEC-S07: Empty state
@Composable
private fun StatsEmptyState(onCreateTask: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(32.dp),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.stat_empty_headline),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCreateTask) {
            Text(stringResource(R.string.stat_empty_cta))
        }
    }
}

// SPEC-S03: Daily progress bar
@Composable
private fun DailyProgressSection(completedToday: Int, dueToday: Int, progress: Float, modifier: Modifier = Modifier) {
    val progressCd = stringResource(R.string.stat_cd_progress, completedToday, dueToday)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = progressCd },
    ) {
        Text(
            text = if (dueToday == 0) {
                stringResource(R.string.stat_all_caught_up)
            } else {
                stringResource(R.string.stat_daily_progress, completedToday, dueToday)
            },
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}

// SPEC-S05: Completion rate card
@Composable
private fun CompletionRateCard(completionRate: Int?, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stat_completion_rate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (completionRate != null) "$completionRate%" else "–",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
            )
        }
    }
}

// SPEC-S04: Weekly breakdown bar chart
@Composable
private fun WeeklyBreakdownSection(weeklyBreakdown: List<DailyCount>, modifier: Modifier = Modifier) {
    val weekSummary = weeklyBreakdown.joinToString(", ") { "${it.date} ${it.count}" }
    val sectionCd = stringResource(R.string.stat_this_week) + ": " + weekSummary

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.stat_this_week),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        val maxCount = weeklyBreakdown.maxOfOrNull { it.count } ?: 1
        val barColor = MaterialTheme.colorScheme.primary
        val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .semantics { contentDescription = sectionCd },
        ) {
            val barCount = weeklyBreakdown.size
            if (barCount == 0) return@Canvas
            val gap = 8.dp.toPx()
            val barWidth = (size.width - (barCount - 1) * gap) / barCount
            val maxBarHeight = size.height - 24.dp.toPx() // Leave space for day labels

            weeklyBreakdown.forEachIndexed { index, dailyCount ->
                val x = index * (barWidth + gap)
                val barHeight = if (maxCount > 0) {
                    (dailyCount.count.toFloat() / maxCount) * maxBarHeight
                } else {
                    0f
                }
                val minBarHeight = 2.dp.toPx()
                val actualHeight = if (dailyCount.count == 0) minBarHeight else barHeight.coerceAtLeast(minBarHeight)
                val color = if (dailyCount.count == 0) emptyBarColor else barColor

                drawRect(
                    color = color,
                    topLeft = Offset(x, maxBarHeight - actualHeight),
                    size = Size(barWidth, actualHeight),
                )
            }
        }

        // Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            weeklyBreakdown.forEach { dailyCount ->
                Text(
                    text = dailyCount.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// SPEC-S01 (tappable), SPEC-S02 (urgency), SPEC-S08 (accessibility)
@Composable
private fun StatCard(
    title: String,
    count: Int,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isUrgent: Boolean = false,
    contentDescriptionText: String = "",
    onClick: (() -> Unit)? = null,
) {
    val containerColor = if (isUrgent) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        CardDefaults.elevatedCardColors().containerColor
    }
    val contentColor = if (isUrgent) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        CardDefaults.elevatedCardColors().contentColor
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                if (contentDescriptionText.isNotEmpty()) {
                    contentDescription = contentDescriptionText
                }
                if (onClick != null) role = Role.Button
            }
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    tint = if (isUrgent) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
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
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUrgent) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isUrgent) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                textAlign = TextAlign.End,
            )

            // SPEC-S01: trailing arrow for tappable cards
            if (onClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Default.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isUrgent) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
