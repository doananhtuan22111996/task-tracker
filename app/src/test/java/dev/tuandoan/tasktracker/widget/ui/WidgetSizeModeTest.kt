package dev.tuandoan.tasktracker.widget.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizeModeTest {

    @Test
    fun `2x2 size resolves to COMPACT_BADGE with 1 row`() {
        val spec = WidgetSizeResolver.resolve(DpSize(110.dp, 110.dp))
        assertEquals(WidgetLayoutMode.COMPACT_BADGE, spec.mode)
        assertEquals(1, spec.rowCount)
    }

    @Test
    fun `4x2 size resolves to LIST with 5 rows`() {
        val spec = WidgetSizeResolver.resolve(DpSize(260.dp, 110.dp))
        assertEquals(WidgetLayoutMode.LIST, spec.mode)
        assertEquals(5, spec.rowCount)
    }

    @Test
    fun `4x4 size resolves to LIST_WITH_OVERDUE with 10 rows`() {
        val spec = WidgetSizeResolver.resolve(DpSize(260.dp, 260.dp))
        assertEquals(WidgetLayoutMode.LIST_WITH_OVERDUE, spec.mode)
        assertEquals(10, spec.rowCount)
    }

    @Test
    fun `intermediate width above threshold resolves to LIST not COMPACT_BADGE`() {
        val spec = WidgetSizeResolver.resolve(DpSize(220.dp, 120.dp))
        assertEquals(WidgetLayoutMode.LIST, spec.mode)
    }

    @Test
    fun `wide but short widget is LIST not LIST_WITH_OVERDUE`() {
        val spec = WidgetSizeResolver.resolve(DpSize(300.dp, 150.dp))
        assertEquals(WidgetLayoutMode.LIST, spec.mode)
    }

    @Test
    fun `tall but narrow widget stays COMPACT_BADGE because width dominates`() {
        val spec = WidgetSizeResolver.resolve(DpSize(150.dp, 260.dp))
        assertEquals(WidgetLayoutMode.COMPACT_BADGE, spec.mode)
    }

    @Test
    fun `width exactly at threshold takes the larger branch`() {
        val spec = WidgetSizeResolver.resolve(DpSize(200.dp, 110.dp))
        assertEquals(WidgetLayoutMode.LIST, spec.mode)
    }

    @Test
    fun `height exactly at threshold takes the LARGE branch`() {
        val spec = WidgetSizeResolver.resolve(DpSize(260.dp, 200.dp))
        assertEquals(WidgetLayoutMode.LIST_WITH_OVERDUE, spec.mode)
    }
}
