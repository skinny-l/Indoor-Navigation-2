package com.example.indoornavigation20.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.indoornavigation20.data.repository.NavigationRepository
import com.example.indoornavigation20.data.repository.Result
import com.example.indoornavigation20.data.repository.UserManager
import com.example.indoornavigation20.domain.model.*
import com.example.indoornavigation20.navigation.NavigationPath
import com.example.indoornavigation20.navigation.PathfindingEngine
import com.example.indoornavigation20.positioning.PositioningEngine
import com.example.indoornavigation20.positioning.BuildingDetector
import kotlinx.coroutines.Dispatchers
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
    val signalStrength: com.example.indoornavigation20.positioning.SignalStrength = com.example.indoornavigation20.positioning.SignalStrength.UNAVAILABLE,
    // Node management state
    val userNodes: List<NavNode> = emptyList(),
    val selectedNode: NavNode? = null,
    val isNodePlacementMode: Boolean = false
)

class MapViewModel(private val context: Context? = null) : ViewModel() {
    private val repository = NavigationRepository()
    private val userManager = UserManager()
    private val pathfindingEngine = PathfindingEngine()
    private val positioningEngine = PositioningEngine(context)
    private val buildingDetector: BuildingDetector? = context?.let { BuildingDetector(it) }

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val floorPlans = mutableMapOf<Int, FloorPlan>()

    // Cache entrance POIs for quick access
    private var entrancePOIs = listOf<PointOfInterest>()
    private var selectedEntrancePOI: PointOfInterest? = null

    init {
        loadBuildingData()
        startPositioning()
        startBuildingDetection()
        loadUserNodes()
    }

    private fun startBuildingDetection() {
        buildingDetector?.let { detector ->
            detector.startDetection()

            // Connect building detector to positioning engine
            positioningEngine.setBuildingDetector(detector)

            // Observe building status changes
            viewModelScope.launch {
                detector.isInsideBuilding.collect { isInside ->
                    println("🏢 Building status changed: ${if (isInside) "INSIDE" else "OUTSIDE"}")
                    // If user goes outside, clear current position to avoid confusion
                    if (!isInside) {
                        // Position will be cleared automatically by positioning engine now
                        println("🚪 User detected outside - indoor position will be cleared")
                    }
                }
            }

            // Observe indoor positioning to update building detector
            viewModelScope.launch {
                positioningEngine.currentPosition.collect { position ->
                    detector.updateIndoorPosition(position)
                }
            }

            // Observe beacon signals to update building detector
            viewModelScope.launch {
                positioningEngine.beaconsUsedInLastPositioning.collect { beacons ->
                    val averageRssi = if (beacons.isNotEmpty()) {
                        beacons.map { it.rssi }.average()
                    } else {
                        -100.0
                    }
                    detector.updateBeaconSignals(beacons.size, averageRssi)
                }
            }
        }
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
                val updatedState = _uiState.value.copy(currentPosition = position)
                _uiState.value = updatedState

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
        buildingDetector?.stopDetection()
    }

    private fun loadBuildingData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load building information
                val building = repository.getBuildingInfo("computer_science_building")

                // Load entrance POIs first
                loadEntrancePOIs()

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

                            // CRITICAL: Refresh floor plans when nodes are loaded
                            refreshFloorPlans()
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

