package com.example.nothingplants.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NothingRed,
    secondary = NothingWhite,
    tertiary = NothingLightGray,
    background = NothingBlack,
    surface = NothingBlack,
    onPrimary = NothingWhite,
    onSecondary = NothingBlack,
    onTertiary = NothingBlack,
    onBackground = NothingWhite,
    onSurface = NothingWhite,
)

// In stile Nothing forziamo quasi tutto sullo scuro, ma gestiamo il light theme
private val LightColorScheme = lightColorScheme(
    primary = NothingRed,
    secondary = NothingBlack,
    tertiary = NothingDarkGray,
    background = NothingWhite,
    surface = NothingWhite,
    onPrimary = NothingWhite,
    onSecondary = NothingWhite,
    onTertiary = NothingWhite,
    onBackground = NothingBlack,
    onSurface = NothingBlack,
)

@Composable
fun NothingPlantsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
