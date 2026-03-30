package com.akqa.sparksatelliteweather.di

import com.akqa.sparksatelliteweather.data.WeatherApi
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val userAgentInterceptor = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "SparkSatelliteWeather/1.0 (Android)")
                .build()
        )
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(userAgentInterceptor)
        .build()

    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}
