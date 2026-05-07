package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.ui.theme.AppSpacing

/**
 * CAL-16 empty-state hint card. Rendered above the month grid in [CalendarScreen] when the
 * database holds zero dated tasks (`CalendarUiState.hasAnyDatedTask == false`). Carries a
 * short explainer plus a CTA that opens the task editor in create mode with today's date
 * pre-seeded via the same `onNavigateToCreateForDay` callback the agenda FAB uses (CAL-19).
 *
 * Visual grammar matches the CAL-22 empty-day polish: icon + short body + tonal button,
 * sitting inside a surface-tinted Card so the grid below still reads as the primary surface.
 */
@Composable
fun CalendarEmptyStateCard(onAddTaskClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            Icon(
                imageVector = Icons.Outlined.EventAvailable,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = stringResource(R.string.calendar_empty_state_title),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.calendar_empty_state_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppSpacing.extraSmall))
            FilledTonalButton(onClick = onAddTaskClick) {
                Text(stringResource(R.string.calendar_empty_state_cta))
            }
        }
    }
}
