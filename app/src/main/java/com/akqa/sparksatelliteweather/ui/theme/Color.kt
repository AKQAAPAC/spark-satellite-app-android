package com.akqa.sparksatelliteweather.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Tokens from Spark Generative Commerce design system
 * (Figma BAEZNkIwx845LdB2Msfhat — Primitives / Color).
 */
@Immutable
data class SparkColors(
    val bgCanvas: Color,
    val bgBrand: Color,
    val bgPlan: Color,
    val bgPrimarySubtle: Color,
    val textInverse: Color,
    val textOnDark: Color,
    val ctaCyan: Color,
    val ctaSubmit: Color,
) {
    val selectedFill: Color get() = ctaCyan.copy(alpha = 0.28f)

    companion object {
        val Light = SparkColors(
            bgCanvas = Color(0xFFFFFFFF),
            bgBrand = Color(0xFF400E7D),
            bgPlan = Color(0xFFEEEDF0),
            bgPrimarySubtle = Color(0xFFE6DDFD),
            textInverse = Color(0xFF24242E),
            textOnDark = Color(0xFF400E7D),
            ctaCyan = Color(0xFF2DF4E4),
            ctaSubmit = Color(0xFF8950DA),
        )

        val Dark = SparkColors(
            bgCanvas = Color(0xFF1A0831),
            bgBrand = Color(0xFF400E7D),
            bgPlan = Color(0xFF350570),
            bgPrimarySubtle = Color(0xFFE6DDFD),
            textInverse = Color(0xFFFFFFFF),
            textOnDark = Color(0xFFE6DDFD),
            ctaCyan = Color(0xFF2DF4E4),
            ctaSubmit = Color(0xFF8950DA),
        )

        fun forAppearance(appearance: SparkAppearance): SparkColors =
            if (appearance == SparkAppearance.Light) Light else Dark
    }
}
