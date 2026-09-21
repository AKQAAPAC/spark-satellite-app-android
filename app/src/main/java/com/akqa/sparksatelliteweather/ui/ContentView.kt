package com.akqa.sparksatelliteweather.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.akqa.sparksatelliteweather.Connectivity
import com.akqa.sparksatelliteweather.R
import com.akqa.sparksatelliteweather.WeatherUiState
import com.akqa.sparksatelliteweather.data.DayForecast
import com.akqa.sparksatelliteweather.data.HourForecast
import com.akqa.sparksatelliteweather.ui.theme.LocalSetSparkAppearance
import com.akqa.sparksatelliteweather.ui.theme.LocalSparkAppearance
import com.akqa.sparksatelliteweather.ui.theme.LocalSparkColors
import com.akqa.sparksatelliteweather.ui.theme.SparkAppearance
import com.akqa.sparksatelliteweather.ui.theme.SparkRadius
import com.akqa.sparksatelliteweather.ui.theme.SparkSpacing
import com.akqa.sparksatelliteweather.ui.theme.SparkTypography
import com.akqa.sparksatelliteweather.ui.theme.sparkPlanCard
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private fun formatLastWeatherFetch(epochMillis: Long): String {
    val zdt = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM).format(zdt)
}

@Composable
private fun connectivityStatusLabel(connectivity: Connectivity): String = when (connectivity) {
    Connectivity.Good -> stringResource(R.string.connectivity_good)
    Connectivity.Low -> stringResource(R.string.connectivity_low)
    Connectivity.None -> stringResource(R.string.connectivity_none)
}

@Composable
fun ContentView(
    state: WeatherUiState,
    onRefreshLocation: () -> Unit,
    onSelectDay: (String) -> Unit,
    onRadarIndexChange: (Int) -> Unit
) {
    val colors = LocalSparkColors.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            onRefreshLocation()
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(top = SparkSpacing.sm, bottom = SparkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SparkSpacing.sm)
    ) {
        StatusBar(
            connectivity = state.connectivity,
            lastFetchAtMillis = state.lastWeatherFetchAtMillis,
            onRefresh = onRefreshLocation
        )

        Box(modifier = Modifier.padding(horizontal = SparkSpacing.lg)) {
            when {
                state.noLocation -> NoLocationCard()
                state.weatherLoadError -> ErrorCard()
                state.daily.isEmpty() -> LoadingCard()
                else -> ForecastCard(
                    state = state,
                    onSelectDay = onSelectDay
                )
            }
        }

        if (state.daily.isNotEmpty()) {
            HourlySection(
                hours = state.displayHours,
                isToday = state.selectedDate == state.daily.firstOrNull()?.date
            )
        }

        Box(modifier = Modifier.padding(horizontal = SparkSpacing.lg)) {
            if (state.connectivity == Connectivity.Good) {
                if (state.noLocation || state.mapLat == null || state.mapLon == null) {
                    PlaceholderCard(text = stringResource(R.string.rain_placeholder_need_location))
                } else {
                    RainMapSection(
                        mapLat = state.mapLat,
                        mapLon = state.mapLon,
                        radarFrames = state.radarFrames,
                        selectedIndex = state.selectedRadarIndex,
                        onIndexChange = onRadarIndexChange
                    )
                }
            } else {
                PlaceholderCard(
                    text = if (state.connectivity == Connectivity.None) {
                        stringResource(R.string.rain_placeholder_offline)
                    } else {
                        stringResource(R.string.rain_placeholder_low_data)
                    }
                )
            }
        }

        ThemeToggle()
    }
}

@Composable
private fun StatusBar(
    connectivity: Connectivity,
    lastFetchAtMillis: Long?,
    onRefresh: () -> Unit
) {
    val colors = LocalSparkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SparkSpacing.lg)
            .clip(RoundedCornerShape(SparkRadius.sm))
            .background(colors.bgPlan)
            .padding(horizontal = SparkSpacing.md, vertical = SparkSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = connectivityStatusLabel(connectivity),
            style = SparkTypography.planLabel,
            color = colors.textInverse,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.status_refresh),
                style = SparkTypography.planLabel,
                color = colors.bgBrand,
                modifier = Modifier
                    .clip(RoundedCornerShape(SparkRadius.full))
                    .background(colors.ctaCyan)
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 12.dp, vertical = SparkSpacing.xs)
            )
            lastFetchAtMillis?.let { ms ->
                Text(
                    text = formatLastWeatherFetch(ms),
                    style = SparkTypography.micro,
                    color = colors.textOnDark,
                    modifier = Modifier.padding(top = SparkSpacing.xs)
                )
            }
        }
    }
}

