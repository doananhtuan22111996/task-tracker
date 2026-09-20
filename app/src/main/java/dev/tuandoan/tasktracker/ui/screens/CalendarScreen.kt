package dev.tuandoan.tasktracker.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.diagnostics.PerformanceTrace
import dev.tuandoan.tasktracker.domain.model.AgendaItem
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import dev.tuandoan.tasktracker.ui.components.CalendarEmptyStateCard
import dev.tuandoan.tasktracker.ui.components.CalendarMonthView
import dev.tuandoan.tasktracker.ui.components.DayAgendaContent
import dev.tuandoan.tasktracker.ui.components.DayAgendaSheet
import dev.tuandoan.tasktracker.ui.events.UiEvent
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.ui.viewmodel.CalendarUiState
import dev.tuandoan.tasktracker.ui.viewmodel.CalendarViewModel
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * v1.14.0 Calendar screen.
 * Supports adaptive layouts (CAL-25):
 * - Phone / portrait: Month grid with modal bottom sheet [DayAgendaSheet].
 * - Tablet landscape / expanded width: Two-pane layout with month view (50%) and inline [DayAgendaContent] (50%).
 * Supports undo for agenda task archive (CAL-21).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToCreateForDay: (Long) -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    bottomBarPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val pendingBulkArchiveTasks by viewModel.pendingBulkArchiveTasks.collectAsStateWithLifecycle()
    val pendingBulkDeleteTasks by viewModel.pendingBulkDeleteTasks.collectAsStateWithLifecycle()
    var isAgendaOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val zone = remember { ZoneId.systemDefault() }
    val showEmptyStateHint = !uiState.hasAnyDatedTask
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTwoPane = windowWidthSizeClass == WindowWidthSizeClass.Expanded ||
        (windowWidthSizeClass == WindowWidthSizeClass.Medium && isLandscape)

    LaunchedEffect(viewModel) {
        viewModel.agendaUiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    if (event.actionLabel != null) {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            event.onActionClick()
                        }
                    } else {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
                is UiEvent.ShowUndoDelete -> {
                    val taskCount = event.tasks.size
                    val message = event.message ?: if (taskCount == 1) {
                        context.getString(R.string.snackbar_task_deleted)
                    } else {
                        context.getString(R.string.snackbar_tasks_deleted, taskCount)
                    }

                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = context.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onUndo()
                    }
                }
                is UiEvent.ShowUndoArchive -> {
                    val taskCount = event.tasks.size
                    val message = event.message ?: if (taskCount == 1) {
                        context.getString(R.string.snackbar_task_archived)
                    } else {
                        context.getString(R.string.snackbar_tasks_archived, taskCount)
                    }

                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = context.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onUndo()
                    }
                }
                else -> Unit
            }
        }
    }

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearAgendaSelection()
    }

    // FB-16: `calendar_month_render` Performance trace.
    rememberMonthRenderTrace(
        visibleMonth = uiState.visibleMonth,
        decorations = uiState.decorations,
        startTrace = viewModel::startMonthRenderTrace,
    )

    // Bulk archive confirmation dialog
    if (pendingBulkArchiveTasks.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = viewModel::cancelAgendaBulkArchive,
            title = { Text(stringResource(R.string.dialog_archive_tasks_title)) },
            text = { Text(stringResource(R.string.dialog_archive_tasks_message, pendingBulkArchiveTasks.size)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmAgendaBulkArchive) {
                    Text(stringResource(R.string.action_archive))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelAgendaBulkArchive) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Bulk delete confirmation dialog
    if (pendingBulkDeleteTasks.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = viewModel::cancelAgendaBulkDelete,
            title = { Text(stringResource(R.string.dialog_delete_tasks_title)) },
            text = { Text(stringResource(R.string.dialog_delete_tasks_message, pendingBulkDeleteTasks.size)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmAgendaBulkDelete) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelAgendaBulkDelete) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!isTwoPane) {
                CalendarTopBar(
                    visibleMonth = uiState.visibleMonth,
                    onPrevMonth = { viewModel.onMonthChange(-1) },
                    onNextMonth = { viewModel.onMonthChange(1) },
                    onTodayClick = viewModel::onTodayClick,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (isTwoPane) {
            TwoPaneCalendarContent(
                uiState = uiState,
                showEmptyStateHint = showEmptyStateHint,
                isSelectionMode = isSelectionMode,
                selectedIds = selectedIds,
                selectedCount = selectedCount,
                bottomBarPadding = bottomBarPadding,
                paddingValues = paddingValues,
                zone = zone,
                onPrevMonth = { viewModel.onMonthChange(-1) },
                onNextMonth = { viewModel.onMonthChange(1) },
                onTodayClick = viewModel::onTodayClick,
                onDaySelect = viewModel::onDaySelect,
                onJumpToMonth = viewModel::onJumpToMonth,
                onItemClick = { item -> viewModel.onAgendaItemClick(item, onNavigateToEditor) },
                onToggleComplete = viewModel::onAgendaItemToggleComplete,
                onArchive = viewModel::onAgendaItemArchive,
                onTogglePin = viewModel::onAgendaItemTogglePin,
                onAddTaskClick = {
                    val epoch = uiState.selectedDay
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()
                    onNavigateToCreateForDay(epoch)
                },
                onLongPressTask = viewModel::onAgendaLongPress,
                onToggleSelection = viewModel::onAgendaToggleSelection,
                onBulkComplete = viewModel::agendaBulkComplete,
                onBulkArchive = viewModel::agendaBulkArchive,
                onBulkDelete = viewModel::agendaBulkDelete,
                onNavigateToCreateForDay = onNavigateToCreateForDay,
            )
        } else {
            SinglePaneCalendarContent(
                uiState = uiState,
                showEmptyStateHint = showEmptyStateHint,
                isSelectionMode = isSelectionMode,
                selectedIds = selectedIds,
                selectedCount = selectedCount,
                isAgendaOpen = isAgendaOpen,
                snackbarHostState = snackbarHostState,
                bottomBarPadding = bottomBarPadding,
                paddingValues = paddingValues,
                zone = zone,
                onDayClick = { date ->
                    viewModel.onDaySelect(date)
                    isAgendaOpen = true
                },
                onJumpToMonth = viewModel::onJumpToMonth,
                onItemClick = { item ->
                    if (!isSelectionMode) {
                        isAgendaOpen = false
                        viewModel.onAgendaItemClick(item, onNavigateToEditor)
                    }
                },
                onToggleComplete = viewModel::onAgendaItemToggleComplete,
                onArchive = viewModel::onAgendaItemArchive,
                onTogglePin = viewModel::onAgendaItemTogglePin,
                onAddTaskClick = {
                    isAgendaOpen = false
                    val epoch = uiState.selectedDay
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()
                    onNavigateToCreateForDay(epoch)
                },
                onDismissAgenda = {
                    viewModel.clearAgendaSelection()
                    isAgendaOpen = false
                },
                onLongPressTask = viewModel::onAgendaLongPress,
                onToggleSelection = viewModel::onAgendaToggleSelection,
                onBulkComplete = viewModel::agendaBulkComplete,
                onBulkArchive = viewModel::agendaBulkArchive,
                onBulkDelete = viewModel::agendaBulkDelete,
                onNavigateToCreateForDay = onNavigateToCreateForDay,
            )
        }
    }
}

@Composable
private fun TwoPaneCalendarContent(
    uiState: CalendarUiState,
    showEmptyStateHint: Boolean,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    selectedCount: Int,
    bottomBarPadding: Dp,
    paddingValues: PaddingValues,
    zone: ZoneId,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onDaySelect: (LocalDate) -> Unit,
    onJumpToMonth: (YearMonth) -> Unit,
    onItemClick: (AgendaItem) -> Unit,
    onToggleComplete: (AgendaItem) -> Unit,
    onArchive: (AgendaItem) -> Unit,
    onTogglePin: (AgendaItem) -> Unit,
    onAddTaskClick: () -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onBulkComplete: () -> Unit,
    onBulkArchive: () -> Unit,
    onBulkDelete: () -> Unit,
    onNavigateToCreateForDay: (Long) -> Unit,
) {
    val monthPaneTitle = stringResource(R.string.cd_calendar_month_pane)
    val agendaPaneTitle = stringResource(R.string.cd_calendar_agenda_pane)
    val isSelectedDayInVisibleMonth = YearMonth.from(uiState.selectedDay) == uiState.visibleMonth

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(bottom = bottomBarPadding),
    ) {
        // Left Pane (50%): Month Navigation + Month View
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = AppSpacing.medium)
                .semantics { paneTitle = monthPaneTitle },
        ) {
            CalendarTopBar(
                visibleMonth = uiState.visibleMonth,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onTodayClick = onTodayClick,
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
            if (showEmptyStateHint) {
                CalendarEmptyStateCard(
                    onAddTaskClick = {
                        val epoch = LocalDate.now(zone)
                            .atStartOfDay(zone)
                            .toInstant()
                            .toEpochMilli()
                        onNavigateToCreateForDay(epoch)
                    },
                    modifier = Modifier.padding(bottom = AppSpacing.medium),
                )
            }
            CalendarMonthView(
                visibleMonth = uiState.visibleMonth,
                selectedDay = uiState.selectedDay,
                decorations = uiState.decorations,
                onDayClick = onDaySelect,
                onJumpToMonth = onJumpToMonth,
            )
        }

        VerticalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxHeight(),
        )

        // Right Pane (50%): Day Agenda Content
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .semantics { paneTitle = agendaPaneTitle },
            color = MaterialTheme.colorScheme.surface,
        ) {
            if (isSelectedDayInVisibleMonth) {
                DayAgendaContent(
                    selectedDay = uiState.selectedDay,
                    items = uiState.agendaItems,
                    subtaskProgress = uiState.subtaskProgress,
                    onItemClick = onItemClick,
                    onToggleComplete = onToggleComplete,
                    onArchive = onArchive,
                    onTogglePin = onTogglePin,
                    onAddTaskClick = onAddTaskClick,
                    modifier = Modifier.fillMaxSize(),
                    isTwoPane = true,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    selectedCount = selectedCount,
                    onLongPressTask = onLongPressTask,
                    onToggleSelection = onToggleSelection,
                    onBulkComplete = onBulkComplete,
                    onBulkArchive = onBulkArchive,
                    onBulkDelete = onBulkDelete,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.screenPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(AppSpacing.medium))
                    Text(
                        text = stringResource(R.string.calendar_select_day_prompt),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SinglePaneCalendarContent(
    uiState: CalendarUiState,
    showEmptyStateHint: Boolean,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    selectedCount: Int,
    isAgendaOpen: Boolean,
    snackbarHostState: SnackbarHostState,
    bottomBarPadding: Dp,
    paddingValues: PaddingValues,
    zone: ZoneId,
    onDayClick: (LocalDate) -> Unit,
    onJumpToMonth: (YearMonth) -> Unit,
    onItemClick: (AgendaItem) -> Unit,
    onToggleComplete: (AgendaItem) -> Unit,
    onArchive: (AgendaItem) -> Unit,
    onTogglePin: (AgendaItem) -> Unit,
    onAddTaskClick: () -> Unit,
    onDismissAgenda: () -> Unit,
    onLongPressTask: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onBulkComplete: () -> Unit,
    onBulkArchive: () -> Unit,
    onBulkDelete: () -> Unit,
    onNavigateToCreateForDay: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(bottom = bottomBarPadding)
            .padding(horizontal = AppSpacing.medium),
    ) {
        if (showEmptyStateHint) {
            CalendarEmptyStateCard(
                onAddTaskClick = {
                    val epoch = LocalDate.now(zone)
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()
                    onNavigateToCreateForDay(epoch)
                },
                modifier = Modifier.padding(bottom = AppSpacing.medium),
            )
        }
        CalendarMonthView(
            visibleMonth = uiState.visibleMonth,
            selectedDay = uiState.selectedDay,
            decorations = uiState.decorations,
            onDayClick = onDayClick,
            onJumpToMonth = onJumpToMonth,
        )
    }

    if (isAgendaOpen) {
        DayAgendaSheet(
            selectedDay = uiState.selectedDay,
            items = uiState.agendaItems,
            subtaskProgress = uiState.subtaskProgress,
            onItemClick = onItemClick,
            onToggleComplete = onToggleComplete,
            onArchive = onArchive,
            onTogglePin = onTogglePin,
            onAddTaskClick = onAddTaskClick,
            onDismiss = onDismissAgenda,
            snackbarHostState = snackbarHostState,
            isSelectionMode = isSelectionMode,
            selectedIds = selectedIds,
            selectedCount = selectedCount,
            onLongPressTask = onLongPressTask,
            onToggleSelection = onToggleSelection,
            onBulkComplete = onBulkComplete,
            onBulkArchive = onBulkArchive,
            onBulkDelete = onBulkDelete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    visibleMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    val monthTitle = visibleMonth.format(monthTitleFormatter(Locale.getDefault()))
    TopAppBar(
        windowInsets = windowInsets,
        navigationIcon = {
            IconButton(onClick = onPrevMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_calendar_prev_month),
                )
            }
        },
        title = {
            Text(
                text = monthTitle,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    heading()
                },
            )
        },
        actions = {
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_calendar_next_month),
                )
            }
            TextButton(onClick = onTodayClick) {
                Text(stringResource(R.string.action_calendar_today))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

// Locale-aware "Month yyyy" formatter. `LLLL` is the standalone-month variant so locales like
// Russian render nominative-case month names instead of the genitive form used by `MMMM` in
// full-date contexts.
private fun monthTitleFormatter(locale: Locale): DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", locale)

/**
 * Drives the `calendar_month_render` Firebase Performance trace (FB-16).
 */
@Composable
private fun rememberMonthRenderTrace(
    visibleMonth: YearMonth,
    decorations: Map<LocalDate, DayDecoration>,
    startTrace: () -> PerformanceTrace,
) {
    val holder = remember { PendingTraceHolder() }
    val decorationsState = remember { mutableStateOf(decorations) }
    SideEffect { decorationsState.value = decorations }

    val isInitialRun = remember { mutableStateOf(true) }

    LaunchedEffect(visibleMonth) {
        if (isInitialRun.value) {
            isInitialRun.value = false
            return@LaunchedEffect
        }
        holder.stopIfPending()
        val baseline = decorationsState.value
        holder.start(startTrace())
        snapshotFlow { decorationsState.value }
            .first { it !== baseline }
        withFrameNanos { }
        holder.stopIfPending()
    }
    DisposableEffect(Unit) {
        onDispose { holder.stopIfPending() }
    }
}

private class PendingTraceHolder {
    private var pending: PerformanceTrace? = null

    fun start(trace: PerformanceTrace) {
        pending = trace
    }

    fun stopIfPending() {
        pending?.let {
            it.stop()
            pending = null
        }
    }
}
