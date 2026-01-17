package dev.tuandoan.tasktracker.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Material 3 consistent spacing system based on 8dp grid
 * Following Material Design spacing guidelines for optimal layout consistency
 */
object AppSpacing {
    // Basic spacing units (multiples of 8dp for consistency)
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
    val huge = 32.dp
    val extraHuge = 40.dp

    // Specific use-case spacing
    val minTouchTarget = 48.dp // Minimum touch target size (accessibility)
    val cardPadding = large // Standard card internal padding
    val screenPadding = large // Screen edge padding
    val sectionSpacing = extraLarge // Between major sections
    val itemSpacing = medium // Between list items
    val chipSpacing = small // Between chips/tags
    val buttonSpacing = medium // Between buttons
    val dialogPadding = extraLarge // Dialog internal padding
    val topBarHeight = 64.dp // Standard top app bar height

    // Layout-specific spacing
    val taskItemVerticalPadding = medium
    val taskItemHorizontalPadding = large
    val sectionHeaderPadding = medium
    val fabMargin = large
    val snackbarMargin = large
    val searchFieldPadding = medium
    val filterChipPadding = small
}
