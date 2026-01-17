package dev.tuandoan.tasktracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shape system with consistent rounded corners
 * Following Material Design shape guidelines for visual hierarchy
 */
val AppShapes = Shapes(
    // Small components: chips, small buttons
    extraSmall = RoundedCornerShape(4.dp),

    // Small elements: small cards, text fields
    small = RoundedCornerShape(8.dp),

    // Medium elements: cards, buttons, list items
    medium = RoundedCornerShape(12.dp),

    // Large elements: dialogs, sheets
    large = RoundedCornerShape(16.dp),

    // Extra large: modal sheets, major containers
    extraLarge = RoundedCornerShape(20.dp),
)

/**
 * Additional custom shapes for specific use cases
 */
object CustomShapes {
    // Task item cards - subtle rounding for list appearance
    val taskItem = RoundedCornerShape(8.dp)

    // Section headers - minimal rounding
    val sectionHeader = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)

    // Tags/chips - fully rounded
    val chip = RoundedCornerShape(50)

    // Search field - medium rounding
    val searchField = RoundedCornerShape(12.dp)

    // Dialogs and bottom sheets - generous rounding
    val bottomSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    // FAB - circular
    val fab = RoundedCornerShape(50)

    // Snackbar - minimal rounding
    val snackbar = RoundedCornerShape(4.dp)
}
