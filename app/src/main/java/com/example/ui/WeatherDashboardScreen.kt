package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeocodingResult
import com.example.data.model.WeatherInfo
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboardScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearchLoading by viewModel.isSearchLoading.collectAsState()
    val tempUnit by viewModel.tempUnit.collectAsState()

    // Location request setup
    val locationPermissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val fineGranted = permissionsMap[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissionsMap[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.fetchWeatherFromLocation()
        } else {
            // Permission denied -> Fallbacks automatically to Paris inside viewModel
            viewModel.fetchWeatherFromLocation()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(locationPermissions)
    }

    // Adapt layout brush based on current weather condition
    val isDark = isSystemInDarkTheme()
    val colorScheme = determineAtmosphericGradient(uiState, isDark)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colorScheme))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Skyline Weather",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        // Refresh Button with manual rotate action
                        var isRotating by remember { mutableStateOf(false) }
                        val rotationAngle by animateFloatAsState(
                            targetValue = if (isRotating) 360f else 0f,
                            animationSpec = tween(600, easing = LinearEasing),
                            finishedListener = { isRotating = false }
                        )

                        IconButton(
                            onClick = {
                                isRotating = true
                                viewModel.refreshWeather()
                            },
                            modifier = Modifier.testTag("refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh weather details",
                                modifier = Modifier.rotate(rotationAngle),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Sleek Instant geocoder lookup box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .zIndex(10f)
                    ) {
                        Column {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                label = { Text("Search location...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    if (isSearchLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input_field")
                            )

                            // Dropdown search results mapping
                            if (searchResults.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .testTag("autocomplete_dropdown_card"),
                                    elevation = CardDefaults.cardElevation(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                                    )
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.heightIn(max = 200.dp)
                                    ) {
                                        items(searchResults) { city ->
                                            SearchSuggestionItem(city = city) {
                                                viewModel.selectCity(city)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Weather UI details container based on current State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = uiState,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(400))
                            },
                            label = "weather_state_animation"
                        ) { state ->
                            when (state) {
                                is WeatherUiState.Loading -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Analyzing regional skylines...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                is WeatherUiState.Success -> {
                                    WeatherContent(
                                        weather = state.weather,
                                        tempUnit = tempUnit,
                                        onToggleUnit = { viewModel.toggleTempUnit() }
                                    )
                                }
                                is WeatherUiState.Error -> {
                                    ErrorStateView(
                                        message = state.message,
                                        onRetry = { viewModel.refreshWeather() }
                                    )
                                }
                                is WeatherUiState.Idle -> {
                                    Text(text = "Initializing location sensors...")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSuggestionItem(
    city: GeocodingResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = city.name,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = listOfNotNull(city.state, city.country).joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun WeatherContent(
    weather: WeatherInfo,
    tempUnit: TemperatureUnit,
    onToggleUnit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("weather_dashboard_content"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Hero City Details Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_weather_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // City & State metadata
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = weather.cityName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animated Breathing weather icon
                    val infiniteTransition = rememberInfiniteTransition(label = "icon_transition")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "weather_pulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .rotate(pulseScale * 3f)
                    ) {
                        Icon(
                            imageVector = weather.icon,
                            contentDescription = weather.conditionDesc,
                            tint = getAtmosphericIconColor(weather.weatherCode),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Display temp toggle values
                    val tempValue = when (tempUnit) {
                        TemperatureUnit.CELSIUS -> "${weather.temperatureCelsius.toInt()}°C"
                        TemperatureUnit.FAHRENHEIT -> "${weather.temperatureFahrenheit.toInt()}°F"
                    }

                    Text(
                        text = tempValue,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 54.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("temperature_text")
                    )

                    Text(
                        text = weather.conditionDesc,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick temperature unit conversion switch
                    Button(
                        onClick = onToggleUnit,
                        modifier = Modifier.testTag("unit_toggle_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (tempUnit == TemperatureUnit.CELSIUS) "Switch to Fahrenheit" else "Switch to Celsius",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Expanded Metric cards grid
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary feels like card
                val feltValue = when (tempUnit) {
                    TemperatureUnit.CELSIUS -> "${weather.feelsLikeCelsius.toInt()}°C"
                    TemperatureUnit.FAHRENHEIT -> "${weather.feelsLikeFahrenheit.toInt()}°F"
                }
                MetricCard(
                    title = "Feels Like",
                    value = feltValue,
                    description = "Thermal apparent temperature sensation",
                    icon = Icons.Rounded.Thermostat,
                    iconColor = Color(0xFFEF5350),
                    testTag = "feels_like_metric"
                )

                // Humidity index
                MetricCard(
                    title = "Humidity",
                    value = "${weather.relativeHumidity.toInt()}%",
                    description = "Relative water vapor density in air",
                    icon = Icons.Rounded.WaterDrop,
                    iconColor = Color(0xFF42A5F5),
                    testTag = "humidity_metric"
                )

                // Air velocity speed card
                MetricCard(
                    title = "Wind Velocity",
                    value = "${weather.windSpeedKmh.toInt()} km/h",
                    description = "Speed of wind currents measured at 10m",
                    icon = Icons.Rounded.Air,
                    iconColor = Color(0xFF26A69A),
                    testTag = "wind_metric"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("error_view_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Connection Offline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Retry Sync", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Determine dynamic gradients for top and bottom background brushes based on current weather condition.
 */
@Composable
fun determineAtmosphericGradient(state: WeatherUiState, isDark: Boolean): List<Color> {
    if (state !is WeatherUiState.Success) {
        return if (isDark) {
            listOf(Color(0xFF1A237E), Color(0xFF121212))
        } else {
            listOf(Color(0xFFE3F2FD), Color(0xFFECEFF1))
        }
    }

    val code = state.weather.weatherCode
    return if (isDark) {
        // High fidelity dark theme adaptive atmosphere
        when (code) {
            0 -> listOf(Color(0xFFE65100), Color(0xFF0F0F1A)) // Dark sunset clear sky
            1, 2, 3 -> listOf(Color(0xFF37474F), Color(0xFF121212)) // Dark cloudy slate
            45, 48 -> listOf(Color(0xFF455A64), Color(0xFF1E272C)) // Dark misty shroud
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> listOf(Color(0xFF006064), Color(0xFF0C191D)) // Deep subsea rainy teals
            71, 73, 75, 77, 85, 86 -> listOf(Color(0xFF303F9F), Color(0xFF10142C)) // Crisp frosty midnight indigo
            95, 96, 99 -> listOf(Color(0xFF4A148C), Color(0xFF080014)) // Ominous celestial purples
            else -> listOf(Color(0xFF1A1F2C), Color(0xFF121212))
        }
    } else {
        // High fidelity light theme adaptive atmosphere
        when (code) {
            0 -> listOf(Color(0xFFFFF9C4), Color(0xFFFFB300)) // Solar clear sky
            1, 2, 3 -> listOf(Color(0xFFECEFF1), Color(0xFF90A4AE)) // Cloud slate
            45, 48 -> listOf(Color(0xFFCFD8DC), Color(0xFFB0BEC5)) // Atmospheric foggy shroud
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> listOf(Color(0xFFE0F7FA), Color(0xFF26C6DA)) // Misty cobalt rainy teals
            71, 73, 75, 77, 85, 86 -> listOf(Color(0xFFE8EAF6), Color(0xFF7986CB)) // Clean glacial blue
            95, 96, 99 -> listOf(Color(0xFFEDE7F6), Color(0xFF7E57C2)) // Electric summer indigo purple
            else -> listOf(Color(0xFFF5F5F5), Color(0xFFCFD8DC))
        }
    }
}

/**
 * Determine aesthetic icon tints mapped to specific conditions
 */
@Composable
fun getAtmosphericIconColor(code: Int): Color {
    return when (code) {
        0 -> Color(0xFFFBC02D) // Solar Yellow
        1, 2, 3 -> Color(0xFF78909C) // Soft Slate Blue
        45, 48 -> Color(0xFF90A4AE) // Heavy Fog Gray
        51, 53, 55 -> Color(0xFF29B6F6) // Light drizzle cyan
        61, 63, 65, 80, 81, 82 -> Color(0xFF1E88E5) // Rainy sapphire blue
        71, 73, 75, 77, 85, 86 -> Color(0xFF4FC3F7) // Glacial frosty ice-blue
        95, 96, 99 -> Color(0xFFAB47BC) // Electrical purple storm
        else -> Color(0xFF78909C)
    }
}
