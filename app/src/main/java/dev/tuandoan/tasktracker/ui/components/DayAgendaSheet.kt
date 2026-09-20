package dev.tuandoan.tasktracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.tuandoan.tasktracker.data.database.SubtaskProgress
import dev.tuandoan.tasktracker.domain.model.AgendaItem
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import java.time.LocalDate

/**
 * Day agenda bottom sheet (CAL-17 + CAL-18 + CAL-19 + CAL-24 + CAL-25.2).
 * Thin wrapper around [DayAgendaContent] inside [ModalBottomSheet].
 * Supports hosting an optional [SnackbarHostState] (CAL-21.2) so undo snackbars
 * appear visibly on top of the bottom sheet without being obscured by the scrim.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAgendaSheet(
    selectedDay: LocalDate,
    items: List<AgendaItem>,
    subtaskProgress: Map<Long, SubtaskProgress>,
    onItemClick: (AgendaItem) -> Unit,
    onToggleComplete: (AgendaItem) -> Unit,
    onArchive: (AgendaItem) -> Unit,
    onTogglePin: (AgendaItem) -> Unit,
    onAddTaskClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    selectedCount: Int = 0,
    onLongPressTask: (Long) -> Unit = {},
    onToggleSelection: (Long) -> Unit = {},
    onBulkComplete: () -> Unit = {},
    onBulkArchive: () -> Unit = {},
    onBulkDelete: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            DayAgendaContent(
                selectedDay = selectedDay,
                items = items,
                subtaskProgress = subtaskProgress,
                onItemClick = onItemClick,
                onToggleComplete = onToggleComplete,
                onArchive = onArchive,
                onTogglePin = onTogglePin,
                onAddTaskClick = onAddTaskClick,
                isTwoPane = false,
                isSelectionMode = isSelectionMode,
                selectedIds = selectedIds,
                selectedCount = selectedCount,
                onLongPressTask = onLongPressTask,
                onToggleSelection = onToggleSelection,
                onBulkComplete = onBulkComplete,
                onBulkArchive = onBulkArchive,
                onBulkDelete = onBulkDelete,
            )

            if (snackbarHostState != null) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = AppSpacing.large),
                )
            }
        }
    }
}
