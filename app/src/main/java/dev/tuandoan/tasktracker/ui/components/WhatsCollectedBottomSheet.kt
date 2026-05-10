package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.ui.theme.AppSpacing

/**
 * "What's collected?" bottom sheet (v1.12.0, FB-08).
 *
 * Opened from the Settings → Privacy section to disclose exactly what each Firebase
 * SDK collects. Static content — no ViewModel. Three section blocks matching the
 * three SDKs wired up in FB-05's `PrivacyManager`:
 *  - Crashlytics (crash stack traces, device metadata, random install ID)
 *  - Analytics (screen + event names, app version/locale, **never** task content)
 *  - Performance (cold-start, render times, **never** screen contents)
 *
 * The "Never:" lines on Analytics + Performance are deliberately repeated per
 * section — they're the reassurance users care most about and shouldn't rely on
 * them remembering the once-stated promise two bullets up.
 *
 * Content is wrapped in `verticalScroll` so long locale translations (FB-09) or
 * large accessibility font sizes don't clip on small devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsCollectedBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.screenPadding),
        ) {
            Text(
                text = stringResource(R.string.whats_collected_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(bottom = AppSpacing.large)
                    .semantics { heading() },
            )

            SectionBlock(
                header = stringResource(R.string.whats_collected_crashlytics_header),
                bullets = listOf(
                    stringResource(R.string.whats_collected_crashlytics_1),
                    stringResource(R.string.whats_collected_crashlytics_2),
                    stringResource(R.string.whats_collected_crashlytics_3),
                ),
            )

            SectionBlock(
                header = stringResource(R.string.whats_collected_analytics_header),
                bullets = listOf(
                    stringResource(R.string.whats_collected_analytics_1),
                    stringResource(R.string.whats_collected_analytics_2),
                    stringResource(R.string.whats_collected_analytics_3),
                ),
            )

            SectionBlock(
                header = stringResource(R.string.whats_collected_performance_header),
                bullets = listOf(
                    stringResource(R.string.whats_collected_performance_1),
                    stringResource(R.string.whats_collected_performance_2),
                    stringResource(R.string.whats_collected_performance_3),
                ),
            )

            Spacer(modifier = Modifier.height(AppSpacing.large))
        }
    }
}

@Composable
private fun SectionBlock(header: String, bullets: List<String>) {
    Text(
        text = header,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .padding(top = AppSpacing.medium, bottom = AppSpacing.small)
            .semantics { heading() },
    )
    bullets.forEach { bullet ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.extraSmall),
        ) {
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = AppSpacing.small),
            )
            Text(
                text = bullet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
