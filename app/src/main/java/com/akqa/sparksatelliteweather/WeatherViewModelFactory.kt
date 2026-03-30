package com.akqa.sparksatelliteweather

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WeatherViewModelFactory(
    private val context: Context,
    private val connectivityManager: android.net.ConnectivityManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != WeatherViewModel::class.java) throw IllegalArgumentException("Unknown ViewModel")
        return WeatherViewModel(context, connectivityManager) as T
    }
}
