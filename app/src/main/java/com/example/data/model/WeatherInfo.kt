package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

data class WeatherInfo(
    val cityName: String,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val relativeHumidity: Double,
    val windSpeedKmh: Double,
    val conditionDesc: String,
    val icon: ImageVector,
    val weatherCode: Int
) {
    val temperatureFahrenheit: Double
        get() = (temperatureCelsius * 9 / 5) + 32

    val feelsLikeFahrenheit: Double
        get() = (feelsLikeCelsius * 9 / 5) + 32

    companion object {
        fun fromCode(code: Int): WeatherDetails {
            return when (code) {
                0 -> WeatherDetails("Sunny & Clear", Icons.Rounded.WbSunny)
                1, 2, 3 -> WeatherDetails("Partly Cloudy", Icons.Rounded.Cloud)
                45, 48 -> WeatherDetails("Foggy", Icons.Rounded.BlurOn)
                51, 53, 55 -> WeatherDetails("Light Drizzle", Icons.Rounded.WaterDrop)
                61, 63, 65 -> WeatherDetails("Rainy", Icons.Rounded.Grain)
                71, 73, 75, 77 -> WeatherDetails("Snowy", Icons.Rounded.AcUnit)
                80, 81, 82 -> WeatherDetails("Rain Showers", Icons.Rounded.BeachAccess)
                85, 86 -> WeatherDetails("Heavy Snow Showers", Icons.Rounded.SevereCold)
                95, 96, 99 -> WeatherDetails("Thunderstorm", Icons.Rounded.FlashOn)
                else -> WeatherDetails("Cloudy", Icons.Rounded.Cloud)
            }
        }
    }
}

data class WeatherDetails(
    val description: String,
    val icon: ImageVector
)
