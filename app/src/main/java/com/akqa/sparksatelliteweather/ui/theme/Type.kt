package com.akqa.sparksatelliteweather.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Spark Generative Commerce type scale (Figma uses Inter; app uses system default).
 */
object SparkTypography {
    val display = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
    val productName = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val planLabel = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val sectionDesc = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val micro = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
}
