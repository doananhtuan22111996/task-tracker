package dev.tuandoan.tasktracker.widget.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Layout mode selected by widget size, declared in V13-01.
 *
 * - [COMPACT_BADGE]: 2x2 — count badge + top 1 task.
 * - [LIST]: 4x2 — top 5 tasks (current default behavior).
 * - [LIST_WITH_OVERDUE]: 4x4 — top 10 tasks with an overdue section header.
 */
enum class WidgetLayoutMode { COMPACT_BADGE, LIST, LIST_WITH_OVERDUE }

data class WidgetLayoutSpec(val mode: WidgetLayoutMode, val rowCount: Int)

/**
 * Pure helper mapping widget [DpSize] (from `LocalSize.current`) to the layout
 * spec we should render. No Glance imports — unit-testable on the JVM.
 *
 * Threshold (200dp) sits midway between the V13-01 SMALL (110dp) and
 * MEDIUM/LARGE (260dp) breakpoints so launcher-cell quantization can't bump
 * a 4x2 widget into the 2x2 bucket.
 */
object WidgetSizeResolver {
    private val WIDTH_THRESHOLD = 200.dp
    private val HEIGHT_THRESHOLD = 200.dp

    const val COMPACT_ROW_COUNT = 1
    const val LIST_ROW_COUNT = 5
    const val LARGE_ROW_COUNT = 10

    /**
     * Upper bound on tasks fetched for the widget. Equals [LARGE_ROW_COUNT] so
     * the 4x4 layout has enough rows; smaller layouts slice client-side.
     * The 2x2 count badge displays "${LARGE_ROW_COUNT}+" when the fetched list
     * hits this cap, since we don't have an exact total without a separate
     * count query (V13-03 will revisit).
     */
    const val FETCH_LIMIT = LARGE_ROW_COUNT

    fun resolve(size: DpSize): WidgetLayoutSpec = when {
        size.width < WIDTH_THRESHOLD ->
            WidgetLayoutSpec(WidgetLayoutMode.COMPACT_BADGE, COMPACT_ROW_COUNT)
        size.height >= HEIGHT_THRESHOLD ->
            WidgetLayoutSpec(WidgetLayoutMode.LIST_WITH_OVERDUE, LARGE_ROW_COUNT)
        else ->
            WidgetLayoutSpec(WidgetLayoutMode.LIST, LIST_ROW_COUNT)
    }
}
