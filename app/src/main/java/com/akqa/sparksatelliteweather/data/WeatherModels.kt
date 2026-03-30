package com.akqa.sparksatelliteweather.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayForecast(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val weatherCode: Int
) {
    val dayLabel: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(date) ?: return date
            val cal = Calendar.getInstance()
            cal.time = d
            val today = Calendar.getInstance()
            return when {
                cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
                else -> SimpleDateFormat("EEE", Locale.getDefault()).format(d)
            }
        }
}

data class HourForecast(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val precipitationProbability: Int?,
    val precipitationMm: Double?,
    val windSpeed10m: Double?
) {
    val hourLabel: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val d = sdf.parse(time) ?: return time
            return SimpleDateFormat("ha", Locale.getDefault()).format(d)
        }
}

fun OpenMeteoResponse.toDayForecasts(): List<DayForecast> {
    val daily = this.daily ?: return emptyList()
    val list = daily.time.indices.map { i ->
        DayForecast(
            date = daily.time[i],
            maxTemp = daily.temperature2mMax[i],
            minTemp = daily.temperature2mMin[i],
            weatherCode = daily.weatherCode[i]
        )
    }
    return list.withTodayFirst()
}

fun List<DayForecast>.withTodayFirst(): List<DayForecast> {
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val idx = indexOfFirst { it.date == todayStr }
    if (idx <= 0) return this
    return drop(idx) + take(idx)
}

fun OpenMeteoResponse.toHourForecasts(): List<HourForecast> {
    val hourly = this.hourly ?: return emptyList()
    val precipProb = hourly.precipitationProbability ?: List(hourly.time.size) { null as Int? }
    val precip = hourly.precipitation ?: List(hourly.time.size) { null as Double? }
    val wind = hourly.windSpeed10m ?: List(hourly.time.size) { null as Double? }
    return hourly.time.indices.map { i ->
        HourForecast(
            time = hourly.time[i],
            temperature = hourly.temperature2m[i],
            weatherCode = hourly.weatherCode[i],
            precipitationProbability = precipProb.getOrNull(i),
            precipitationMm = precip.getOrNull(i),
            windSpeed10m = wind.getOrNull(i)
        )
    }
}

fun List<HourForecast>.remainingHoursToday(): List<HourForecast> {
    val cal = Calendar.getInstance()
    val now = Date()
    val startOfCurrentHour = run {
        val c = Calendar.getInstance()
        c.time = now
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.time
    }
    return filter { hour ->
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val d = sdf.parse(hour.time) ?: return@filter false
        d >= startOfCurrentHour && cal.isSameDay(d)
    }
}

private fun Calendar.isSameDay(d: Date): Boolean {
    val c = Calendar.getInstance()
    c.time = d
    return get(Calendar.YEAR) == c.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)
}
