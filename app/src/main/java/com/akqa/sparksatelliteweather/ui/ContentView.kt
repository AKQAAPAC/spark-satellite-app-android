package com.akqa.sparksatelliteweather.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akqa.sparksatelliteweather.Connectivity
import com.akqa.sparksatelliteweather.R
import com.akqa.sparksatelliteweather.WeatherUiState
import com.akqa.sparksatelliteweather.data.DayForecast
import com.akqa.sparksatelliteweather.data.HourForecast
import com.akqa.sparksatelliteweather.ui.theme.Background
import com.akqa.sparksatelliteweather.ui.theme.OnBackground
import com.akqa.sparksatelliteweather.ui.theme.OnBackgroundVariant
import com.akqa.sparksatelliteweather.ui.theme.SurfaceVariant
import com.akqa.sparksatelliteweather.ui.theme.SurfaceVariant2
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapType
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
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(top = 60.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        StatusBar(
            connectivity = state.connectivity,
            lastFetchAtMillis = state.lastWeatherFetchAtMillis,
            onRefresh = onRefreshLocation
        )
        Spacer(modifier = Modifier.height(24.dp))

        when {
            state.noLocation -> NoLocationCard()
            state.weatherLoadError -> ErrorCard()
            state.daily.isEmpty() -> LoadingCard()
            else -> ForecastCard(
                state = state,
                onSelectDay = onSelectDay
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (state.daily.isNotEmpty()) {
            HourlySection(
                hours = state.displayHours,
                isToday = state.selectedDate == state.daily.firstOrNull()?.date
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

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
}

@Composable
private fun StatusBar(
    connectivity: Connectivity,
    lastFetchAtMillis: Long?,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = connectivityStatusLabel(connectivity),
                color = OnBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.status_refresh),
                    color = OnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onRefresh)
                )
                lastFetchAtMillis?.let { ms ->
                    Text(
                        text = formatLastWeatherFetch(ms),
                        color = OnBackgroundVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastCard(state: WeatherUiState, onSelectDay: (String) -> Unit) {
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.daily.forEach { day ->
                DayRow(
                    day = day,
                    isSelected = day.date == state.selectedDate,
                    onClick = { onSelectDay(day.date) }
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = state.placeName ?: stringResource(R.string.place_current_location),
                color = OnBackgroundVariant,
                fontSize = 15.sp
            )
            IoniconsWeatherIcon(
                code = selectedDay.weatherCode,
                size = 36.dp,
                color = OnBackground
            )
            Text(
                text = "${temp?.toInt() ?: "-"}°",
                color = OnBackground,
                fontSize = 36.sp
            )
            Text(
                text = "High ${selectedDay.maxTemp.toInt()}° · Low ${selectedDay.minTemp.toInt()}°",
                color = OnBackgroundVariant,
                fontSize = 13.sp
            )
            if (precipLow != null && precipHigh != null) {
                Text(
                    text = if (precipLow == precipHigh) "$precipLow% precipitation" else "Precip $precipLow–$precipHigh%",
                    color = OnBackgroundVariant,
                    fontSize = 13.sp
                )
            }
            if (windLow != null && windHigh != null) {
                Text(
                    text = if (windLow == windHigh) "${windLow.toInt()} km/h wind" else "Wind ${windLow.toInt()}–${windHigh.toInt()} km/h",
                    color = OnBackgroundVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun DayRow(day: DayForecast, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SurfaceVariant else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = day.dayLabel, color = OnBackgroundVariant, fontSize = 13.sp, modifier = Modifier.width(40.dp))
        Box(
            modifier = Modifier.size(height = 24.dp, width = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            IoniconsWeatherIcon(
                code = day.weatherCode,
                size = 20.dp,
                color = OnBackground
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "${day.maxTemp.toInt()}° / ${day.minTemp.toInt()}°", color = OnBackground, fontSize = 12.sp)
    }
}

@Composable
private fun HourlySection(hours: List<HourForecast>, isToday: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            hours.forEachIndexed { index, hour ->
            Card(
                modifier = Modifier.width(60.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isToday && index == 0) "Now" else hour.hourLabel,
                        color = OnBackgroundVariant,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier.size(width = 28.dp, height = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IoniconsWeatherIcon(
                            code = hour.weatherCode,
                            size = 22.dp,
                            color = OnBackground
                        )
                    }
                    Text("${hour.temperature.toInt()}°", color = OnBackground, fontSize = 15.sp)
                    hour.precipitationProbability?.let { Text("$it%", color = OnBackgroundVariant, fontSize = 10.sp) }
                    hour.windSpeed10m?.let { Text("${it.toInt()} km/h", color = OnBackgroundVariant, fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable
private fun RainMapSection(
    mapLat: Double?,
    mapLon: Double?,
    radarFrames: List<com.akqa.sparksatelliteweather.data.RainViewerApi.RadarFrame>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant2),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rain_map_title), color = OnBackground, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                            .background(SurfaceVariant)
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = OnBackground)
                }
            }

            if (radarFrames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rain_map_older), color = OnBackgroundVariant, fontSize = 10.sp)
                    Slider(
                        value = selectedIndex.toFloat(),
                        onValueChange = { v -> onIndexChange(kotlin.math.round(v).toInt().coerceIn(0, radarFrames.size - 1)) },
                        valueRange = 0f..(radarFrames.size - 1).coerceAtLeast(0).toFloat(),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = OnBackground, activeTrackColor = OnBackgroundVariant)
                    )
                    Text(stringResource(R.string.rain_map_newer), color = OnBackgroundVariant, fontSize = 10.sp)
                }
                val time = radarFrames.getOrNull(selectedIndex)?.time
                val timeStr = time?.let { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it * 1000)) }
                val radarAttribution = stringResource(R.string.rain_map_attribution)
                Text(
                    text = buildString {
                        timeStr?.let { append("$it · ") }
                        append(radarAttribution)
                    },
                    color = OnBackgroundVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun NoLocationCard() {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariant2), shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.location_prompt_settings),
                color = OnBackground,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.open_settings),
                color = OnBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariant2), shape = RoundedCornerShape(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OnBackground)
        }
    }
}

@Composable
private fun ErrorCard() {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariant2), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.error_unable_load_weather), color = OnBackground, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PlaceholderCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariant2), shape = RoundedCornerShape(20.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(24.dp),
            color = OnBackground,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
