package dev.tuandoan.tasktracker.ui.components

import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.navigation.TaskTrackerRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavBarTest {

    // === Tab Configuration ===

    @Test
    fun `BottomNavTab has exactly 3 entries`() {
        assertEquals(3, BottomNavTab.entries.size)
    }

    @Test
    fun `TASKS tab has correct route`() {
        assertEquals(TaskTrackerRoutes.TASK_LIST_BASE, BottomNavTab.TASKS.route)
    }

    @Test
    fun `STATS tab has correct route`() {
        assertEquals(TaskTrackerRoutes.STATS, BottomNavTab.STATS.route)
    }

    @Test
    fun `ARCHIVED tab has correct route`() {
        assertEquals(TaskTrackerRoutes.ARCHIVED, BottomNavTab.ARCHIVED.route)
    }

    @Test
    fun `TASKS tab has correct label resource`() {
        assertEquals(R.string.nav_tab_tasks, BottomNavTab.TASKS.labelResId)
    }

    @Test
    fun `STATS tab has correct label resource`() {
        assertEquals(R.string.nav_tab_stats, BottomNavTab.STATS.labelResId)
    }

    @Test
    fun `ARCHIVED tab has correct label resource`() {
        assertEquals(R.string.nav_tab_archived, BottomNavTab.ARCHIVED.labelResId)
    }

    // === Tab Order ===

    @Test
    fun `tab order is Tasks, Stats, Archived`() {
        val tabs = BottomNavTab.entries
        assertEquals(BottomNavTab.TASKS, tabs[0])
        assertEquals(BottomNavTab.STATS, tabs[1])
        assertEquals(BottomNavTab.ARCHIVED, tabs[2])
    }

    // === Icon Configuration ===

    @Test
    fun `each tab has different selected and unselected icons`() {
        BottomNavTab.entries.forEach { tab ->
            assertTrue(
                "Tab ${tab.name} should have different selected/unselected icons",
                tab.selectedIcon != tab.unselectedIcon,
            )
        }
    }

    // === Route Matching ===

    @Test
    fun `task_list route matches TASKS tab`() {
        val route = "task_list?statsFilter={statsFilter}"
        assertTrue(route.startsWith(BottomNavTab.TASKS.route))
    }

    @Test
    fun `task_list with filter argument matches TASKS tab`() {
        val route = "task_list?statsFilter=ACTIVE"
        assertTrue(route.startsWith(BottomNavTab.TASKS.route))
    }

    @Test
    fun `stats route matches STATS tab`() {
        val route = "stats"
        assertTrue(route.startsWith(BottomNavTab.STATS.route))
    }

    @Test
    fun `archived route matches ARCHIVED tab`() {
        val route = "archived"
        assertTrue(route.startsWith(BottomNavTab.ARCHIVED.route))
    }

    @Test
    fun `settings route does not match any tab`() {
        val route = "settings"
        BottomNavTab.entries.forEach { tab ->
            assertFalse(
                "Settings route should not match ${tab.name} tab",
                route.startsWith(tab.route),
            )
        }
    }

    @Test
    fun `task_editor route does not match any tab`() {
        val route = "task_editor"
        BottomNavTab.entries.forEach { tab ->
            assertFalse(
                "Task editor route should not match ${tab.name} tab",
                route.startsWith(tab.route),
            )
        }
    }

    @Test
    fun `onboarding route does not match any tab`() {
        val route = "onboarding"
        BottomNavTab.entries.forEach { tab ->
            assertFalse(
                "Onboarding route should not match ${tab.name} tab",
                route.startsWith(tab.route),
            )
        }
    }

    @Test
    fun `help route does not match any tab`() {
        val route = "help"
        BottomNavTab.entries.forEach { tab ->
            assertFalse(
                "Help route should not match ${tab.name} tab",
                route.startsWith(tab.route),
            )
        }
    }

    // === Null Route Handling ===

    @Test
    fun `null route does not match any tab`() {
        val route: String? = null
        BottomNavTab.entries.forEach { tab ->
            assertFalse(
                "Null route should not match ${tab.name} tab",
                route?.startsWith(tab.route) == true,
            )
        }
    }
}
