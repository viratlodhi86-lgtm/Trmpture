package com.example.data.repository

import android.content.Context
import android.location.Geocoder
import com.example.data.api.GeocodingApiService
import com.example.data.api.WeatherApiService
import com.example.data.model.GeocodingResult
import com.example.data.model.WeatherInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale

class WeatherRepository(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val weatherApi: WeatherApiService = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(WeatherApiService::class.java)

    private val geocodingApi: GeocodingApiService = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GeocodingApiService::class.java)

    suspend fun fetchWeather(latitude: Double, longitude: Double, customCityName: String? = null): WeatherInfo = withContext(Dispatchers.IO) {
        val weatherResponse = weatherApi.getForecast(latitude, longitude)
        val current = weatherResponse.current

        val resolvedCityName = customCityName ?: getCityNameFromCoords(latitude, longitude)
        val details = WeatherInfo.fromCode(current.weatherCode)

        WeatherInfo(
            cityName = resolvedCityName,
            temperatureCelsius = current.temperatureC,
            feelsLikeCelsius = current.feelsLikeC,
            relativeHumidity = current.relativeHumidity,
            windSpeedKmh = current.windSpeedKmh,
            conditionDesc = details.description,
            icon = details.icon,
            weatherCode = current.weatherCode
        )
    }

    suspend fun searchCities(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        try {
            val response = geocodingApi.searchCity(query)
            response.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getCityNameFromCoords(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: address.adminArea ?: "Near ${String.format(Locale.US, "%.2f", lat)}, ${String.format(Locale.US, "%.2f", lng)}"
            } else {
                "Near ${String.format(Locale.US, "%.2f", lat)}, ${String.format(Locale.US, "%.2f", lng)}"
            }
        } catch (e: Exception) {
            "Near ${String.format(Locale.US, "%.2f", lat)}, ${String.format(Locale.US, "%.2f", lng)}"
        }
    }
}
