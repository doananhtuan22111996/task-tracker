package dev.tuandoan.tasktracker.widget

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import dev.tuandoan.tasktracker.data.preferences.ThemeMode
import dev.tuandoan.tasktracker.ui.theme.darkSchemeColors
import dev.tuandoan.tasktracker.ui.theme.lightSchemeColors

@Composable
fun WidgetTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme.colors
    } else {
        ColorProviders(
            light = lightSchemeColors,
            dark = darkSchemeColors,
        )
    }

    GlanceTheme(colors = colors) {
        content()
    }
}
