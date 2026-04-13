package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.model.CompletedGrouping
import dev.tuandoan.tasktracker.domain.model.SortDirection
import dev.tuandoan.tasktracker.domain.model.SortKey
import dev.tuandoan.tasktracker.domain.model.TaskSort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: TaskSort,
    sortOptions: List<TaskSort>,
    onSortSelected: (TaskSort) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val completedLast = currentSort.completedGrouping == CompletedGrouping.COMPLETED_LAST

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.sort_header),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            sortOptions.forEach { option ->
                val isSelected = option.key == currentSort.key && option.direction == currentSort.direction
                val label = getSortLabel(option)
                val selectedLabel = if (isSelected) {
                    stringResource(R.string.cd_day_selected)
                } else {
                    stringResource(R.string.cd_day_not_selected)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) {
                            onSortSelected(
                                currentSort.copy(key = option.key, direction = option.direction),
                            )
                        }
                        .padding(vertical = 8.dp)
                        .semantics {
                            contentDescription = label
                            stateDescription = selectedLabel
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Switch) {
                        val newGrouping = if (completedLast) {
                            CompletedGrouping.NONE
                        } else {
                            CompletedGrouping.COMPLETED_LAST
                        }
                        onSortSelected(currentSort.copy(completedGrouping = newGrouping))
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sort_completed_last),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = completedLast,
                    onCheckedChange = null,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun getSortLabel(sort: TaskSort): String = when (sort.key) {
    SortKey.DUE_DATE -> stringResource(R.string.sort_due_date_earliest)
    SortKey.CREATED_AT -> when (sort.direction) {
        SortDirection.DESC -> stringResource(R.string.sort_created_newest)
        SortDirection.ASC -> stringResource(R.string.sort_created_oldest)
    }
    SortKey.TITLE -> stringResource(R.string.sort_title_az)
    SortKey.PRIORITY -> stringResource(R.string.sort_priority_high_low)
}
