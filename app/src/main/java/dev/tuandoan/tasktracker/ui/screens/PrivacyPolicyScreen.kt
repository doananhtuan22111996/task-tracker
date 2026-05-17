package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R

/**
 * In-app Privacy Policy screen (FB-18). Reachable from Settings → Privacy → Privacy Policy.
 *
 * Static content. Pure read-only — no ViewModel, no analytics emission. The policy text is
 * the public-facing legal contract for the v1.12.0 Firebase integration; every claim it
 * makes must match what the code actually does (per `feedback_copy_verification`).
 *
 * Layout is a vertical-scrolling [Column] of headed sections — Material3 typography ramps
 * (`titleLarge` for the screen subject, `titleMedium` for section headers, `bodyMedium` for
 * body copy). Section headers carry a `semantics { heading() }` tag so TalkBack users can
 * jump between sections by heading-navigation gesture.
 *
 * The on-disable promise ("queued reports are deleted") is verifiable in
 * [PrivacyManager.setEnabled] which calls `Crashlytics.deleteUnsentReports()` on the
 * disable path. The 90-day retention claim is Firebase's default for Analytics +
 * Performance Monitoring (per Google's docs, not user-configurable in this app).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_privacy_policy),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            BodyText(textRes = R.string.privacy_policy_intro)

            SectionHeader(R.string.privacy_policy_section_what_we_collect)
            BodyText(R.string.privacy_policy_what_we_collect_intro)
            BodyText(R.string.privacy_policy_what_we_collect_crashlytics)
            BodyText(R.string.privacy_policy_what_we_collect_analytics)
            BodyText(R.string.privacy_policy_what_we_collect_performance)

            SectionHeader(R.string.privacy_policy_section_why)
            BodyText(R.string.privacy_policy_why_body)

            SectionHeader(R.string.privacy_policy_section_retention)
            BodyText(R.string.privacy_policy_retention_body)

            SectionHeader(R.string.privacy_policy_section_your_control)
            BodyText(R.string.privacy_policy_your_control_body)

            SectionHeader(R.string.privacy_policy_section_third_party)
            BodyText(R.string.privacy_policy_third_party_body)

            SectionHeader(R.string.privacy_policy_section_no_account)
            BodyText(R.string.privacy_policy_no_account_body)

            SectionHeader(R.string.privacy_policy_section_changes)
            BodyText(R.string.privacy_policy_changes_body)

            SectionHeader(R.string.privacy_policy_section_effective)
            BodyText(R.string.privacy_policy_effective_body)

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(textRes: Int) {
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun BodyText(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