@Composable
private fun ForecastCard(state: WeatherUiState, onSelectDay: (String) -> Unit) {
    val colors = LocalSparkColors.current
    val selectedDay = state.daily.find { it.date == state.selectedDate } ?: state.daily.first()
    val isToday = state.selectedDate == state.daily.firstOrNull()?.date
    val temp = if (isToday) state.currentWeatherTemp else selectedDay.maxTemp
    val precipValues = state.displayHours.mapNotNull { it.precipitationProbability }
    val precipLow = precipValues.minOrNull()
    val precipHigh = precipValues.maxOrNull()
    val windValues = state.displayHours.mapNotNull { it.windSpeed10m }
    val windLow = windValues.minOrNull()
    val windHigh = windValues.maxOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sparkPlanCard(padding = SparkSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(SparkSpacing.sm)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            state.daily.forEach { day ->
                DayRow(
                    day = day,
                    isSelected = day.date == state.selectedDate,
                    onClick = { onSelectDay(day.date) }
                )
            }
        }
        Column(
            modifier = Modifier.width(128.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(SparkSpacing.xs)
        ) {
            Text(
                text = state.placeName ?: stringResource(R.string.place_current_location),
                style = SparkTypography.body,
                color = colors.textOnDark,
                textAlign = TextAlign.End
            )
            IoniconsWeatherIcon(
                code = selectedDay.weatherCode,
                size = 28.dp,
                color = colors.textOnDark
            )
            Text(
                text = "${temp?.toInt() ?: "-"}°",
                style = SparkTypography.display,
                color = colors.textOnDark
            )
            Text(
                text = "Low ${selectedDay.minTemp.toInt()}° · High ${selectedDay.maxTemp.toInt()}°",
                style = SparkTypography.sectionDesc,
                color = colors.textInverse.copy(alpha = 0.85f)
            )
            if (precipLow != null && precipHigh != null) {
                Text(
                    text = if (precipLow == precipHigh) "$precipLow% precipitation" else "Precip $precipLow–$precipHigh%",
                    style = SparkTypography.sectionDesc,
                    color = colors.textInverse.copy(alpha = 0.85f)
                )
            }
            if (windLow != null && windHigh != null) {
                Text(
                    text = if (windLow == windHigh) "${windLow.toInt()} km/h wind" else "Wind ${windLow.toInt()}–${windHigh.toInt()} km/h",
                    style = SparkTypography.sectionDesc,
                    color = colors.textInverse.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun DayRow(day: DayForecast, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalSparkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SparkSpacing.sm))
            .background(if (isSelected) colors.selectedFill else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = SparkSpacing.sm, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = day.dayLabel,
            style = SparkTypography.sectionDesc,
            color = colors.textInverse.copy(alpha = 0.9f)
        )
        IoniconsWeatherIcon(
            code = day.weatherCode,
            size = 16.dp,
            color = colors.textOnDark
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${day.minTemp.toInt()}°–${day.maxTemp.toInt()}°",
            style = SparkTypography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textInverse
        )
    }
}

@Composable
private fun HourlySection(hours: List<HourForecast>, isToday: Boolean) {
    val colors = LocalSparkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = SparkSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(SparkSpacing.sm)
    ) {
        hours.forEachIndexed { index, hour ->
            val isNow = isToday && index == 0
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .clip(RoundedCornerShape(SparkRadius.sm))
                    .background(colors.bgPlan)
            ) {
                if (isNow) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(colors.selectedFill)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SparkSpacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = if (isNow) "Now" else hour.hourLabel,
                        style = SparkTypography.caption,
                        color = colors.textInverse.copy(alpha = 0.9f)
                    )
                    IoniconsWeatherIcon(
                        code = hour.weatherCode,
                        size = 18.dp,
                        color = colors.textOnDark
                    )
                    Text(
                        text = "${hour.temperature.toInt()}°",
                        style = SparkTypography.planLabel,
                        color = colors.textOnDark
                    )
                    hour.precipitationProbability?.let {
                        Text(
                            text = "$it%",
                            style = SparkTypography.micro,
                            color = colors.textInverse.copy(alpha = 0.8f)
                        )
                    }
                    hour.windSpeed10m?.let {
                        Text(
                            text = "${it.toInt()} km/h",
                            style = SparkTypography.micro,
                            color = colors.textInverse.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RainMapSection(
    mapLat: Double?,
    mapLon: Double?,
    radarFrames: List<com.akqa.sparksatelliteweather.data.RainViewerApi.RadarFrame>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit
) {
    val colors = LocalSparkColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sparkPlanCard()
    ) {
        Text(
            text = stringResource(R.string.rain_map_title),
            style = SparkTypography.productName,
            color = colors.textInverse
        )
        Spacer(modifier = Modifier.height(SparkSpacing.sm))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(SparkRadius.sm))
        ) {
            if (mapLat != null && mapLon != null) {
                key(mapLat, mapLon) {
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(mapLat, mapLon), 10f)
                    }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(mapType = MapType.SATELLITE),
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bgPlan)
                )
            }
            val frame = radarFrames.getOrNull(selectedIndex)
            if (frame != null) {
                AsyncImage(
                    model = frame.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.82f
                )
            } else if (mapLat == null || mapLon == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.ctaCyan
                )
            }
        }

        if (radarFrames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(SparkSpacing.sm))
            val sliderTrackColor = colors.textInverse.copy(alpha = 0.25f)
            val sliderColors = SliderDefaults.colors(
                thumbColor = colors.ctaCyan,
                activeTrackColor = sliderTrackColor,
                inactiveTrackColor = sliderTrackColor,
            )
            val sliderInteractionSource = remember { MutableInteractionSource() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.rain_map_older),
                    style = SparkTypography.micro,
                    color = colors.textOnDark
                )
                Slider(
                    value = selectedIndex.toFloat(),
                    onValueChange = { v ->
                        onIndexChange(kotlin.math.round(v).toInt().coerceIn(0, radarFrames.size - 1))
                    },
                    valueRange = 0f..(radarFrames.size - 1).coerceAtLeast(0).toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = SparkSpacing.sm),
                    interactionSource = sliderInteractionSource,
                    colors = sliderColors,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = sliderInteractionSource,
                            colors = sliderColors,
                            thumbSize = DpSize(30.dp, 20.dp),
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(3.dp),
                            colors = sliderColors,
                        )
                    },
                )
                Text(
                    text = stringResource(R.string.rain_map_newer),
                    style = SparkTypography.micro,
                    color = colors.textOnDark
                )
            }
            val time = radarFrames.getOrNull(selectedIndex)?.time
            val locale = LocalConfiguration.current.locales[0]
            val timeStr = remember(time, locale) {
                time?.let {
                    java.text.SimpleDateFormat("HH:mm", locale).format(java.util.Date(it * 1000))
                }
            }
            val radarAttribution = stringResource(R.string.rain_map_attribution)
            Text(
                text = buildString {
                    timeStr?.let { append("$it · ") }
                    append(radarAttribution)
                },
                modifier = Modifier.fillMaxWidth(),
                style = SparkTypography.micro,
                color = colors.textOnDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ThemeToggle() {
    val colors = LocalSparkColors.current
    val appearance = LocalSparkAppearance.current
    val setAppearance = LocalSetSparkAppearance.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SparkSpacing.xs, bottom = SparkSpacing.md),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(SparkRadius.full))
                .background(colors.bgBrand)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(SparkSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeTabItem(
                icon = Icons.Filled.WbSunny,
                contentDescription = stringResource(R.string.theme_light),
                isActive = appearance == SparkAppearance.Light,
                onClick = { setAppearance(SparkAppearance.Light) }
            )
            ThemeTabItem(
                icon = Icons.Filled.DarkMode,
                contentDescription = stringResource(R.string.theme_dark),
                isActive = appearance == SparkAppearance.Dark,
                onClick = { setAppearance(SparkAppearance.Dark) }
            )
        }
    }
}

