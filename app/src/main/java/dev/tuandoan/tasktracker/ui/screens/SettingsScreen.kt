package dev.tuandoan.tasktracker.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.preferences.ThemeMode
import dev.tuandoan.tasktracker.domain.backup.model.BackupFormat
import dev.tuandoan.tasktracker.ui.viewmodel.SettingsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generates a default backup filename with the current timestamp.
 * Called each time an export is initiated to ensure unique filenames.
 */
private fun generateBackupFileName(): String {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    return "task_tracker_backup_$timestamp"
}

/**
 * Settings screen with Appearance, Language, and Backup & Restore sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit, onNavigateToHelp: () -> Unit = {}) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showImportConfirmation by viewModel.showImportConfirmation.collectAsStateWithLifecycle()
    val showErrorDialog by viewModel.showErrorDialog.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showLanguageDialog by remember { mutableStateOf(false) }

    // Resolve app version
    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    // SAF launchers
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { viewModel.exportBackup(it, BackupFormat.JSON, appVersion) }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let { viewModel.exportBackup(it, BackupFormat.CSV, appVersion) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.requestImport(it) }
    }

    // Collect snackbar messages
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    // Visual dimming when loading to indicate disabled state
    val contentAlpha = if (isLoading) 0.5f else 1f

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_settings),
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
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(contentAlpha)
                    .verticalScroll(rememberScrollState()),
            ) {
                // =============================================
                // Appearance Section
                // =============================================
                SectionHeader(text = stringResource(R.string.settings_section_appearance))

                // Theme selector
                ThemeSelector(
                    currentTheme = userPreferences.themeMode,
                    onThemeSelected = { viewModel.setThemeMode(it) },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                // Dynamic Color toggle
                DynamicColorToggle(
                    enabled = userPreferences.dynamicColor,
                    onToggle = { viewModel.setDynamicColor(it) },
                )

                // =============================================
                // Language Section
                // =============================================
                SectionHeader(text = stringResource(R.string.settings_section_language))

                LanguageSelector(
                    currentTag = userPreferences.languageTag,
                    onClick = { showLanguageDialog = true },
                    supportedLocales = viewModel.getSupportedLocales(),
                )

                // =============================================
                // Backup & Restore Section
                // =============================================
                SectionHeader(text = stringResource(R.string.settings_section_backup))

                // Export backup (JSON)
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_export_backup),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_export_backup_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading, role = Role.Button) {
                            jsonExportLauncher.launch("${generateBackupFileName()}.json")
                        }
                        .semantics {
                            contentDescription = context.getString(R.string.cd_export_json)
                        },
                )

                // Import backup
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_import_backup),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_import_backup_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading, role = Role.Button) {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                        .semantics {
                            contentDescription = context.getString(R.string.cd_import_json)
                        },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                // Export as CSV
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_export_csv),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_export_csv_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading, role = Role.Button) {
                            csvExportLauncher.launch("${generateBackupFileName()}.csv")
                        }
                        .semantics {
                            contentDescription = context.getString(R.string.cd_export_csv)
                        },
                )

                // =============================================
                // Help & FAQ Section
                // =============================================
                SectionHeader(text = stringResource(R.string.settings_help_faq))

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_help_faq),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_help_faq_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onNavigateToHelp),
                )

                // =============================================
                // Feedback Section
                // =============================================
                SectionHeader(text = stringResource(R.string.settings_section_feedback))

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_send_feedback),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_send_feedback_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Feedback,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(
                                    Intent.EXTRA_EMAIL,
                                    arrayOf("doananhtuan22111996@gmail.com"),
                                )
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    context.getString(R.string.feedback_email_subject),
                                )
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        context.getString(R.string.settings_send_feedback),
                                    ),
                                )
                            }
                        }
                        .semantics {
                            contentDescription =
                                context.getString(R.string.cd_send_feedback)
                        },
                )

                // =============================================
                // More Apps Section
                // =============================================
                SectionHeader(text = stringResource(R.string.settings_section_more_apps))

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_expense_tracker),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    supportingContent = {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_expense_tracker_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_view_on_play_store),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=dev.tuandoan.expensetracker"),
                            )
                            runCatching { context.startActivity(intent) }
                        }
                        .semantics {
                            contentDescription =
                                context.getString(R.string.cd_open_expense_tracker)
                        },
                )
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = context.getString(R.string.cd_loading)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Language picker dialog
    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentTag = userPreferences.languageTag,
            supportedLocales = viewModel.getSupportedLocales(),
            onLanguageSelected = { tag ->
                viewModel.setLanguageTag(tag)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    // Import confirmation dialog
    if (showImportConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelImport() },
            title = {
                Text(stringResource(R.string.dialog_replace_tasks_title))
            },
            text = {
                Text(stringResource(R.string.dialog_replace_tasks_message))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport() }) {
                    Text(stringResource(R.string.action_replace))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelImport() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Error dialog
    showErrorDialog?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = {
                Text(stringResource(R.string.dialog_error_title))
            },
            text = {
                Text(errorMessage)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }
}

// =============================================
// Reusable Section Components
// =============================================

/**
 * Section header styled with primary color and consistent padding.
 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
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

/**
 * Theme selector using radio buttons for Light, Dark, and System options.
 */
@Composable
private fun ThemeSelector(currentTheme: ThemeMode, onThemeSelected: (ThemeMode) -> Unit) {
    val themeOptions = listOf(
        ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
        ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
        ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        themeOptions.forEach { (mode, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = currentTheme == mode,
                        onClick = { onThemeSelected(mode) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                RadioButton(
                    selected = currentTheme == mode,
                    onClick = null,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * Dynamic Color toggle with support detection for Android 12+.
 */
@Composable
private fun DynamicColorToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val isDynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.settings_dynamic_color),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        supportingContent = {
            Text(
                text = if (isDynamicColorAvailable) {
                    stringResource(R.string.settings_dynamic_color_description)
                } else {
                    stringResource(R.string.settings_dynamic_color_unavailable)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Switch(
                checked = enabled && isDynamicColorAvailable,
                onCheckedChange = { onToggle(it) },
                enabled = isDynamicColorAvailable,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDynamicColorAvailable) 1f else 0.6f),
    )
}

/**
 * Language selector showing the current language and opening a dialog on click.
 */
@Composable
private fun LanguageSelector(currentTag: String, onClick: () -> Unit, supportedLocales: List<Pair<String, String>>) {
    val currentDisplayName = if (currentTag.isEmpty()) {
        stringResource(R.string.settings_language_system)
    } else {
        supportedLocales.find { it.first == currentTag }?.second ?: currentTag
    }

    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        supportingContent = {
            Text(
                text = currentDisplayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button),
    )
}

/**
 * Dialog for selecting a language from supported locales.
 * Includes a "Follow system" option at the top.
 */
@Composable
private fun LanguagePickerDialog(
    currentTag: String,
    supportedLocales: List<Pair<String, String>>,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val followSystemLabel = stringResource(R.string.settings_language_system)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_language))
        },
        text = {
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState()),
            ) {
                // "Follow system" option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentTag.isEmpty(),
                            onClick = { onLanguageSelected("") },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 8.dp),
                ) {
                    RadioButton(
                        selected = currentTag.isEmpty(),
                        onClick = null,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = followSystemLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Supported locale options
                supportedLocales.forEach { (tag, displayName) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentTag == tag,
                                onClick = { onLanguageSelected(tag) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(
                            selected = currentTag == tag,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
