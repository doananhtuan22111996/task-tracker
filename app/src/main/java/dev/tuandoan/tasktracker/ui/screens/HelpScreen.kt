package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R

private data class FaqItem(val questionRes: Int, val answerRes: Int)
private data class FaqSection(val titleRes: Int, val items: List<FaqItem>)

private val faqSections = listOf(
    FaqSection(
        R.string.help_section_getting_started,
        listOf(
            FaqItem(R.string.help_q_create_task, R.string.help_a_create_task),
            FaqItem(R.string.help_q_need_account, R.string.help_a_need_account),
        ),
    ),
    FaqSection(
        R.string.help_section_managing_tasks,
        listOf(
            FaqItem(R.string.help_q_filter_by_tag, R.string.help_a_filter_by_tag),
            FaqItem(R.string.help_q_change_priority, R.string.help_a_change_priority),
            FaqItem(R.string.help_q_multi_select, R.string.help_a_multi_select),
        ),
    ),
    FaqSection(
        R.string.help_section_reminders,
        listOf(
            FaqItem(R.string.help_q_no_reminder, R.string.help_a_no_reminder),
            FaqItem(R.string.help_q_reminder_no_due, R.string.help_a_reminder_no_due),
        ),
    ),
    FaqSection(
        R.string.help_section_archive,
        listOf(
            FaqItem(R.string.help_q_archive_task, R.string.help_a_archive_task),
            FaqItem(R.string.help_q_restore_archived, R.string.help_a_restore_archived),
        ),
    ),
    FaqSection(
        R.string.help_section_stats,
        listOf(
            FaqItem(R.string.help_q_completion_rate, R.string.help_a_completion_rate),
            FaqItem(R.string.help_q_tap_stat_card, R.string.help_a_tap_stat_card),
        ),
    ),
    FaqSection(
        R.string.help_section_backup,
        listOf(
            FaqItem(R.string.help_q_backup_tasks, R.string.help_a_backup_tasks),
            FaqItem(R.string.help_q_export_csv, R.string.help_a_export_csv),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    val expandedItems = remember { mutableStateMapOf<Int, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_help_faq),
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
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            faqSections.forEach { section ->
                item(key = "header_${section.titleRes}") {
                    Text(
                        text = stringResource(section.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 8.dp,
                        ),
                    )
                }
                items(
                    items = section.items,
                    key = { it.questionRes },
                ) { faqItem ->
                    val isExpanded = expandedItems[faqItem.questionRes] ?: false
                    val question = stringResource(faqItem.questionRes)
                    val answer = stringResource(faqItem.answerRes)
                    val expandedLabel = if (isExpanded) {
                        stringResource(R.string.cd_faq_expanded)
                    } else {
                        stringResource(R.string.cd_faq_collapsed)
                    }

                    Column(
                        modifier = Modifier.semantics {
                            contentDescription = "$question, $expandedLabel"
                        },
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = question,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = if (isExpanded) {
                                        Icons.Default.ExpandLess
                                    } else {
                                        Icons.Default.ExpandMore
                                    },
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    expandedItems[faqItem.questionRes] = !isExpanded
                                },
                        )
                        AnimatedVisibility(visible = isExpanded) {
                            Text(
                                text = answer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 12.dp,
                                ),
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}
