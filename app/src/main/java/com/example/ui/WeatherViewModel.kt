package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GeocodingResult
import com.example.data.model.WeatherInfo
import com.example.data.repository.WeatherRepository
import com.example.utils.LocationTracker
import com.example.utils.DefaultLocationTracker
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(val weather: WeatherInfo) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

enum class TemperatureUnit {
    CELSIUS, FAHRENHEIT
}

@OptIn(FlowPreview::class)
class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(application)
    private val locationTracker: LocationTracker = DefaultLocationTracker(
        application,
        LocationServices.getFusedLocationProviderClient(application)
    )

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading: StateFlow<Boolean> = _isSearchLoading.asStateFlow()

    private val _tempUnit = MutableStateFlow(TemperatureUnit.CELSIUS)
    val tempUnit: StateFlow<TemperatureUnit> = _tempUnit.asStateFlow()

    // Keep track of last successful coordinates to handle manual refresh accurately
    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private var lastCityName: String? = null

    init {
        // Debounce manual search terms to prevent duplicate rapid API calls
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { query ->
                    _isSearchLoading.value = true
                    val results = repository.searchCities(query)
                    _searchResults.value = results
                    _isSearchLoading.value = false
                }
        }

        viewModelScope.launch {
            _searchQuery
                .filter { it.length < 2 }
                .collect {
                    _searchResults.value = emptyList()
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleTempUnit() {
        _tempUnit.value = if (_tempUnit.value == TemperatureUnit.CELSIUS) {
            TemperatureUnit.FAHRENHEIT
        } else {
            TemperatureUnit.CELSIUS
        }
    }

    fun fetchWeatherFromLocation() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val location = locationTracker.getCurrentLocation()
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    lastLat = lat
                    lastLng = lng
                    lastCityName = null // Reverse geocode it directly in repository

                    val weather = repository.fetchWeather(lat, lng)
                    _uiState.value = WeatherUiState.Success(weather)
                } else {
                    // Coordinates unavailable (e.g., GPS disabled or permissions denied)
                    // Gracefully fallback to a famous central default city for a perfect out-of-box experience!
                    val defaultLat = 48.8566 // Paris
                    val defaultLng = 2.3522
                    lastLat = defaultLat
                    lastLng = defaultLng
                    lastCityName = "Paris, France"

                    val weather = repository.fetchWeather(defaultLat, defaultLng, lastCityName)
                    _uiState.value = WeatherUiState.Success(weather)
                }
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(
                    e.localizedMessage ?: "Failed to retrieve location weather information. Check network connection."
                )
            }
        }
    }

    fun selectCity(city: GeocodingResult) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            _searchQuery.value = "" // Clear query to hide dropdown
            _searchResults.value = emptyList()
            try {
                lastLat = city.latitude
                lastLng = city.longitude
                lastCityName = "${city.name}, ${city.country ?: ""}"

                val weather = repository.fetchWeather(city.latitude, city.longitude, lastCityName)
                _uiState.value = WeatherUiState.Success(weather)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(
                    e.localizedMessage ?: "Failed to load weather for ${city.name}."
                )
            }
        }
    }

    fun refreshWeather() {
        val lat = lastLat
        val lng = lastLng
        if (lat != null && lng != null) {
            viewModelScope.launch {
                _uiState.value = WeatherUiState.Loading
                try {
                    val weather = repository.fetchWeather(lat, lng, lastCityName)
                    _uiState.value = WeatherUiState.Success(weather)
                } catch (e: Exception) {
                    _uiState.value = WeatherUiState.Error(
                        e.localizedMessage ?: "Failed to update weather information. Check internet connection."
                    )
                }
            }
        } else {
            fetchWeatherFromLocation()
        }
    }
}
