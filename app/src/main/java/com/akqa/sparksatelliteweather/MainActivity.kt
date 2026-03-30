package com.akqa.sparksatelliteweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import com.akqa.sparksatelliteweather.ui.ContentView
import com.akqa.sparksatelliteweather.ui.theme.SparkSatelliteWeatherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Testing override: persist so connectivity flow can read it (e.g. adb --es connectivity_override good|low|none).
        intent.getStringExtra("connectivity_override")?.let { value ->
            if (value in listOf("good", "low", "none")) {
                getSharedPreferences(CONNECTIVITY_OVERRIDE_PREF, MODE_PRIVATE).edit()
                    .putString(CONNECTIVITY_OVERRIDE_PREF, value).apply()
            }
        }

        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val viewModel = ViewModelProvider(
            this,
            WeatherViewModelFactory(applicationContext, connectivityManager)
        )[WeatherViewModel::class.java]

        setContent {
            SparkSatelliteWeatherTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                ContentView(
                    state = state,
                    onRefreshLocation = { viewModel.requestWeather(forceRefreshLocation = true) },
                    onSelectDay = { viewModel.selectDay(it) },
                    onRadarIndexChange = { viewModel.setRadarIndex(it) }
                )
            }
        }
    }
}
