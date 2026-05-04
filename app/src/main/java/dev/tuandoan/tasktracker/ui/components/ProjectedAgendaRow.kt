package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.model.AgendaItem
import dev.tuandoan.tasktracker.ui.theme.AppSpacing

/**
 * Read-only row for an [AgendaItem.Projected] in the day agenda sheet (CAL-24).
 *
 * Visually distinct from [TaskItem]:
 * - dimmed container (surfaceContainerLow with reduced alpha) to read as "not yet real"
 * - a small "Upcoming" badge with a refresh icon so the row reads as "next occurrence"
 * - no checkbox, no overflow menu (read-only until tap materializes it)
 * - tapping the row forwards to [onClick]; the ViewModel materializes and then routes to
 *   the editor (ADR-002 option c — materialize-then-open)
 */
@Composable
fun ProjectedAgendaRow(projected: AgendaItem.Projected, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val priorityDotColor = priorityDotColor(projected.priority)
    val tagColor = TagColors.fromKey(projected.tagColor)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacing.taskItemHorizontalPadding,
                    vertical = AppSpacing.taskItemVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Priority dot (no checkbox — projection is read-only until materialized).
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(priorityDotColor),
            )

            Spacer(Modifier.width(AppSpacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = projected.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
                val tagText = projected.tag
                if (!tagText.isNullOrBlank()) {
                    Spacer(Modifier.height(AppSpacing.extraSmall))
                    Text(
                        text = tagText,
                        style = MaterialTheme.typography.labelSmall,
                        color = tagColor?.primary ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(AppSpacing.small))

            UpcomingBadge()
        }
    }
}

@Composable
private fun UpcomingBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(AppSpacing.extraSmall))
            Text(
                text = stringResource(R.string.agenda_upcoming_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/** Mirror of [TaskItem]'s priority-dot color mapping; kept local to avoid a wider refactor. */
@Composable
private fun priorityDotColor(priority: Int): Color = when (priority) {
    2 -> MaterialTheme.colorScheme.error // HIGH
    1 -> MaterialTheme.colorScheme.primary // MEDIUM
    else -> MaterialTheme.colorScheme.tertiary // LOW
}
