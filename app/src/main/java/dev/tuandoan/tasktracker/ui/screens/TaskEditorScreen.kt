package dev.tuandoan.tasktracker.ui.screens

import android.app.DatePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.model.DueDatePreset
import dev.tuandoan.tasktracker.domain.model.ReminderOption
import dev.tuandoan.tasktracker.ui.components.NotificationPermissionDialog
import dev.tuandoan.tasktracker.ui.components.PermissionDeniedDialog
import dev.tuandoan.tasktracker.ui.components.RecurrencePicker
import dev.tuandoan.tasktracker.ui.components.SubtaskListSection
import dev.tuandoan.tasktracker.ui.components.TimePickerDialog
import dev.tuandoan.tasktracker.ui.manager.NotificationPermissionManager
import dev.tuandoan.tasktracker.ui.viewmodel.TaskEditorEvent
import dev.tuandoan.tasktracker.ui.viewmodel.TaskEditorViewModel
import dev.tuandoan.tasktracker.utils.formatDate
import java.util.Calendar

/**
 * Full-screen task editor optimized for small devices.
 * Supports both create and edit modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    navController: NavController,
    viewModel: TaskEditorViewModel = hiltViewModel(),
    notificationPermissionManager: NotificationPermissionManager? = null,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val titleFocusRequester = remember { FocusRequester() }

    // Collect state
    val taskTitle by viewModel.taskTitle.collectAsStateWithLifecycle()
    val taskDescription by viewModel.taskDescription.collectAsStateWithLifecycle()
    val dueAt by viewModel.dueAt.collectAsStateWithLifecycle()
    val dueAtHasTime by viewModel.dueAtHasTime.collectAsStateWithLifecycle()
    val reminderOption by viewModel.reminderOption.collectAsStateWithLifecycle()
    val tag by viewModel.tag.collectAsStateWithLifecycle()
    val priority by viewModel.priority.collectAsStateWithLifecycle()
    val isPinned by viewModel.isPinned.collectAsStateWithLifecycle()
    val recurrenceType by viewModel.recurrenceType.collectAsStateWithLifecycle()
    val recurrenceInterval by viewModel.recurrenceInterval.collectAsStateWithLifecycle()
    val recurrenceDaysOfWeek by viewModel.recurrenceDaysOfWeek.collectAsStateWithLifecycle()
    val recurrenceEndDate by viewModel.recurrenceEndDate.collectAsStateWithLifecycle()
    val subtasks by viewModel.subtasks.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle(initialValue = false)

    // Validation errors
    val titleError by viewModel.titleError.collectAsStateWithLifecycle(initialValue = null)
    val dueDateError by viewModel.dueDateError.collectAsStateWithLifecycle(initialValue = null)
    val reminderError by viewModel.reminderError.collectAsStateWithLifecycle(initialValue = null)
    val tagError by viewModel.tagError.collectAsStateWithLifecycle(initialValue = null)

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }

    // Time picker state
    var showTimePicker by remember { mutableStateOf(false) }

    // Priority dropdown state
    var showPriorityDropdown by remember { mutableStateOf(false) }

    // Reminder dropdown state
    var showReminderDropdown by remember { mutableStateOf(false) }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskEditorEvent.TaskSaved -> {
                    keyboardController?.hide()
                    navController.popBackStack()
                }
                is TaskEditorEvent.TaskNotFound -> {
                    navController.popBackStack()
                }
            }
        }
    }

    // Auto-focus title on create mode
    LaunchedEffect(viewModel.isEditMode) {
        if (!viewModel.isEditMode) {
            titleFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (viewModel.isEditMode) R.string.title_edit_task else R.string.title_new_task,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveTask() },
                        enabled = isSaveEnabled && !isLoading,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Title field
            OutlinedTextField(
                value = taskTitle,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.label_title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester)
                    .then(
                        titleError?.let { err ->
                            Modifier.semantics { error(err) }
                        } ?: Modifier,
                    ),
                singleLine = true,
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = if (isSaveEnabled) ImeAction.Done else ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isSaveEnabled) {
                            keyboardController?.hide()
                            viewModel.saveTask()
                        }
                    },
                ),
            )

            // Description field
            OutlinedTextField(
                value = taskDescription,
                onValueChange = viewModel::updateDescription,
                label = { Text(stringResource(R.string.label_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )

            // Subtasks section
            SubtaskListSection(
                subtasks = subtasks,
                onAddSubtask = viewModel::addSubtaskDraft,
                onToggleSubtask = viewModel::toggleSubtaskDraft,
                onUpdateTitle = viewModel::updateSubtaskDraftTitle,
                onRemoveSubtask = viewModel::removeSubtaskDraft,
                onMoveSubtaskBy = viewModel::moveSubtaskDraftBy,
            )

            // Organization section
            Text(
                text = stringResource(R.string.section_organization),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            // Tag and Priority row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Tag field
                OutlinedTextField(
                    value = tag,
                    onValueChange = viewModel::updateTag,
                    label = { Text(stringResource(R.string.label_tag)) },
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            tagError?.let { err ->
                                Modifier.semantics { error(err) }
                            } ?: Modifier,
                        ),
                    singleLine = true,
                    isError = tagError != null,
                    supportingText = tagError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                )

                // Priority field
                val priorityName = when (priority) {
                    0 -> stringResource(R.string.priority_low)
                    1 -> stringResource(R.string.priority_medium)
                    2 -> stringResource(R.string.priority_high)
                    else -> stringResource(R.string.priority_medium)
                }
                val priorities = listOf(
                    0 to stringResource(R.string.priority_low),
                    1 to stringResource(R.string.priority_medium),
                    2 to stringResource(R.string.priority_high),
                )
                ExposedDropdownMenuBox(
                    expanded = showPriorityDropdown,
                    onExpandedChange = { showPriorityDropdown = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = priorityName,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.label_priority)) },
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPriorityDropdown) },
                    )

                    ExposedDropdownMenu(
                        expanded = showPriorityDropdown,
                        onDismissRequest = { showPriorityDropdown = false },
                    ) {
                        priorities.forEach { (priorityValue, priorityLabel) ->
                            DropdownMenuItem(
                                text = { Text(priorityLabel) },
                                onClick = {
                                    viewModel.updatePriority(priorityValue)
                                    showPriorityDropdown = false
                                },
                            )
                        }
                    }
                }
            }

            // Due date section
            Text(
                text = stringResource(R.string.section_due_date_reminder),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            // Due date quick-select chips
            val chipSelectedDesc = stringResource(R.string.a11y_chip_selected)
            val chipUnselectedDesc = stringResource(R.string.a11y_chip_not_selected)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DueDatePreset.entries.forEach { preset ->
                    // Compare date-only (presets always use 23:59, but user may have set a time)
                    val isSelected = dueAt != null &&
                        !dueAtHasTime &&
                        dueAt == preset.toEpochMillis()
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                viewModel.clearDueDate()
                            } else {
                                viewModel.setDueDatePreset(preset)
                            }
                        },
                        label = { Text(preset.label) },
                        modifier = Modifier.semantics {
                            stateDescription = if (isSelected) chipSelectedDesc else chipUnselectedDesc
                        },
                    )
                }
            }

            // Due date field
            val dateDisplayFormat = if (dueAtHasTime) {
                stringResource(R.string.date_format_short_with_time)
            } else {
                stringResource(R.string.date_format_short)
            }
            OutlinedTextField(
                value = dueAt?.let { formatDate(it, dateDisplayFormat) } ?: "",
                onValueChange = {},
                label = { Text(stringResource(R.string.label_due_date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        dueDateError?.let { err ->
                            Modifier.semantics { error(err) }
                        } ?: Modifier,
                    ),
                readOnly = true,
                isError = dueDateError != null,
                supportingText = dueDateError?.let { { Text(it) } },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(stringResource(R.string.action_select))
                    }
                },
            )

            // Add time / time chip (visible only when due date is set)
            if (dueAt != null) {
                if (dueAtHasTime) {
                    // Show time chip with close button
                    val timeText = formatDate(dueAt!!, stringResource(R.string.date_format_time_only))
                    val chipContentDesc = stringResource(R.string.cd_due_time, timeText)
                    AssistChip(
                        onClick = { showTimePicker = true },
                        label = { Text(timeText) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.clearDueTime() },
                                modifier = Modifier.size(18.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_remove_time),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = chipContentDesc
                        },
                    )
                } else {
                    // Show "Add time" button
                    TextButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription =
                                context.getString(R.string.cd_add_time_to_due_date)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(stringResource(R.string.action_add_time))
                    }
                }
            }

            // Reminder field (enabled only when due date is set)
            val reminderDisplayName = when (reminderOption) {
                ReminderOption.NONE -> stringResource(R.string.reminder_none)
                ReminderOption.MINUTES_1 -> stringResource(R.string.reminder_1_minute)
                ReminderOption.MINUTES_5 -> stringResource(R.string.reminder_5_minutes)
                ReminderOption.HOURS_1 -> stringResource(R.string.reminder_1_hour)
                ReminderOption.DAYS_1 -> stringResource(R.string.reminder_1_day)
            }
            val reminderOptions = mapOf(
                ReminderOption.NONE to stringResource(R.string.reminder_none),
                ReminderOption.MINUTES_1 to stringResource(R.string.reminder_1_minute),
                ReminderOption.MINUTES_5 to stringResource(R.string.reminder_5_minutes),
                ReminderOption.HOURS_1 to stringResource(R.string.reminder_1_hour),
                ReminderOption.DAYS_1 to stringResource(R.string.reminder_1_day),
            )
            ExposedDropdownMenuBox(
                expanded = showReminderDropdown,
                onExpandedChange = { showReminderDropdown = it && dueAt != null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = reminderDisplayName,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.label_reminder)) },
                    readOnly = true,
                    enabled = dueAt != null,
                    isError = reminderError != null,
                    supportingText = reminderError?.let { { Text(it) } },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .then(
                            reminderError?.let { err ->
                                Modifier.semantics { error(err) }
                            } ?: Modifier,
                        ),
                    trailingIcon = {
                        if (dueAt != null) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showReminderDropdown)
                        }
                    },
                )

                if (dueAt != null) {
                    ExposedDropdownMenu(
                        expanded = showReminderDropdown,
                        onDismissRequest = { showReminderDropdown = false },
                    ) {
                        ReminderOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(reminderOptions.getValue(option)) },
                                onClick = {
                                    // Check notification permission for reminder options (except NONE)
                                    if (option != ReminderOption.NONE && notificationPermissionManager != null) {
                                        if (notificationPermissionManager.shouldRequestPermission()) {
                                            // Request permission before setting reminder
                                            notificationPermissionManager.requestNotificationPermission { isGranted ->
                                                if (isGranted) {
                                                    viewModel.updateReminderOption(option)
                                                } else {
                                                    // Permission denied - revert to NONE
                                                    viewModel.updateReminderOption(ReminderOption.NONE)
                                                }
                                            }
                                        } else {
                                            // Permission already granted or not needed
                                            viewModel.updateReminderOption(option)
                                        }
                                    } else {
                                        // No permission manager or NONE selected
                                        viewModel.updateReminderOption(option)
                                    }
                                    showReminderDropdown = false
                                },
                            )
                        }
                    }
                }
            }

            // Recurrence picker (enabled only when due date is set)
            RecurrencePicker(
                recurrenceType = recurrenceType,
                recurrenceInterval = recurrenceInterval,
                recurrenceDaysOfWeek = recurrenceDaysOfWeek,
                recurrenceEndDate = recurrenceEndDate,
                enabled = dueAt != null,
                onTypeChanged = viewModel::updateRecurrenceType,
                onIntervalChanged = viewModel::updateRecurrenceInterval,
                onDayOfWeekToggled = viewModel::toggleRecurrenceDayOfWeek,
                onEndDateChanged = viewModel::updateRecurrenceEndDate,
            )

            // Pin toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.label_pin_task),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.label_pin_task_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isPinned,
                    onCheckedChange = viewModel::updateIsPinned,
                )
            }

            // Error message
            errorMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            // Bottom padding for better scrolling experience
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        if (dueAt != null) {
            calendar.timeInMillis = dueAt!!
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    if (dueAtHasTime && dueAt != null) {
                        // Preserve existing time when user changes date
                        timeInMillis = dueAt!!
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    } else {
                        set(year, month, dayOfMonth, 23, 59, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }
                viewModel.updateDueAt(selectedCalendar.timeInMillis)
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).apply {
            // Set minimum date to today
            datePicker.minDate = System.currentTimeMillis()
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }

    // Time picker dialog
    if (showTimePicker && dueAt != null) {
        val calendar = Calendar.getInstance().apply { timeInMillis = dueAt!! }
        val defaultHour: Int
        val defaultMinute: Int
        if (dueAtHasTime) {
            defaultHour = calendar.get(Calendar.HOUR_OF_DAY)
            defaultMinute = calendar.get(Calendar.MINUTE)
        } else {
            // Default to next 30-minute mark
            val now = Calendar.getInstance()
            defaultMinute = if (now.get(Calendar.MINUTE) < 30) 30 else 0
            defaultHour = if (defaultMinute == 0) {
                (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
            } else {
                now.get(Calendar.HOUR_OF_DAY)
            }
        }
        TimePickerDialog(
            initialHour = defaultHour,
            initialMinute = defaultMinute,
            is24Hour = DateFormat.is24HourFormat(context),
            onConfirm = { hour, minute ->
                viewModel.updateDueTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    // Notification permission dialogs
    notificationPermissionManager?.let { permissionManager ->
        // Permission rationale dialog
        if (permissionManager.showPermissionRationale) {
            NotificationPermissionDialog(
                onConfirm = { permissionManager.onPermissionRationaleConfirm() },
                onDismiss = { permissionManager.onPermissionRationaleDismiss() },
            )
        }

        // Permission denied dialog (when user needs to go to settings)
        if (permissionManager.showPermissionDenied) {
            PermissionDeniedDialog(
                onOpenSettings = { permissionManager.onPermissionDeniedOpenSettings() },
                onDismiss = { permissionManager.onPermissionDeniedDismiss() },
            )
        }
    }
}
