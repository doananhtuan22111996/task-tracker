package dev.tuandoan.tasktracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.navigation.TaskTrackerRoutes

enum class BottomNavTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    TASKS(
        route = TaskTrackerRoutes.TASK_LIST_BASE,
        labelResId = R.string.nav_tab_tasks,
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle,
    ),
    CALENDAR(
        route = TaskTrackerRoutes.CALENDAR,
        labelResId = R.string.nav_tab_calendar,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
    ),
    STATS(
        route = TaskTrackerRoutes.STATS,
        labelResId = R.string.nav_tab_stats,
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
    ),
    ARCHIVED(
        route = TaskTrackerRoutes.ARCHIVED,
        labelResId = R.string.nav_tab_archived,
        selectedIcon = Icons.Filled.Archive,
        unselectedIcon = Icons.Outlined.Archive,
    ),
}

@Composable
fun BottomNavBar(currentRoute: String?, onTabSelected: (BottomNavTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = currentRoute?.startsWith(tab.route) == true
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelResId),
                    )
                },
                label = {
                    Text(text = stringResource(tab.labelResId))
                },
                selected = selected,
                onClick = { onTabSelected(tab) },
            )
        }
    }
}
