package dev.tuandoan.tasktracker.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.utils.formatDate
import java.time.DayOfWeek
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrencePicker(
    recurrenceType: RecurrenceType,
    recurrenceInterval: Int,
    recurrenceDaysOfWeek: Set<DayOfWeek>,
    recurrenceEndDate: Long?,
    enabled: Boolean,
    onTypeChanged: (RecurrenceType) -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onDayOfWeekToggled: (DayOfWeek) -> Unit,
    onEndDateChanged: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Recurrence type dropdown
        RecurrenceTypeDropdown(
            selectedType = recurrenceType,
            enabled = enabled,
            onTypeSelected = onTypeChanged,
        )

        // Custom options (visible when recurrence is active)
        if (recurrenceType != RecurrenceType.NONE) {
            // Interval
            RecurrenceIntervalRow(
                interval = recurrenceInterval,
                type = recurrenceType,
                onIntervalChanged = onIntervalChanged,
            )

            // Days of week (weekly only)
            if (recurrenceType == RecurrenceType.WEEKLY) {
                DaysOfWeekSelector(
                    selectedDays = recurrenceDaysOfWeek,
                    onDayToggled = onDayOfWeekToggled,
                )
            }

            // End date
            RecurrenceEndDateField(
                endDate = recurrenceEndDate,
                onEndDateChanged = onEndDateChanged,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrenceTypeDropdown(
    selectedType: RecurrenceType,
    enabled: Boolean,
    onTypeSelected: (RecurrenceType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val typeLabels = mapOf(
        RecurrenceType.NONE to stringResource(R.string.recurrence_none),
        RecurrenceType.DAILY to stringResource(R.string.recurrence_daily),
        RecurrenceType.WEEKLY to stringResource(R.string.recurrence_weekly),
        RecurrenceType.MONTHLY to stringResource(R.string.recurrence_monthly),
        RecurrenceType.YEARLY to stringResource(R.string.recurrence_yearly),
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && enabled },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = typeLabels.getValue(selectedType),
            onValueChange = {},
            label = { Text(stringResource(R.string.label_recurrence)) },
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = {
                if (enabled) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
        )

        if (enabled) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                typeLabels.forEach { (type, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onTypeSelected(type)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurrenceIntervalRow(interval: Int, type: RecurrenceType, onIntervalChanged: (Int) -> Unit) {
    val unitLabel = when (type) {
        RecurrenceType.DAILY -> stringResource(R.string.recurrence_interval_days)
        RecurrenceType.WEEKLY -> stringResource(R.string.recurrence_interval_weeks)
        RecurrenceType.MONTHLY -> stringResource(R.string.recurrence_interval_months)
        RecurrenceType.YEARLY -> stringResource(R.string.recurrence_interval_years)
        RecurrenceType.NONE -> ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.label_recurrence_interval),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = interval.toString(),
            onValueChange = { text ->
                val parsed = text.filter { it.isDigit() }.take(2).toIntOrNull()
                if (parsed != null && parsed in 1..99) {
                    onIntervalChanged(parsed)
                } else if (text.isEmpty()) {
                    onIntervalChanged(1)
                }
            },
            modifier = Modifier.width(72.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Text(
            text = unitLabel,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DaysOfWeekSelector(selectedDays: Set<DayOfWeek>, onDayToggled: (DayOfWeek) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.label_recurrence_days_of_week),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val dayLabels = listOf(
                DayOfWeek.MONDAY to stringResource(R.string.day_mon),
                DayOfWeek.TUESDAY to stringResource(R.string.day_tue),
                DayOfWeek.WEDNESDAY to stringResource(R.string.day_wed),
                DayOfWeek.THURSDAY to stringResource(R.string.day_thu),
                DayOfWeek.FRIDAY to stringResource(R.string.day_fri),
                DayOfWeek.SATURDAY to stringResource(R.string.day_sat),
                DayOfWeek.SUNDAY to stringResource(R.string.day_sun),
            )
            dayLabels.forEach { (day, label) ->
                FilterChip(
                    selected = day in selectedDays,
                    onClick = { onDayToggled(day) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RecurrenceEndDateField(endDate: Long?, onEndDateChanged: (Long?) -> Unit) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    if (endDate != null) {
        val dateText = formatDate(endDate, "MMM d, yyyy")
        val chipDesc = stringResource(R.string.label_recurrence_end_date)
        AssistChip(
            onClick = { showDatePicker = true },
            label = { Text("$chipDesc: $dateText") },
            trailingIcon = {
                IconButton(
                    onClick = { onEndDateChanged(null) },
                    modifier = Modifier.size(18.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_remove_recurrence_end_date),
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            modifier = Modifier.semantics {
                contentDescription = "$chipDesc: $dateText"
            },
        )
    } else {
        TextButton(onClick = { showDatePicker = true }) {
            Text(stringResource(R.string.label_recurrence_end_date))
        }
    }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        if (endDate != null) {
            calendar.timeInMillis = endDate
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onEndDateChanged(selectedCalendar.timeInMillis)
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }
}
