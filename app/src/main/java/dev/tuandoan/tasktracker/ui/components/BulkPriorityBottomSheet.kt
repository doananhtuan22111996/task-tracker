package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.model.Priority
import dev.tuandoan.tasktracker.ui.theme.AppSpacing

/**
 * Bottom sheet that offers the three priority levels (LOW / MEDIUM / HIGH) for a bulk apply
 * operation. Dismissing without selecting is a no-op.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkPriorityBottomSheet(selectedCount: Int, onPrioritySelected: (Int) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding),
        ) {
            Text(
                text = stringResource(R.string.title_change_priority_for_count, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = AppSpacing.medium),
            )

            Priority.entries.forEach { option ->
                val label = when (option) {
                    Priority.LOW -> stringResource(R.string.priority_low)
                    Priority.MEDIUM -> stringResource(R.string.priority_medium)
                    Priority.HIGH -> stringResource(R.string.priority_high)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onPrioritySelected(option.value) }
                        .padding(vertical = AppSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.medium))
        }
    }
}
