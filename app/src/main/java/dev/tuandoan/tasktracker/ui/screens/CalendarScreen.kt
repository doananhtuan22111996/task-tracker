package dev.tuandoan.tasktracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.diagnostics.PerformanceTrace
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import dev.tuandoan.tasktracker.ui.components.CalendarEmptyStateCard
import dev.tuandoan.tasktracker.ui.components.CalendarMonthView
import dev.tuandoan.tasktracker.ui.components.DayAgendaSheet
import dev.tuandoan.tasktracker.ui.events.UiEvent
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.ui.viewmodel.CalendarViewModel
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * v1.11.0 Calendar screen. Hosts the [CalendarMonthView] (CAL-11/12) with a top bar that
 * shows the visible-month title and provides chevron navigation + a Today action (CAL-14).
 * Horizontal swipe paging is handled inside [CalendarMonthView] (CAL-15). Tapping a day
 * opens a [DayAgendaSheet] showing that day's tasks, rendered with the full [TaskItem]
 * composable (CAL-17 + CAL-18). The sheet's FAB opens the task editor with `dueDate`
 * prefilled to the selected day (CAL-19). A [CalendarEmptyStateCard] sits above the grid
 * when the database holds zero dated tasks (CAL-16). Swipe/multi-select (CAL-20/21) land
 * in follow-up tickets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToCreateForDay: (Long) -> Unit,
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
    // Cache the zone so today's-epoch lookups inside click handlers don't re-read the
    // system default on every invocation. Matches the `zone` cache in `CalendarViewModel`.
    val zone = remember { ZoneId.systemDefault() }
    val showEmptyStateHint = !uiState.hasAnyDatedTask

    LaunchedEffect(viewModel) {
        viewModel.bulkUiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.ShowUndoDelete -> event.message?.let { snackbarHostState.showSnackbar(it) }
                else -> Unit
            }
        }
    }

    BackHandler(enabled = isSelectionMode && isAgendaOpen) {
        viewModel.clearAgendaSelection()
    }

    // FB-16: `calendar_month_render` Performance trace. Measures
    // `visibleMonth` change → decorations Flow re-emits for the new month + one paint
    // frame, which is a meaningful "data ready and rendered" signal for chevron, swipe,
    // and today-click paths. See [rememberMonthRenderTrace] KDoc for why the readiness
    // signal is decorations-identity rather than `Modifier.onGloballyPositioned`.
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
            CalendarTopBar(
                visibleMonth = uiState.visibleMonth,
                onPrevMonth = { viewModel.onMonthChange(-1) },
                onNextMonth = { viewModel.onMonthChange(1) },
                onTodayClick = viewModel::onTodayClick,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = bottomBarPadding)
                .padding(horizontal = AppSpacing.medium),
        ) {
            if (showEmptyStateHint) {
                // CAL-16: hint card sits above the grid when the database holds zero
                // dated tasks. Adding a due date to any task — through either the CTA
                // or the normal editor — auto-dismisses it.
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
                onDayClick = { date ->
                    viewModel.onDaySelect(date)
                    isAgendaOpen = true
                },
                onJumpToMonth = viewModel::onJumpToMonth,
            )
        }

        if (isAgendaOpen) {
            DayAgendaSheet(
                selectedDay = uiState.selectedDay,
                items = uiState.agendaItems,
                subtaskProgress = uiState.subtaskProgress,
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
                onDismiss = {
                    viewModel.clearAgendaSelection()
                    isAgendaOpen = false
                },
                isSelectionMode = isSelectionMode,
                selectedIds = selectedIds,
                selectedCount = selectedCount,
                onLongPressTask = viewModel::onAgendaLongPress,
                onToggleSelection = viewModel::onAgendaToggleSelection,
                onBulkComplete = viewModel::agendaBulkComplete,
                onBulkArchive = viewModel::agendaBulkArchive,
                onBulkDelete = viewModel::agendaBulkDelete,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    visibleMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
) {
    val monthTitle = visibleMonth.format(monthTitleFormatter(Locale.getDefault()))
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onPrevMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_calendar_prev_month),
                )
            }
        },
        title = {
            // CAL-29: TalkBack announces the new month title on every change, without stealing
            // focus. `heading()` marks it as a section header so users can navigate headings.
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
 *
 * **Why we don't use `Modifier.onGloballyPositioned` to stop the trace:**
 * Kizitonwose's `HorizontalCalendar` keeps the same outer container coordinates across
 * month changes (only inner cell content swaps), so a wrapper `onGloballyPositioned`
 * doesn't fire for non-initial month changes. The decorations Flow is the more
 * reliable readiness signal — `CalendarViewModel.decorationsFlow` is a `flatMapLatest`
 * keyed on `_visibleMonth`, so a new emission is observable evidence that the new
 * month's data is ready and the next composition tick will lay it out.
 *
 * **Why the initial composition does NOT start a trace:**
 * On first open, the decorations for the initial `visibleMonth` are already loaded
 * (or arrive concurrently with the LaunchedEffect's first run), so there is no
 * "next" emission to settle on. Cold-start render is FR-17 territory (`_app_start`);
 * FR-18's `calendar_month_render` is for *month changes after first composition*.
 *
 * **Lifecycle concerns wrapped here:**
 *  1. **Skip first composition.** The initial run only seeds the holder for tracking;
 *     no trace is started for the seeded `visibleMonth`.
 *  2. **Start on each subsequent `visibleMonth` change.** A [LaunchedEffect] keyed on
 *     `visibleMonth` starts a fresh trace whenever the visible month changes after
 *     first composition (chevron tap, swipe, today-click, saved-state restore).
 *  3. **Stop after data settles + one paint frame.** The effect waits for the
 *     [decorations] reference to change (the Flow's next emission is a different
 *     `Map` instance for the new month) via [snapshotFlow], then awaits one
 *     [withFrameNanos] tick so layout/paint of the new content is included.
 *  4. **Race-tolerance.** A second `visibleMonth` change before the first trace
 *     stops cancels the prior [LaunchedEffect]; the new one starts by stopping any
 *     leftover trace so we never have two running concurrently.
 *  5. **Cleanup on dispose.** A [DisposableEffect] stops any pending trace if the
 *     screen leaves composition before settle (e.g. user switches tabs mid-load),
 *     preventing orphan traces in the SDK.
 *
 * All access to the trace slot is on the main thread (LaunchedEffect, onDispose).
 */
@Composable
private fun rememberMonthRenderTrace(
    visibleMonth: YearMonth,
    decorations: Map<LocalDate, DayDecoration>,
    startTrace: () -> PerformanceTrace,
) {
    val holder = remember { PendingTraceHolder() }
    // Track the latest decorations reference in a Compose state slot so a snapshotFlow
    // can observe changes from inside the LaunchedEffect. Writing inside SideEffect
    // (not the composition body) keeps this side-effect-free at compose time.
    val decorationsState = remember { mutableStateOf(decorations) }
    SideEffect { decorationsState.value = decorations }

    // Skip the initial composition: there is no "next" decorations emission to settle
    // on for the seeded visibleMonth. See KDoc point (1).
    val isInitialRun = remember { mutableStateOf(true) }

    LaunchedEffect(visibleMonth) {
        if (isInitialRun.value) {
            isInitialRun.value = false
            return@LaunchedEffect
        }
        // Cancel any leftover trace from a rapid prior month change.
        holder.stopIfPending()
        // Capture the baseline reference before starting; the next emission (different
        // Map instance from the Flow's flatMapLatest re-fetch) is the readiness signal.
        val baseline = decorationsState.value
        holder.start(startTrace())
        snapshotFlow { decorationsState.value }
            .first { it !== baseline }
        // One frame after data settles to include the new month's layout/paint.
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
