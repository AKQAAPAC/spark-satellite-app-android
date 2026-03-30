package com.akqa.sparksatelliteweather.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weather_code",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,precipitation_probability,precipitation,wind_speed_10m",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}

data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeatherDto,
    val daily: DailyDto?,
    val hourly: HourlyDto?
)

data class CurrentWeatherDto(
    val time: String,
    @SerializedName("temperature_2m") val temperature2m: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("wind_speed_10m") val windSpeed10m: Double?
)

data class DailyDto(
    val time: List<String>,
    @SerializedName("temperature_2m_max") val temperature2mMax: List<Double>,
    @SerializedName("temperature_2m_min") val temperature2mMin: List<Double>,
    @SerializedName("weather_code") val weatherCode: List<Int>
)

data class HourlyDto(
    val time: List<String>,
    @SerializedName("temperature_2m") val temperature2m: List<Double>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int?>?,
    @SerializedName("precipitation") val precipitation: List<Double?>?,
    @SerializedName("wind_speed_10m") val windSpeed10m: List<Double?>?
)
