package com.holidate.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Coral = Color(0xFFFF5A6E)
private val CoralDark = Color(0xFFC7384A)
private val Teal = Color(0xFF14B8A6)
private val Sand = Color(0xFFFFF3EC)

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    secondary = Teal,
    onSecondary = Color.White,
    background = Sand,
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    secondary = Teal,
    onSecondary = Color.Black,
    primaryContainer = CoralDark,
)

@Composable
fun HoliDateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content,
    )
}
