package dev.tuandoan.tasktracker.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.tuandoan.tasktracker.domain.model.ReminderOption
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
fun TaskEditorScreen(navController: NavController, viewModel: TaskEditorViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val titleFocusRequester = remember { FocusRequester() }

    // Collect state
    val taskTitle by viewModel.taskTitle.collectAsState()
    val taskDescription by viewModel.taskDescription.collectAsState()
    val dueAt by viewModel.dueAt.collectAsState()
    val reminderOption by viewModel.reminderOption.collectAsState()
    val tag by viewModel.tag.collectAsState()
    val priority by viewModel.priority.collectAsState()
    val isPinned by viewModel.isPinned.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsState(initial = false)

    // Validation errors
    val titleError by viewModel.titleError.collectAsState(initial = null)
    val dueDateError by viewModel.dueDateError.collectAsState(initial = null)
    val reminderError by viewModel.reminderError.collectAsState(initial = null)
    val tagError by viewModel.tagError.collectAsState(initial = null)

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }

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
                        text = if (viewModel.isEditMode) "Edit task" else "New task",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                            Text("Save")
                        }
                    }
                },
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
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
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
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )

            // Organization section
            Text(
                text = "Organization",
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
                    label = { Text("Tag") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = tagError != null,
                    supportingText = tagError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                )

                // Priority field
                ExposedDropdownMenuBox(
                    expanded = showPriorityDropdown,
                    onExpandedChange = { showPriorityDropdown = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = viewModel.getPriorityName(priority),
                        onValueChange = {},
                        label = { Text("Priority") },
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
                        viewModel.getAllPriorities().forEach { (priorityValue, priorityName) ->
                            DropdownMenuItem(
                                text = { Text(priorityName) },
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
                text = "Due Date & Reminder",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            // Due date field
            OutlinedTextField(
                value = dueAt?.let { formatDate(it) } ?: "",
                onValueChange = {},
                label = { Text("Due date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                isError = dueDateError != null,
                supportingText = dueDateError?.let { { Text(it) } },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Select")
                    }
                },
            )

            // Reminder field (enabled only when due date is set)
            ExposedDropdownMenuBox(
                expanded = showReminderDropdown,
                onExpandedChange = { showReminderDropdown = it && dueAt != null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = reminderOption.displayName,
                    onValueChange = {},
                    label = { Text("Reminder") },
                    readOnly = true,
                    enabled = dueAt != null,
                    isError = reminderError != null,
                    supportingText = reminderError?.let { { Text(it) } },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
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
                                text = { Text(option.displayName) },
                                onClick = {
                                    viewModel.updateReminderOption(option)
                                    showReminderDropdown = false
                                },
                            )
                        }
                    }
                }
            }

            // Pin toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Pin task",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Keep this task at the top",
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
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
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
}
