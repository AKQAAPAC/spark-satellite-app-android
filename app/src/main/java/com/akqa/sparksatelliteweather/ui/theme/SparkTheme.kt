package com.akqa.sparksatelliteweather.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SparkAppearance(val storageValue: String) {
    Light("light"),
    Dark("dark");

    companion object {
        const val STORAGE_KEY = "sparkColorScheme"

        fun fromStorage(raw: String?): SparkAppearance =
            entries.firstOrNull { it.storageValue == raw } ?: Dark
    }
}

object SparkSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val card = 18.dp
}

object SparkRadius {
    val sm = 16.dp
    val card = 21.dp
    val md = 24.dp
    val full = 999.dp
}

val LocalSparkColors = compositionLocalOf { SparkColors.Dark }
val LocalSparkAppearance = compositionLocalOf { SparkAppearance.Dark }

@Composable
fun Modifier.sparkPlanCard(
    radius: Dp = SparkRadius.md,
    padding: Dp = SparkSpacing.card,
): Modifier {
    val colors = LocalSparkColors.current
    return this
        .clip(RoundedCornerShape(radius))
        .background(colors.bgPlan)
        .padding(padding)
}
