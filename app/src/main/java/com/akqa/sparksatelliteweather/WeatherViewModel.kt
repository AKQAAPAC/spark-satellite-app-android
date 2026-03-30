package com.akqa.sparksatelliteweather

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akqa.sparksatelliteweather.data.DayForecast
import com.akqa.sparksatelliteweather.data.HourForecast
import com.akqa.sparksatelliteweather.data.RainViewerApi
import com.akqa.sparksatelliteweather.data.remainingHoursToday
import com.akqa.sparksatelliteweather.data.toDayForecasts
import com.akqa.sparksatelliteweather.data.toHourForecasts
import com.akqa.sparksatelliteweather.di.NetworkModule
import android.util.Log
import com.akqa.sparksatelliteweather.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class WeatherUiState(
    val connectivity: Connectivity = Connectivity.Good,
    val placeName: String? = null,
    val mapLat: Double? = null,
    val mapLon: Double? = null,
    val noLocation: Boolean = false,
    val daily: List<DayForecast> = emptyList(),
    val selectedDate: String? = null,
    val currentWeatherTemp: Double? = null,
    val currentWeatherWind: Double? = null,
    val hourly: List<HourForecast> = emptyList(),
    val displayHours: List<HourForecast> = emptyList(),
    val weatherLoadError: Boolean = false,
    /** Epoch millis when weather last loaded successfully; null after location/API failure (shown under Refresh). */
    val lastWeatherFetchAtMillis: Long? = null,
    val radarFrames: List<RainViewerApi.RadarFrame> = emptyList(),
    val selectedRadarIndex: Int = 0
)

/** Hours for the selected day from [hourly]; empty when [daily] is empty. Not gated on connectivity. */
private fun recalculateDisplayHours(
    daily: List<DayForecast>,
    hourly: List<HourForecast>,
    selectedDate: String?
): List<HourForecast> {
    if (daily.isEmpty() || hourly.isEmpty()) return emptyList()
    val resolved = selectedDate?.takeIf { d -> daily.any { it.date == d } }
        ?: daily.firstOrNull()?.date ?: return emptyList()
    val firstDate = daily.first().date
    return if (resolved == firstDate) hourly.remainingHoursToday()
    else hourly.filter { it.time.startsWith(resolved) }
}

private fun WeatherUiState.withDisplayHoursRecalculated(): WeatherUiState =
    copy(displayHours = recalculateDisplayHours(daily, hourly, selectedDate))

/** State includes [connectivity]; rain map only when Good. Hourly strip follows [daily] / [selectedDate]. */
class WeatherViewModel(
    private val context: Context,
    private val connectivityManager: ConnectivityManager
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private val api = NetworkModule.weatherApi

    // Location: fetched once on app start and again when user taps Refresh. No timer.
    init {
        connectivityManager.connectivityFlow(context)
            .onEach { conn -> _state.update { s -> s.copy(connectivity = conn) } }
            .launchIn(viewModelScope)
        requestWeather()
    }

    /** Fetches location (if forceRefreshLocation then fresh GPS; else last known first), then weather. Called on start and on Refresh. */
    fun requestWeather(forceRefreshLocation: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(weatherLoadError = false, noLocation = false) }
            val location = context.getCurrentLocation(forceRefresh = forceRefreshLocation)
            if (location == null) {
                _state.update {
                    it.copy(
                        placeName = context.getString(R.string.no_location_found),
                        mapLat = null,
                        mapLon = null,
                        noLocation = true,
                        daily = emptyList(),
                        selectedDate = null,
                        currentWeatherTemp = null,
                        currentWeatherWind = null,
                        hourly = emptyList(),
                        displayHours = emptyList(),
                        lastWeatherFetchAtMillis = null,
                        radarFrames = emptyList(),
                        selectedRadarIndex = 0
                    )
                }
                return@launch
            }
            val lat = location.latitude
            val lon = location.longitude
            _state.update {
                it.copy(
                    placeName = location.placeName ?: context.getString(R.string.place_current_location),
                    mapLat = lat,
                    mapLon = lon
                )
            }

            val conn = _state.value.connectivity
            try {
                val response = api.getForecast(lat, lon)
                val daily = response.toDayForecasts()
                val hourly = response.toHourForecasts()
                val selected = _state.value.selectedDate
                val selectedDate = selected?.takeIf { d -> daily.any { it.date == d } } ?: daily.firstOrNull()?.date
                _state.update {
                    it.copy(
                        daily = daily,
                        selectedDate = selectedDate,
                        currentWeatherTemp = response.current.temperature2m,
                        currentWeatherWind = response.current.windSpeed10m,
                        hourly = hourly,
                        weatherLoadError = false,
                        lastWeatherFetchAtMillis = System.currentTimeMillis()
                    ).withDisplayHoursRecalculated()
                }

                if (conn == Connectivity.Good) {
                    val frames = RainViewerApi.getRadarFrames(lat, lon)
                    _state.update {
                        it.copy(
                            radarFrames = frames,
                            selectedRadarIndex = (frames.size - 1).coerceAtLeast(0)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Weather API failed", e)
                _state.update {
                    it.copy(
                        weatherLoadError = true,
                        lastWeatherFetchAtMillis = null,
                        daily = emptyList(),
                        selectedDate = null,
                        currentWeatherTemp = null,
                        currentWeatherWind = null,
                        hourly = emptyList(),
                        displayHours = emptyList(),
                        radarFrames = emptyList(),
                        selectedRadarIndex = 0
                    )
                }
            }
        }
    }

    fun selectDay(date: String) {
        _state.update {
            it.copy(selectedDate = date).withDisplayHoursRecalculated()
        }
    }

    fun setRadarIndex(index: Int) {
        _state.update { s ->
            s.copy(selectedRadarIndex = index.coerceIn(0, (s.radarFrames.size - 1).coerceAtLeast(0)))
        }
    }

    private fun MutableStateFlow<WeatherUiState>.update(block: (WeatherUiState) -> WeatherUiState) {
        value = block(value)
    }
}