@Composable
private fun ThemeTabItem(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSparkColors.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isActive) colors.ctaSubmit else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun NoLocationCard() {
    val context = LocalContext.current
    val colors = LocalSparkColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sparkPlanCard(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SparkSpacing.sm)
    ) {
        Text(
            text = stringResource(R.string.location_prompt_settings),
            style = SparkTypography.body,
            color = colors.textOnDark,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.open_settings),
            style = SparkTypography.planLabel,
            color = colors.bgBrand,
            modifier = Modifier
                .clip(RoundedCornerShape(SparkRadius.sm))
                .background(colors.ctaCyan)
                .clickable {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
                .padding(horizontal = SparkSpacing.md, vertical = SparkSpacing.sm)
        )
    }
}

@Composable
private fun LoadingCard() {
    val colors = LocalSparkColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .sparkPlanCard(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = colors.ctaCyan)
    }
}

@Composable
private fun ErrorCard() {
    val colors = LocalSparkColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sparkPlanCard(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.error_unable_load_weather),
            style = SparkTypography.productName,
            color = colors.textInverse
        )
    }
}

@Composable
private fun PlaceholderCard(text: String) {
    val colors = LocalSparkColors.current
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .sparkPlanCard(),
        style = SparkTypography.body,
        color = colors.textInverse,
        textAlign = TextAlign.Center
    )
}
