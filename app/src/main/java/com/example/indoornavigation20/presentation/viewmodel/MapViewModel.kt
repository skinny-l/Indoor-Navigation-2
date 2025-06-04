package com.example.indoornavigation20.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.indoornavigation20.data.repository.NavigationRepository
import com.example.indoornavigation20.data.repository.Result
import com.example.indoornavigation20.domain.model.*
import com.example.indoornavigation20.navigation.NavigationPath
import com.example.indoornavigation20.navigation.PathfindingEngine
import com.example.indoornavigation20.positioning.PositioningEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUiState(
    val isLoading: Boolean = false,
    val currentFloor: Int = 1,
    val availableFloors: List<Int> = listOf(1),
    val currentPosition: Position? = null,
    val selectedPOI: PointOfInterest? = null,
    val searchResults: List<PointOfInterest> = emptyList(),
    val currentFloorPlan: FloorPlan? = null,
    val navigationPath: NavigationPath? = null,
    val building: Building? = null,
    val errorMessage: String? = null,
    val signalStrength: com.example.indoornavigation20.positioning.SignalStrength = com.example.indoornavigation20.positioning.SignalStrength.UNAVAILABLE
)

class MapViewModel(private val context: Context? = null) : ViewModel() {
    private val repository = NavigationRepository()
    private val pathfindingEngine = PathfindingEngine()
    private val positioningEngine = PositioningEngine(context)

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val floorPlans = mutableMapOf<Int, FloorPlan>()

    init {
        loadBuildingData()
        startPositioning()
    }

    private fun startPositioning() {
        // Start positioning service
        positioningEngine.startPositioning()

        // Setup known beacons
        positioningEngine.setupKnownBeacons()

        // Observe position updates
        viewModelScope.launch {
            positioningEngine.currentPosition.collect { position ->
                val previousPosition = _uiState.value.currentPosition
                _uiState.value = _uiState.value.copy(currentPosition = position)

                // Clear navigation path when position becomes unavailable
                if (position == null && previousPosition != null) {
                    _uiState.value = _uiState.value.copy(navigationPath = null)
                }

                // If we have a selected POI and got a position, try to navigate
                position?.let { pos ->
                    _uiState.value.selectedPOI?.let { poi ->
                        if (pos.floor == poi.position.floor) {
                            navigateTo(poi.position)
                        }
                    }
                }
            }
        }

        // Observe signal strength updates
        viewModelScope.launch {
            positioningEngine.signalStrength.collect { strength ->
                _uiState.value = _uiState.value.copy(signalStrength = strength)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        positioningEngine.stopPositioning()
    }

    private fun loadBuildingData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load building information
                val building = repository.getBuildingInfo("computer_science_building")

                // Load floor plans
                repository.getFloorPlans("computer_science_building").collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }

                        is Result.Success -> {
                            result.data.forEach { floorPlan ->
                                floorPlans[floorPlan.floorNumber] = floorPlan
                            }

                            val currentFloorPlan = floorPlans[_uiState.value.currentFloor]
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                currentFloorPlan = currentFloorPlan,
                                availableFloors = floorPlans.keys.sorted(),
                                building = building
                            )
                        }

                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = result.exception.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun selectPOI(poi: PointOfInterest) {
        _uiState.value = _uiState.value.copy(selectedPOI = poi)

        // Switch to the floor containing the POI
        if (poi.position.floor != _uiState.value.currentFloor) {
            selectFloor(poi.position.floor)
        }

        // ALWAYS attempt to navigate.
        // navigateTo will use a demo start if currentPosition is null.
        navigateTo(poi.position)
    }

    fun updateCurrentPosition(position: Position) {
        _uiState.value = _uiState.value.copy(currentPosition = position)

        // If we have a selected POI and switched floors, try to navigate
        _uiState.value.selectedPOI?.let { poi ->
            if (position.floor == poi.position.floor) {
                navigateTo(poi.position)
            }
        }
    }

    fun selectFloor(floor: Int) {
        val floorPlan = floorPlans[floor]
        if (floorPlan != null) {
            _uiState.value = _uiState.value.copy(
                currentFloor = floor,
                currentFloorPlan = floorPlan
            )
        }
    }

