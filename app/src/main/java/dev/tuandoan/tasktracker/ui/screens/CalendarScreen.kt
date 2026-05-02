package dev.tuandoan.tasktracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.ui.components.CalendarMonthView
import dev.tuandoan.tasktracker.ui.components.DayAgendaSheet
import dev.tuandoan.tasktracker.ui.theme.AppSpacing
import dev.tuandoan.tasktracker.ui.viewmodel.CalendarViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * v1.11.0 Calendar screen. Hosts the [CalendarMonthView] (CAL-11/12) with a top bar that
 * shows the visible-month title and provides chevron navigation + a Today action (CAL-14).
 * Horizontal swipe paging is handled inside [CalendarMonthView] (CAL-15). Tapping a day
 * opens a [DayAgendaSheet] showing that day's tasks (CAL-17). The empty-state hint card
 * for zero dated tasks (CAL-16) and full-featured agenda rows (CAL-18..22) land later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToEditor: (Long) -> Unit,
    bottomBarPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isAgendaOpen by rememberSaveable { mutableStateOf(false) }

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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = bottomBarPadding)
                .padding(horizontal = AppSpacing.medium),
        ) {
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
                tasks = uiState.selectedDayTasks,
                subtaskProgress = uiState.subtaskProgress,
                onTaskClick = { taskId ->
                    isAgendaOpen = false
                    onNavigateToEditor(taskId)
                },
                onToggleComplete = viewModel::onToggleTaskComplete,
                onArchive = viewModel::onArchiveTask,
                onTogglePin = viewModel::onTogglePin,
                onDismiss = { isAgendaOpen = false },
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
        title = { Text(monthTitle) },
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
