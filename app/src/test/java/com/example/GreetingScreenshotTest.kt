package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.WeatherInfo
import com.example.ui.TemperatureUnit
import com.example.ui.WeatherContent
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockWeather = WeatherInfo(
      cityName = "San Francisco",
      temperatureCelsius = 22.5,
      feelsLikeCelsius = 21.0,
      relativeHumidity = 65.0,
      windSpeedKmh = 12.0,
      conditionDesc = "Sunny & Clear",
      icon = Icons.Rounded.WbSunny,
      weatherCode = 0
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        WeatherContent(
          weather = mockWeather,
          tempUnit = TemperatureUnit.CELSIUS,
          onToggleUnit = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
