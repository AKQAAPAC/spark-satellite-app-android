package com.akqa.sparksatelliteweather.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akqa.sparksatelliteweather.R

/**
 * WMO weather code → Ionicons codepoint.
 * Full WMO code descriptions (for reference when mapping):
 * 0 Clear sky | 1 Mainly clear | 2 Partly cloudy | 3 Overcast
 * 45 Fog | 48 Depositing rime fog
 * 51 Light drizzle | 53 Moderate drizzle | 55 Dense drizzle | 56–57 Freezing drizzle
 * 61 Slight rain | 63 Moderate rain | 65 Heavy rain | 66–67 Freezing rain
 * 71,73,75 Snow fall | 77 Snow grains
 * 80 Slight rain showers | 81 Moderate rain showers | 82 Violent rain showers
 * 85–86 Snow showers | 95 Thunderstorm | 96,99 Thunderstorm with hail
 */

private const val IONICONS_SUNNY = 61102
private const val IONICONS_PARTLY_SUNNY = 60817
private const val IONICONS_CLOUDY = 60259
private const val IONICONS_CLOUDY_OUTLINE = 60263
private const val IONICONS_RAINY = 60937
private const val IONICONS_RAINY_OUTLINE = 60938
private const val IONICONS_SNOW = 61066
private const val IONICONS_THUNDERSTORM = 61144

private fun ioniconsCodepointForCode(code: Int): Int = when (code) {
    0 -> IONICONS_SUNNY
    1, 2 -> IONICONS_PARTLY_SUNNY
    3 -> IONICONS_CLOUDY
    45, 48 -> IONICONS_CLOUDY_OUTLINE
    in 51..67 -> IONICONS_RAINY
    in 71..77 -> IONICONS_SNOW
    in 80..82 -> IONICONS_RAINY_OUTLINE
    85, 86 -> IONICONS_SNOW
    in 95..99 -> IONICONS_THUNDERSTORM
    else -> IONICONS_CLOUDY
}

/** Weather icon for WMO code (Ionicons font). */
@Composable
fun IoniconsWeatherIcon(
    code: Int,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color
) {
    val density = LocalDensity.current
    val fontSize = with(density) { size.toSp() }
    Text(
        text = String(Character.toChars(ioniconsCodepointForCode(code))),
        fontFamily = FontFamily(Font(R.font.ionicons, FontWeight.Normal)),
        fontSize = fontSize,
        color = color,
        modifier = modifier.then(Modifier.size(size))
    )
}
