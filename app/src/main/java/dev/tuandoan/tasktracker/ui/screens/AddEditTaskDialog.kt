package dev.tuandoan.tasktracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.domain.model.Priority
import dev.tuandoan.tasktracker.domain.model.ReminderOption
import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.ui.theme.CustomShapes
import dev.tuandoan.tasktracker.ui.viewmodel.TaskViewModel
import dev.tuandoan.tasktracker.utils.NotificationPermission
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskDialog(viewModel: TaskViewModel, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val selectedTask by viewModel.selectedTask.collectAsStateWithLifecycle()
    val taskTitle by viewModel.taskTitle.collectAsStateWithLifecycle()
    val taskDescription by viewModel.taskDescription.collectAsStateWithLifecycle()
    val tag by viewModel.tag.collectAsStateWithLifecycle()
    val dueAt by viewModel.dueAt.collectAsStateWithLifecycle()
    val reminderOption by viewModel.reminderOption.collectAsStateWithLifecycle()
    val isFormValid by viewModel.isFormValid.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // New validation states
    val titleError by viewModel.titleError.collectAsStateWithLifecycle()
    val dueDateError by viewModel.dueDateError.collectAsStateWithLifecycle()
    val reminderError by viewModel.reminderError.collectAsStateWithLifecycle()
    val tagError by viewModel.tagError.collectAsStateWithLifecycle()
    val isTitleValid by viewModel.isTitleValid.collectAsStateWithLifecycle()
    val isDueDateValid by viewModel.isDueDateValid.collectAsStateWithLifecycle()
    val isReminderValid by viewModel.isReminderValid.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()

    val isEditing by viewModel.isEditMode.collectAsStateWithLifecycle()
    val dialogTitle = if (isEditing) "Edit Task" else "Add New Task"

    // Notification permission handling
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var permissionDeniedSnackbarMessage by remember { mutableStateOf<String?>(null) }
    var pendingReminderOption by remember { mutableStateOf<ReminderOption?>(null) }
    var shouldSaveAfterPermission by remember { mutableStateOf(false) }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            // Permission granted, apply the pending reminder option
            pendingReminderOption?.let { option ->
                viewModel.updateReminderOption(option)
                pendingReminderOption = null
            }
            // Save task if this was triggered from save button
            if (shouldSaveAfterPermission) {
                viewModel.saveTask()
                shouldSaveAfterPermission = false
            }
        } else {
            // Permission denied, show snackbar hint
            permissionDeniedSnackbarMessage = "Enable notifications in settings to receive reminders"
            // Still apply the reminder option (user can enable permission later)
            pendingReminderOption?.let { option ->
                viewModel.updateReminderOption(option)
                pendingReminderOption = null
            }
            // Save task even without permission (reminder won't work but task is saved)
            if (shouldSaveAfterPermission) {
                viewModel.saveTask()
                shouldSaveAfterPermission = false
            }
        }
    }

    // Function to check and handle reminder option change
    fun handleReminderOptionChange(newOption: ReminderOption) {
        if (newOption != ReminderOption.NONE &&
            NotificationPermission.needsPermissionRequest(context)
        ) {
            // Need to request permission
            pendingReminderOption = newOption
            showNotificationPermissionDialog = true
        } else {
            // No permission needed or already granted
            viewModel.updateReminderOption(newOption)
        }
    }

    // Focus and keyboard management
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus title field when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = CustomShapes.bottomSheet,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.dialogPadding),
            ) {
                // Enhanced dialog title with better typography
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(AppSpacing.sectionSpacing))

                // Basic Information Section
                Text(
                    text = "Task Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = AppSpacing.medium),
                )

                // Task Title Field with enhanced styling
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { viewModel.updateTaskTitle(it) },
                    label = {
                        Text(
                            "Task Title",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    placeholder = {
                        Text(
                            "Enter task title",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = {
                        Column {
                            titleError?.let { error ->
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Text(
                                text = "${taskTitle.length}/${TaskFormUseCase.MAX_TITLE_LENGTH}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (taskTitle.length > TaskFormUseCase.MAX_TITLE_LENGTH) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                },
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isSaveEnabled) {
                                keyboardController?.hide()
                                viewModel.saveTask()
                            }
                        },
                    ),
                    shape = CustomShapes.searchField,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                // Task Description Field with enhanced styling
                OutlinedTextField(
                    value = taskDescription,
                    onValueChange = { viewModel.updateTaskDescription(it) },
                    label = {
                        Text(
                            "Description (Optional)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    placeholder = {
                        Text(
                            "Add more details about your task...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    supportingText = {
                        Text(
                            text = "${taskDescription.length}/${TaskFormUseCase.MAX_DESCRIPTION_LENGTH}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (taskDescription.length > TaskFormUseCase.MAX_DESCRIPTION_LENGTH) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            },
                        )
                    },
                    shape = CustomShapes.searchField,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(modifier = Modifier.height(AppSpacing.sectionSpacing))

                // Organization Section
                Text(
                    text = "Organization",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = AppSpacing.medium),
                )

                // Tag and Priority Row for better space utilization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.large),
                ) {
                    // Tag Field
                    OutlinedTextField(
                        value = tag,
                        onValueChange = { viewModel.updateTag(it) },
                        label = {
                            Text(
                                "Tag (Optional)",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        placeholder = {
                            Text(
                                "e.g., work, personal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = tagError != null,
                        supportingText = {
                            Column {
                                tagError?.let { error ->
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                Text(
                                    text = "${tag.length}/${TaskFormUseCase.MAX_TAG_LENGTH}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (tag.length > TaskFormUseCase.MAX_TAG_LENGTH) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    },
                                )
                            }
                        },
                        shape = CustomShapes.searchField,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )

                    // Priority Selection with enhanced styling
                    var priorityExpanded by remember { mutableStateOf(false) }
                    val currentPriority = remember(selectedTask) {
                        selectedTask?.let { Priority.fromValue(it.priority) } ?: Priority.MEDIUM
                    }
                    var selectedPriority by remember(selectedTask) {
                        mutableStateOf(currentPriority)
                    }

                    ExposedDropdownMenuBox(
                        expanded = priorityExpanded,
                        onExpandedChange = { priorityExpanded = !priorityExpanded },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = selectedPriority.displayName,
                            onValueChange = { },
                            label = {
                                Text(
                                    "Priority",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = priorityExpanded,
                                )
                            },
                            shape = CustomShapes.searchField,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )

                        DropdownMenu(
                            expanded = priorityExpanded,
                            onDismissRequest = { priorityExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                        ) {
                            Priority.entries.forEach { priority ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = when (priority) {
                                                    Priority.HIGH -> Icons.Default.KeyboardArrowUp
                                                    Priority.LOW -> Icons.Default.KeyboardArrowDown
                                                    else -> Icons.Default.Remove
                                                },
                                                contentDescription = null,
                                                tint = when (priority) {
                                                    Priority.HIGH -> MaterialTheme.colorScheme.error
                                                    Priority.LOW -> MaterialTheme.colorScheme.secondary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(AppSpacing.small))
                                            Text(
                                                text = priority.displayName,
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPriority = priority
                                        selectedTask?.let { task ->
                                            viewModel.updateTaskPriority(task.id, priority.value)
                                        }
                                        priorityExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Due Date Field
                OutlinedTextField(
                    value = if (dueAt != null) {
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                        dateFormat.format(Date(dueAt!!))
                    } else {
                        ""
                    },
                    onValueChange = { /* Read-only field - use date picker */ },
                    label = { Text("Due Date (Optional)") },
                    placeholder = { Text("Set due date and time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    isError = dueDateError != null,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                // For now, set to 8 hours from now as a sensible default
                                // This ensures the due date is always in the future
                                val currentTime = System.currentTimeMillis()
                                val eightHoursLater = currentTime + (8 * 60 * 60 * 1000L) // Add 8 hours
                                viewModel.updateDueAt(eightHoursLater)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date and time",
                            )
                        }
                    },
                    supportingText = {
                        Column {
                            dueDateError?.let { error ->
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            if (dueAt != null) {
                                Row {
                                    TextButton(
                                        onClick = { viewModel.updateDueAt(null) },
                                        modifier = Modifier.padding(0.dp),
                                    ) {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reminder Selection
                var reminderExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = reminderExpanded,
                    onExpandedChange = { reminderExpanded = !reminderExpanded },
                ) {
                    OutlinedTextField(
                        value = reminderOption.displayName,
                        onValueChange = { },
                        label = { Text("Reminder") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderExpanded)
                        },
                        isError = reminderError != null,
                        supportingText = {
                            reminderError?.let { error ->
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )

                    ExposedDropdownMenu(
                        expanded = reminderExpanded,
                        onDismissRequest = { reminderExpanded = false },
                    ) {
                        // None option
                        DropdownMenuItem(
                            text = { Text(ReminderOption.NONE.displayName) },
                            onClick = {
                                viewModel.updateReminderOption(ReminderOption.NONE)
                                reminderExpanded = false
                            },
                        )

                        // Selectable options
                        ReminderOption.getSelectableOptions().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    handleReminderOptionChange(option)
                                    reminderExpanded = false
                                },
                                enabled = dueAt != null, // Only allow selection if due date is set
                            )
                        }
                    }
                }

                // Error Message
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Permission denied snackbar message
                permissionDeniedSnackbarMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                TextButton(
                                    onClick = { permissionDeniedSnackbarMessage = null },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                ) {
                                    Text("Dismiss")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        NotificationPermission.openAppNotificationSettings(context)
                                        permissionDeniedSnackbarMessage = null
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                ) {
                                    Text("Open Settings")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(AppSpacing.sectionSpacing))

                // Enhanced Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline),
                        ),
                        shape = CustomShapes.searchField,
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(modifier = Modifier.width(AppSpacing.buttonSpacing))

                    Button(
                        onClick = {
                            // Check for notification permission before saving if reminder is set
                            if (reminderOption != ReminderOption.NONE &&
                                NotificationPermission.needsPermissionRequest(context)
                            ) {
                                // Need to request permission before saving
                                pendingReminderOption = reminderOption
                                shouldSaveAfterPermission = true
                                showNotificationPermissionDialog = true
                            } else {
                                // Save normally
                                viewModel.saveTask()
                            }
                        },
                        enabled = isSaveEnabled && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
                        ),
                        shape = CustomShapes.searchField,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp,
                        ),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.extraSmall))
                                Text(
                                    text = if (isEditing) "Update Task" else "Add Task",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Permission education dialog
    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showNotificationPermissionDialog = false
                pendingReminderOption = null
                shouldSaveAfterPermission = false
            },
            title = {
                Text(
                    text = "Enable notifications?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = "Notifications are required for task reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationPermissionDialog = false
                        permissionLauncher.launch(NotificationPermission.getPermissionString())
                    },
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationPermissionDialog = false
                        // Apply the reminder option even without permission (user can enable later)
                        pendingReminderOption?.let { option ->
                            viewModel.updateReminderOption(option)
                        }
                        pendingReminderOption = null
                        permissionDeniedSnackbarMessage = "Enable notifications in settings to receive reminders"
                        // Save task if this was triggered from save button
                        if (shouldSaveAfterPermission) {
                            viewModel.saveTask()
                            shouldSaveAfterPermission = false
                        }
                    },
                ) {
                    Text("Not now")
                }
            },
        )
    }
}
