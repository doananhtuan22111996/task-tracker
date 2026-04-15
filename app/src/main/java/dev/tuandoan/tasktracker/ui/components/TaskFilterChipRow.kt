package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFilterChipRow(currentFilter: TaskFilter, onFilterChange: (TaskFilter) -> Unit, modifier: Modifier = Modifier) {
    val filters = listOf(
        TaskFilter.ALL to stringResource(R.string.nav_all),
        TaskFilter.ACTIVE to stringResource(R.string.nav_active),
        TaskFilter.COMPLETED to stringResource(R.string.nav_completed),
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding)
            .padding(top = AppSpacing.small, bottom = AppSpacing.extraSmall),
    ) {
        filters.forEachIndexed { index, (filter, label) ->
            SegmentedButton(
                selected = currentFilter == filter,
                onClick = { onFilterChange(filter) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = filters.size,
                    baseShape = RoundedCornerShape(12.dp),
                ),
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}