    fun searchPOIs(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        viewModelScope.launch {
            repository.searchPOIs(query).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            searchResults = result.data
                        )
                    }

                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.exception.message
                        )
                    }

                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            }
        }
    }

    fun navigateTo(destination: Position) {
        val currentPosition = _uiState.value.currentPosition
        if (currentPosition == null) {
            // For testing purposes, use a demo start position when no real position is available
            // Using ENTRANCE_MAIN_SOUTH from the user's latest node list
            val demoPosition = Position(
                x = 775f,
                y = 835f,
                floor = 1
            ) // Updated to match user's ENTRANCE_MAIN_SOUTH
            println("🧪 Using demo position for testing: (${demoPosition.x}, ${demoPosition.y})")

            viewModelScope.launch {
                try {
                    val path = pathfindingEngine.findPath(
                        start = demoPosition,
                        goal = destination,
                        floorPlans = floorPlans
                    )

                    _uiState.value = _uiState.value.copy(
                        navigationPath = path,
                        errorMessage = "Demo route from main entrance (you are currently outside)"
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Navigation failed: ${e.message}"
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            try {
                val path = pathfindingEngine.findPath(
                    start = currentPosition,
                    goal = destination,
                    floorPlans = floorPlans
                )

                _uiState.value = _uiState.value.copy(navigationPath = path)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Navigation failed: ${e.message}"
                )
            }
        }
    }

    fun centerOnCurrentLocation() {
        val currentPos = _uiState.value.currentPosition
        if (currentPos != null) {
            // In a real implementation, this would trigger the map component to center on position
            // For now, we'll update the UI to show the position is being centered
            _uiState.value = _uiState.value.copy(
                errorMessage = "Centered on position: Floor ${currentPos.floor}, X: ${currentPos.x.toInt()}, Y: ${currentPos.y.toInt()}"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Current position not available. Make sure positioning is enabled."
            )
        }
    }

    fun clearNavigation() {
        _uiState.value = _uiState.value.copy(
            navigationPath = null,
            selectedPOI = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Convert pixel coordinates to real-world coordinates
    fun pixelToGeoCoordinate(
        pixelX: Float,
        pixelY: Float,
        floorPlan: FloorPlan
    ): Pair<Double, Double>? {
        val coordinateSystem = floorPlan.coordinateSystem ?: return null

        val deltaX = pixelX * coordinateSystem.metersPerPixelX
        val deltaY = pixelY * coordinateSystem.metersPerPixelY

        // Convert meters to lat/lng (approximate)
        val deltaLat = deltaY / 111000.0 // 1 degree latitude ≈ 111km
        val deltaLng =
            deltaX / (111000.0 * kotlin.math.cos(Math.toRadians(coordinateSystem.originLatitude)))

        return Pair(
            coordinateSystem.originLatitude + deltaLat,
            coordinateSystem.originLongitude + deltaLng
        )
    }

    // Convert real-world coordinates to pixel coordinates
    fun geoToPixelCoordinate(
        latitude: Double,
        longitude: Double,
        floorPlan: FloorPlan
    ): Pair<Float, Float>? {
        val coordinateSystem = floorPlan.coordinateSystem ?: return null

        val deltaLat = latitude - coordinateSystem.originLatitude
        val deltaLng = longitude - coordinateSystem.originLongitude

        // Convert lat/lng to meters (approximate)
        val deltaX =
            deltaLng * 111000.0 * kotlin.math.cos(Math.toRadians(coordinateSystem.originLatitude))
        val deltaY = deltaLat * 111000.0

        val pixelX = (deltaX / coordinateSystem.metersPerPixelX).toFloat()
        val pixelY = (deltaY / coordinateSystem.metersPerPixelY).toFloat()

        return Pair(pixelX, pixelY)
    }

    fun getRoomAtPosition(position: Position): Room? {
        val floorPlan = floorPlans[position.floor] ?: return null

        return floorPlan.rooms.find { room ->
            position.x >= room.bounds.topLeft.x &&
                    position.x <= room.bounds.bottomRight.x &&
                    position.y >= room.bounds.topLeft.y &&
                    position.y <= room.bounds.bottomRight.y
        }
    }
}
