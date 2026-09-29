package dev.tuandoan.tasktracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskItemA11yTest {

    @Test
    fun `buildSubtaskExpandA11yDescription formats progress with expand action`() {
        val result = buildSubtaskExpandA11yDescription(
            progressDescription = "2 of 4 subtasks completed",
            expandCollapseDescription = "Expand subtasks",
        )
        assertEquals("2 of 4 subtasks completed, Expand subtasks", result)
    }

    @Test
    fun `buildSubtaskExpandA11yDescription formats progress with collapse action`() {
        val result = buildSubtaskExpandA11yDescription(
            progressDescription = "4 of 4 subtasks completed",
            expandCollapseDescription = "Collapse subtasks",
        )
        assertEquals("4 of 4 subtasks completed, Collapse subtasks", result)
    }
}
