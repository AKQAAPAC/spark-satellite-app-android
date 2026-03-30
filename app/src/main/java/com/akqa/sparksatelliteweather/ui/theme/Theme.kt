package com.akqa.sparksatelliteweather.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.akqa.sparksatelliteweather.ui.theme.Background
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SparkColorScheme = lightColorScheme(
    primary = OnBackground,
    onPrimary = Background,
    surface = Background,
    onSurface = OnBackground,
    primaryContainer = SurfaceVariant,
    onPrimaryContainer = OnBackground
)

@Composable
fun SparkSatelliteWeatherTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = SparkColorScheme, content = content)
}
