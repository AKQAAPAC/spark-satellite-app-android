package com.akqa.sparksatelliteweather.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalSetSparkAppearance = compositionLocalOf<(SparkAppearance) -> Unit> {
    error("LocalSetSparkAppearance not provided")
}

@Composable
fun SparkSatelliteWeatherTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(SparkAppearance.STORAGE_KEY, Context.MODE_PRIVATE)
    }
    var appearance by remember {
        mutableStateOf(SparkAppearance.fromStorage(prefs.getString(SparkAppearance.STORAGE_KEY, null)))
    }
    val colors = SparkColors.forAppearance(appearance)

    val colorScheme = if (appearance == SparkAppearance.Dark) {
        darkColorScheme(
            primary = colors.ctaCyan,
            onPrimary = colors.bgBrand,
            surface = colors.bgCanvas,
            onSurface = colors.textInverse,
            primaryContainer = colors.bgPlan,
            onPrimaryContainer = colors.textInverse,
        )
    } else {
        lightColorScheme(
            primary = colors.ctaCyan,
            onPrimary = colors.bgBrand,
            surface = colors.bgCanvas,
            onSurface = colors.textInverse,
            primaryContainer = colors.bgPlan,
            onPrimaryContainer = colors.textInverse,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.bgCanvas.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                appearance == SparkAppearance.Light
        }
    }

    CompositionLocalProvider(
        LocalSparkColors provides colors,
        LocalSparkAppearance provides appearance,
        LocalSetSparkAppearance provides { next ->
            appearance = next
            prefs.edit().putString(SparkAppearance.STORAGE_KEY, next.storageValue).apply()
        },
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
