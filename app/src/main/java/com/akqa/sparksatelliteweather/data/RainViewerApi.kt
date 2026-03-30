package com.akqa.sparksatelliteweather.data

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object RainViewerApi {

    private const val mapsUrl = "https://api.rainviewer.com/public/weather-maps.json"
    private const val size = "512"
    private const val zoom = "4"
    private const val colorScheme = "2"
    private const val options = "1_1"

    data class RadarFrame(val time: Long, val imageUrl: String)

    data class MapsResponse(
        val host: String,
        val radar: Radar?
    ) {
        data class Radar(val past: List<PastFrame>?)
        data class PastFrame(
            @SerializedName("time") val time: Long,
            @SerializedName("path") val path: String
        )
    }

    suspend fun getRadarFrames(lat: Double, lon: Double): List<RadarFrame> = withContext(Dispatchers.IO) {
        try {
            val conn = URL(mapsUrl).openConnection() as HttpsURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val response = com.google.gson.Gson().fromJson(json, MapsResponse::class.java)
            val host = response.host
            val past = response.radar?.past ?: return@withContext emptyList()
            past.map { frame ->
                val pathPart = if (frame.path.endsWith("/")) frame.path else "${frame.path}/"
                RadarFrame(
                    time = frame.time,
                    imageUrl = "$host$pathPart$size/$zoom/$lat/$lon/$colorScheme/$options.png"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