    private suspend fun loadEntrancePOIs() {
        repository.searchPOIs("entrance").collect { result ->
            when (result) {
                is Result.Success -> {
                    entrancePOIs = result.data.filter { it.category == POICategory.ENTRANCE }
                    println("🚪 Loaded ${entrancePOIs.size} entrance POIs:")

                    // Print detailed info about each entrance
                    entrancePOIs.forEach { entrance ->
                        println("   - ${entrance.name}: (${entrance.position.x}, ${entrance.position.y}) floor ${entrance.position.floor}")
                    }

                    // Auto-select the first entrance for testing if none selected
                    if (selectedEntrancePOI == null && entrancePOIs.isNotEmpty()) {
                        selectedEntrancePOI = entrancePOIs.first()
                        println("🎯 Auto-selected entrance: ${selectedEntrancePOI?.name} at (${selectedEntrancePOI?.position?.x}, ${selectedEntrancePOI?.position?.y})")
                    }
                }

                is Result.Error -> {
                    println("❌ Failed to load entrance POIs: ${result.exception.message}")
                }

                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    private fun loadUserNodes() {
        viewModelScope.launch {
            try {
                val nodes = repository.getUserNodes()
                _uiState.value = _uiState.value.copy(userNodes = nodes)
                println("Loaded ${nodes.size} user nodes")
            } catch (e: Exception) {
                println("Failed to load user nodes: ${e.message}")
            }
        }
    }

    fun forceReloadAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val result = repository.forceReloadAllData()
                loadUserNodes()// Refresh the UI
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Force reload failed: ${e.message}"
                )
            }
        }
    }

    fun connectAllExistingNodes() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // FORCE reload nodes from Firebase first
                repository.reloadNodes()

                // Wait for Firebase, respond
                kotlinx.coroutines.delay(3000)

                // Now get the nodes,


                val allNodes = repository.getUserNodes()
                val currentFloor = _uiState.value.currentFloor
                val nodesOnThisFloor = allNodes.filter { it.position.floor == currentFloor }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "🔍 DIAGNOSTIC: Found ${allNodes.size} total nodes, ${nodesOnThisFloor.size} on floor $currentFloor"
                )

                // Refresh, floor plans
                loadUserNodes()
                refreshFloorPlans()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "❌ Error: ${e.message}"
                )
            }
        }
    }

    // Helper function to calculate distance between positions
    private fun distance(pos1: Position, pos2: Position): Float {
        val dx = pos1.x - pos2.x
        val dy = pos1.y - pos2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
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
                        _uiState.value = _uiState.value.copy(searchResults = result.data)
                    }

                    is Result.Error -> {
                        _uiState.value =
                            _uiState.value.copy(errorMessage = result.exception.message)
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
        val isInside = buildingDetector?.isInsideBuilding?.value ?: false

        // Determine start position with better logic
        val startPosition = when {
            // Priority 1: Real indoor position when marked as inside
            currentPosition != null && isInside && currentPosition.accuracy <= 10f -> {
                println("📍 Using verified indoor position: (${currentPosition.x}, ${currentPosition.y}) accuracy: ${currentPosition.accuracy}m")
                currentPosition
            }

            // Priority 2: Use entrance when outside OR when inside but no accurate position
            else -> {
                val entrance = getCurrentSelectedEntrance()
                if (entrance != null) {
                    val status =
                        if (isInside) "inside but no accurate position" else "outside building"
                    println("🚪 User $status, starting from entrance: ${entrance.name} at (${entrance.position.x}, ${entrance.position.y})")
                    entrance.position
                } else {
                    // Show error instead of using demo position
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "❌ Cannot navigate: No entrance POIs available and no indoor position. Please add entrance POIs first."
                    )
                    return
                }
            }
        }

        viewModelScope.launch {
            try {
                val path = pathfindingEngine.findPath(
                    start = startPosition,
                    goal = destination,
                    floorPlans = floorPlans
                )

                val statusMessage = when {
                    currentPosition != null && isInside && currentPosition.accuracy <= 10f ->
                        "📍 Route from your current location (${currentPosition.accuracy.toInt()}m accuracy)"

                    isInside && (currentPosition == null || currentPosition.accuracy > 10f) -> {
                        val entranceName = getCurrentSelectedEntrance()?.name ?: "entrance"
                        "🚪 Using $entranceName as start (indoor position unavailable/inaccurate)"
                    }

                    !isInside -> {
                        val entranceName = getCurrentSelectedEntrance()?.name ?: "entrance"
                        "🚪 Route from $entranceName (you are outside building)"
                    }

                    else ->
                        "📍 Navigation route calculated"
                }

                _uiState.value = _uiState.value.copy(
                    navigationPath = path,
                    errorMessage = statusMessage
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Navigation failed: ${e.message}"
                )
            }
        }
    }

    private fun getCurrentSelectedEntrance(): PointOfInterest? {
        return selectedEntrancePOI ?: entrancePOIs.firstOrNull()
    }

    /**
     * Select which entrance to use as starting point for routing when user is outside
     */
    fun selectEntrance(entrance: PointOfInterest) {
        if (entrance.category == POICategory.ENTRANCE) {
            selectedEntrancePOI = entrance
            println("🎯 Selected entrance: ${entrance.name} at (${entrance.position.x}, ${entrance.position.y})")

            // If we have a selected POI, recalculate route from new entrance
            _uiState.value.selectedPOI?.let { poi ->
                navigateTo(poi.position)
            }
        }
    }

    /**
     * Get available entrances for selection
     */
    fun getAvailableEntrances(): List<PointOfInterest> {
        return entrancePOIs
    }

    /**
     * Get currently selected entrance
     */
    fun getSelectedEntranceForTesting(): PointOfInterest? {
        return selectedEntrancePOI
    }

    /**
     * For testing: manually set whether user is inside or outside
     */
    fun setTestingInsideBuilding(isInside: Boolean) {
        buildingDetector?.setTestingMode(isInside, "Manual testing override")
        println("🧪 Testing mode: User is ${if (isInside) "INSIDE" else "OUTSIDE"} building")

        // Recalculate route if we have a selected POI
        _uiState.value.selectedPOI?.let { poi ->
            navigateTo(poi.position)
        }
    }

    /**
     * Check if user is currently inside building
     */
    fun isUserInsideBuilding(): Boolean {
        return buildingDetector?.isInsideBuilding?.value ?: false
    }

    /**
     * Get building detection method for debugging
     */
    fun getBuildingDetectionMethod(): String {
        return buildingDetector?.detectionMethod?.value ?: "Unknown"
    }

    fun centerOnCurrentLocations() {
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

    // POI Management Methods for Admin Mode
    fun addPOI(name: String, x: Float, y: Float, category: POICategory) {
        viewModelScope.launch {
            try {
                // Check if user has permission to add POIs
                val permissions = userManager.getUserPermissions()
                if (permissions.canAddPOI) {
                    val newPOI = repository.addPOI(name, x, y, category)
                    // Refresh POI search to update the, PO
                    searchPOIs("")
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "You don't have permission to add POIs"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to add POI: ${e.message}"
                )
            }
        }
    }

    fun updatePOIPosition(poiId: String, x: Float, y: Float) {
        viewModelScope.launch {
            try {
                // Check if user has permission to update POIs
                val permissions = userManager.getUserPermissions()
                if (permissions.canEditPOI) {
                    val success = repository.updatePOIPosition(poiId, x, y)
                    if (success) {
                        // Refresh POI search to update the, PO
                        searchPOIs("")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to update POI position"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "You don't have permission to update POIs"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update POI position: ${e.message}"
                )
            }
        }
    }

    fun deletePOI(poiId: String) {
        viewModelScope.launch {
            try {
                // Check if user has permission to delete POIs
                val permissions = userManager.getUserPermissions()
                if (permissions.canDeletePOI) {
                    val success = repository.deletePOI(poiId)
                    if (success) {
                        // Clear selection if the deleted POI was selected
                        if (_uiState.value.selectedPOI?.id == poiId) {
                            _uiState.value = _uiState.value.copy(selectedPOI = null)
                        }
                        // Refresh POI search to update the, PO
                        searchPOIs("")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to delete POI"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "You don't have permission to delete POIs"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete POI: ${e.message}"
                )
            }
        }
    }

    fun getAllPOIs() {
        searchPOIs("")
    }

    suspend fun getUserPermissions(): UserPermissions {
        return userManager.getUserPermissions()
    }

    fun reloadPOIs() {
        repository.reloadPOIs()
        // Also refresh the search, update the, UI
        searchPOIs("")
    }

    // Node management methods
    fun selectNode(node: NavNode) {
        _uiState.value = _uiState.value.copy(selectedNode = node)
    }

    fun toggleNodePlacementMode() {
        _uiState.value = _uiState.value.copy(
            isNodePlacementMode = !_uiState.value.isNodePlacementMode,
            selectedNode = null
        )
    }

    fun addNode(x: Float, y: Float) {
        viewModelScope.launch {
            try {
                val permissions = userManager.getUserPermissions()
                if (permissions.canAddPOI) { // Reusing POI permissions for nodes
                    val newNode = repository.addNode(x, y)
                    loadUserNodes()// Refresh nodes
                    _uiState.value = _uiState.value.copy(
                        selectedNode = newNode,
                        errorMessage = "Navigation node added at (${x.toInt()}, ${y.toInt()})"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "You don't have permission to add navigation nodes"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to add navigation node: ${e.message}"
                )
            }
        }
    }

    fun updateNodePosition(nodeId: String, x: Float, y: Float) {
        viewModelScope.launch {
            try {
                val permissions = userManager.getUserPermissions()
                if (permissions.canEditPOI) { // Reusing POI permissions for nodes
                    val success = repository.updateNodePosition(nodeId, x, y)
                    if (success) {
                        loadUserNodes()// Refresh nodes
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Node position updated"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to update node position"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "You don't have permission to update navigation nodes"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update node position: ${e.message}"
                )
            }
        }
    }

    fun deleteNode(nodeId: String) {
        viewModelScope.launch {
            try {
                val permissions = userManager.getUserPermissions()
                if (permissions.canDeletePOI) { // Reusing POI permissions for nodes
                    val success = repository.deleteNode(nodeId)
                    if (success) {
                        // Clear selection if the deleted node was selected
                        if (_uiState.value.selectedNode?.id == nodeId) {
                            _uiState.value = _uiState.value.copy(selectedNode = null)
                        }
                        loadUserNodes()// Refresh nodes
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Navigation node deleted"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to delete navigation node"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "You don't have permission to delete navigation nodes"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete navigation node: ${e.message}"
                )
            }
        }
    }

    fun updateNodeConnections(nodeId: String, connectedNodeIds: List<String>) {
        viewModelScope.launch {
            try {
                val permissions = userManager.getUserPermissions()
                if (permissions.canEditPOI) { // Reusing POI permissions for nodes
                    val success = repository.updateNodeConnections(nodeId, connectedNodeIds)
                    if (success) {
                        loadUserNodes()// Refresh nodes
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Node connections updated"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to update node connections"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "You don't have permission to update navigation nodes"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update node connections: ${e.message}"
                )
            }
        }
    }

    fun reloadNodes() {
        repository.reloadNodes()
        loadUserNodes()
    }

    fun getNodeStorageInfo(): String {
        return repository.getNodeStorageInfo()
    }

    fun showNodeStorageInfo() {
        val storageInfo = repository.getNodeStorageInfo()
        _uiState.value = _uiState.value.copy(errorMessage = storageInfo)
    }

    // Add this new function to refresh floor plans with updated nodes
    private fun refreshFloorPlans() {
        viewModelScope.launch {
            try {
                // Force refresh floor plans with current nodes
                repository.getFloorPlans("computer_science_building").collect { result ->
                    when (result) {
                        is Result.Success -> {
                            result.data.forEach { floorPlan ->
                                floorPlans[floorPlan.floorNumber] = floorPlan
                            }

                            val currentFloorPlan = floorPlans[_uiState.value.currentFloor]
                            _uiState.value = _uiState.value.copy(
                                currentFloorPlan = currentFloorPlan
                            )
                        }

                        else -> { /* ignore */
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore refresh errors
            }
        }
    }

    // Enhanced method to connect all nodes with better feedback
    fun connectAllNodesEnhanced() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value =
                _uiState.value.copy(isLoading = true, errorMessage = "🔄 Connecting nodes...")

            try {
                // First validate and fix any broken connections,
                val validationResult = repository.validateAndFixConnections()
                println(validationResult)

                // Then connect all nodes
                val connectionResult = repository.connectAllNodesImproved()

                // Reload nodes, refresh floor plans,
                loadUserNodes()
                refreshFloorPlans()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = connectionResult
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "❌ Connection failed: ${e.message}"
                )
            }
        }
    }

    // Method to rebuild the, connection network
    fun rebuildConnectionNetwork() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = "🔄 Rebuilding connection network..."
            )

            try {
                val result = repository.rebuildConnectionNetwork()

                // Reload everything,
                loadUserNodes()
                refreshFloorPlans()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "❌ Rebuild failed: ${e.message}"
                )
            }
        }
    }

    // Enhanced add node method with better connection handling
    fun addNodeEnhanced(x: Float, y: Float) {
        viewModelScope.launch {
            try {
                val permissions = userManager.getUserPermissions()
                if (permissions.canAddPOI) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "🔄 Adding node and establishing connections..."
                    )

                    val newNode = repository.addNodeWithSmartConnection(x, y)

                    // Refresh, floor plans,
                    loadUserNodes()
                    refreshFloorPlans()

                    _uiState.value = _uiState.value.copy(
                        selectedNode = newNode,
                        errorMessage = "✅ Node added at (${x.toInt()}, ${y.toInt()}) with ${newNode.connections.size} connections"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "❌ You don't have permission to add navigation nodes"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "❌ Failed to add navigation node: ${e.message}"
                )
            }
        }
    }

    // Method to test pathfinding between two specific nodes
    fun testPathfinding(startNodeId: String, goalNodeId: String) {
        viewModelScope.launch {
            try {
                val nodes = _uiState.value.userNodes
                val startNode = nodes.find { it.id == startNodeId }
                val goalNode = nodes.find { it.id == goalNodeId }

                if (startNode == null || goalNode == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "❌ Could not find specified nodes"
                    )
                    return@launch
                }

                val startPos = startNode.position
                val goalPos = goalNode.position

                _uiState.value = _uiState.value.copy(
                    errorMessage = "🧪 Testing pathfinding from ${startNode.id.takeLast(4)} to ${
                        goalNode.id.takeLast(
                            4
                        )
                    }"
                )

                val path = pathfindingEngine.findPath(
                    start = startPos,
                    goal = goalPos,
                    floorPlans = floorPlans
                )

                _uiState.value = _uiState.value.copy(
                    navigationPath = path,
                    errorMessage = "✅ Test path found with ${path.steps.size} steps"
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "❌ Pathfinding test failed: ${e.message}"
                )
            }
        }
    }

    // Enhanced navigation method with better debugging
    fun navigateToEnhanced(destination: Position) {
        val currentPosition = _uiState.value.currentPosition
        val isInside = buildingDetector?.isInsideBuilding?.value ?: false

        // Determine start position with better logic
        val startPosition = when {
            currentPosition != null && isInside && currentPosition.accuracy <= 10f -> {
                println("📍 Using verified indoor position: (${currentPosition.x}, ${currentPosition.y}) accuracy: ${currentPosition.accuracy}m")
                currentPosition
            }

            else -> {
                val entrance = getCurrentSelectedEntrance()
                if (entrance != null) {
                    val status =
                        if (isInside) "inside but no accurate position" else "outside building"
                    println("🚪 User $status, starting from entrance: ${entrance.name} at (${entrance.position.x}, ${entrance.position.y})")
                    entrance.position
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "❌ Cannot navigate: No entrance POIs available and no indoor position. Please add entrance POIs first."
                    )
                    return
                }
            }
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "🧭 Calculating route..."
                )

                // Debug: Check available nodes
                val currentFloorPlan = floorPlans[startPosition.floor]
                val nodeCount = currentFloorPlan?.nodes?.size ?: 0
                println("🗺️ Available nodes on floor ${startPosition.floor}: $nodeCount")

                val path = pathfindingEngine.findPath(
                    start = startPosition,
                    goal = destination,
                    floorPlans = floorPlans
                )

                val statusMessage = when {
                    currentPosition != null && isInside && currentPosition.accuracy <= 10f ->
                        "📍 Route from your current location (${currentPosition.accuracy.toInt()}m accuracy)"

                    isInside && (currentPosition == null || currentPosition.accuracy > 10f) -> {
                        val entranceName = getCurrentSelectedEntrance()?.name ?: "entrance"
                        "🚪 Using $entranceName as start (indoor position unavailable/inaccurate)"
                    }

                    !isInside -> {
                        val entranceName = getCurrentSelectedEntrance()?.name ?: "entrance"
                        "🚪 Route from $entranceName (you are outside building)"
                    }

                    else -> "📍 Navigation route calculated"
                }

                _uiState.value = _uiState.value.copy(
                    navigationPath = path,
                    errorMessage = "$statusMessage - ${path.steps.size} steps, ${path.totalDistance.toInt()}px total"
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "❌ Navigation failed: ${e.message}"
                )
            }
        }
    }

    // Method to get detailed pathfinding diagnostics
    fun getDiagnostics(): String {
        val currentFloor = _uiState.value.currentFloor
        val floorPlan = floorPlans[currentFloor]
        val userNodes = _uiState.value.userNodes
        val nodesOnFloor = userNodes.filter { it.position.floor == currentFloor }

        val totalConnections = nodesOnFloor.sumOf { it.connections.size }
        val connectedNodes = nodesOnFloor.count { it.connections.isNotEmpty() }
        val isolatedNodes = nodesOnFloor.size - connectedNodes

        return """
        🔍 PATHFINDING DIAGNOSTICS - Floor $currentFloor
        
        📊 Node Statistics:
        • Total nodes on floor: ${nodesOnFloor.size}
        • Connected nodes: $connectedNodes
        • Isolated nodes: $isolatedNodes
        • Total connections: $totalConnections
        • Average connections: ${if (nodesOnFloor.isNotEmpty()) "%.1f".format(totalConnections.toFloat() / nodesOnFloor.size) else "0"}
        
        🗺️ Floor Plan Info:
        • Floor plan loaded: ${floorPlan != null}
        • Walls defined: ${floorPlan?.walls?.size ?: 0}
        • Rooms defined: ${floorPlan?.rooms?.size ?: 0}
        
        📱 Current State:
        • Current position: ${_uiState.value.currentPosition?.let { "(${it.x.toInt()}, ${it.y.toInt()})" } ?: "None"}
        • Selected POI: ${_uiState.value.selectedPOI?.name ?: "None"}
        • Navigation active: ${_uiState.value.navigationPath != null}
        
        🔗 Connection Details:
        ${
            nodesOnFloor.joinToString("\n") { node ->
                "• ${node.id.takeLast(8)}: (${node.position.x.toInt()}, ${node.position.y.toInt()}) → ${node.connections.size} connections"
            }
        }
        """.trimIndent()
    }

    // Method to show diagnostics in UI
    fun showDiagnostics() {
        val diagnostics = getDiagnostics()
        _uiState.value = _uiState.value.copy(errorMessage = diagnostics)
    }
}
