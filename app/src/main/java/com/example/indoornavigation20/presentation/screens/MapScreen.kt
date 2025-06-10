package com.example.indoornavigation20.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.indoornavigation20.domain.model.UserPermissions
import com.example.indoornavigation20.domain.model.PointOfInterest
import com.example.indoornavigation20.domain.model.Position
import com.example.indoornavigation20.presentation.components.FloorPlanViewer
import com.example.indoornavigation20.presentation.viewmodel.MapViewModel
import com.example.indoornavigation20.positioning.SignalStrength
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToPositioning: () -> Unit = {},
    onNavigateToBeacons: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: MapViewModel = remember { MapViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var userPermissions by remember {
        mutableStateOf<UserPermissions?>(
            null
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var showTestingControls by remember { mutableStateOf(false) }

    // Load user permissions
    LaunchedEffect(Unit) {
        userPermissions = viewModel.getUserPermissions()
    }

    // Add debug logging and periodic refresh
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000) // Wait 2 seconds after screen loads
        userPermissions = viewModel.getUserPermissions()
        android.util.Log.d("MapScreen", "User permissions: $userPermissions")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.building?.name ?: "Indoor Navigation",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "Search POI")
                    }
                    IconButton(onClick = {
                        // Refresh permissions
                        coroutineScope.launch {
                            userPermissions = viewModel.getUserPermissions()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        // Reload POIs from Firestore
                        viewModel.reloadPOIs()
                    }) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Reload POIs")
                    }
                    IconButton(onClick = {
                        // Reload nodes from Firestore
                        viewModel.reloadNodes()
                    }) {
                        Icon(Icons.Default.AccountTree, contentDescription = "Reload Nodes")
                    }
                    IconButton(onClick = {
                        // Connect all existing nodes
                        viewModel.connectAllExistingNodes()
                    }) {
                        Icon(Icons.Default.Link, contentDescription = "Connect All Nodes")
                    }

                    // Enhanced pathfinding test controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = {
                            viewModel.connectAllNodesEnhanced()
                        }) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Enhanced Connect")
                        }

                        IconButton(onClick = {
                            viewModel.rebuildConnectionNetwork()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rebuild Network")
                        }

                        IconButton(onClick = {
                            viewModel.showDiagnostics()
                        }) {
                            Icon(Icons.Default.Analytics, contentDescription = "Show Diagnostics")
                        }
                    }

                    IconButton(onClick = onNavigateToPositioning) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Positioning")
                    }
                    IconButton(onClick = onNavigateToBeacons) {
                        Icon(Icons.Default.Sensors, contentDescription = "Beacons")
                    }
                    IconButton(onClick = { showTestingControls = !showTestingControls }) {
                        Icon(Icons.Default.BugReport, contentDescription = "Testing Controls")
                    }
                    if (userPermissions?.canAccessAdmin == true) {
                        IconButton(onClick = onNavigateToAdmin) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Panel"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on map */ },
                    icon = { Icon(Icons.Default.Place, contentDescription = "Map") },
                    label = { Text("Map") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPositioning,
                    icon = { Icon(Icons.Default.MyLocation, contentDescription = "Position") },
                    label = { Text("Position") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToBeacons,
                    icon = { Icon(Icons.Default.Sensors, contentDescription = "Beacons") },
                    label = { Text("Beacons") }
                )
                if (userPermissions?.canAccessAdmin == true) {
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToAdmin,
                        icon = {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin"
                            )
                        },
                        label = { Text("Admin") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column {
                // Debug info card to show current user status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (userPermissions?.canAccessAdmin == true)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = if (userPermissions?.canAccessAdmin == true) {
                            "👑 ADMIN MODE - Full Access"
                        } else {
                            "👤 USER MODE - View Only"
                        },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Search Bar
                if (showSearch) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.searchPOIs(it)
                            },
                            label = { Text("Search rooms, labs, offices...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        viewModel.searchPOIs("")
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                // Floor selector
                if (uiState.availableFloors.size > 1) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Floor:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            uiState.availableFloors.forEach { floor ->
                                FilterChip(
                                    selected = floor == uiState.currentFloor,
                                    onClick = { viewModel.selectFloor(floor) },
                                    label = { Text("$floor") }
                                )
                            }
                        }
                    }
                }

                // Search Results
                if (showSearch && uiState.searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .heightIn(max = 200.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.searchResults) { poi ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    onClick = {
                                        viewModel.selectPOI(poi)
                                        showSearch = false
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = poi.name,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = poi.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Floor ${poi.position.floor} • ${poi.category.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Floor Plan Area
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading floor plan...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    // Add Status Cards to show building status and other info
                    StatusCards(
                        currentPosition = uiState.currentPosition,
                        selectedPOI = uiState.selectedPOI,
                        signalStrength = uiState.signalStrength,
                        isInsideBuilding = viewModel.isUserInsideBuilding(),
                        detectionMethod = viewModel.getBuildingDetectionMethod(),
                        selectedEntrance = viewModel.getSelectedEntranceForTesting()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        FloorPlanViewer(
                            floorPlan = uiState.currentFloorPlan,
                            currentPosition = uiState.currentPosition,
                            selectedPOI = uiState.selectedPOI,
                            pointsOfInterest = uiState.searchResults,
                            navigationPath = uiState.navigationPath,
                            onPOIClick = { poi -> viewModel.selectPOI(poi) },
                            // Node-related parameters
                            userNodes = uiState.userNodes,
                            selectedNode = uiState.selectedNode,
                            onNodeClick = { node -> viewModel.selectNode(node) },
                            onAddNode = { x, y -> viewModel.addNode(x, y) },
                            onMoveNode = { node, x, y ->
                                viewModel.updateNodePosition(
                                    node.id,
                                    x,
                                    y
                                )
                            },
                            isNodePlacementMode = uiState.isNodePlacementMode,
                            isAdminMode = userPermissions?.canAccessAdmin == true,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Signal strength indicator in top-right corner
                        SignalStrengthIndicator(
                            signalStrength = uiState.signalStrength,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        )

                        // POI management controls for admin users  
                        if (userPermissions?.canAccessAdmin == true && !uiState.isNodePlacementMode) {
                            POIManagementControls(
                                selectedPOI = uiState.selectedPOI,
                                onDeletePOI = { poiId -> viewModel.deletePOI(poiId) },
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            )
                        }

                        // Node placement controls for admin users
                        if (userPermissions?.canAccessAdmin == true) {
                            NodePlacementControls(
                                isNodePlacementMode = uiState.isNodePlacementMode,
                                selectedNode = uiState.selectedNode,
                                onToggleNodePlacement = { viewModel.toggleNodePlacementMode() },
                                onDeleteNode = { nodeId -> viewModel.deleteNode(nodeId) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                // Error message
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearError() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Building info
                uiState.building?.let { building ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Building Information",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${building.dimensions.width}m × ${building.dimensions.length}m",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Lat: ${building.coordinates.latitude}, Lng: ${building.coordinates.longitude}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (building.description.isNotEmpty()) {
                                Text(
                                    text = building.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Testing Controls Overlay
            if (showTestingControls) {
                TestingControlsOverlay(
                    viewModel = viewModel,
                    onDismiss = { showTestingControls = false }
                )
            }
        }
    }
}

@Composable
private fun TestingControlsOverlay(
    viewModel: MapViewModel,
    onDismiss: () -> Unit
) {
    val isInside = viewModel.isUserInsideBuilding()
    val detectionMethod = viewModel.getBuildingDetectionMethod()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Testing Controls",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Building Status Display
            Text(
                text = "Current Building Status",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isInside)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isInside) "INSIDE BUILDING" else "OUTSIDE BUILDING",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Detection Method: $detectionMethod",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = if (isInside) Icons.Default.Home else Icons.Default.OutdoorGrill,
                        contentDescription = if (isInside) "Inside Building" else "Outside Building",
                        modifier = Modifier.size(48.dp),
                        tint = if (isInside) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Building Status Controls
            Text(
                text = "Change Building Status",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.setTestingInsideBuilding(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Set Outside")
                }

                Button(
                    onClick = { viewModel.setTestingInsideBuilding(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Set Inside")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Entrance Selection
            Text(
                text = "Select Entrance (for outside routing)",
                style = MaterialTheme.typography.titleMedium
            )

            val availableEntrances = viewModel.getAvailableEntrances()
            val selectedEntrance = viewModel.getSelectedEntranceForTesting()

            if (availableEntrances.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(availableEntrances) { entrance ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { viewModel.selectEntrance(entrance) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedEntrance?.id == entrance.id) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedEntrance?.id == entrance.id) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = entrance.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Position: (${entrance.position.x.toInt()}, ${entrance.position.y.toInt()})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No entrance POIs found. Add some entrances first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusCards(
    currentPosition: Position?,
    selectedPOI: PointOfInterest?,
    signalStrength: SignalStrength,
    isInsideBuilding: Boolean,
    detectionMethod: String,
    selectedEntrance: PointOfInterest?
) {
    LazyRow(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Position Status
        item {
            StatusCard(
                title = "Position",
                content = currentPosition?.let {
                    "Floor ${it.floor}\n(${it.x.toInt()}, ${it.y.toInt()})\nAccuracy: ${it.accuracy}m"
                } ?: "No position"
            )
        }

        // Building Status
        item {
            StatusCard(
                title = "Building",
                content = "${if (isInsideBuilding) "INSIDE" else "OUTSIDE"}\n$detectionMethod",
                backgroundColor = if (isInsideBuilding)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
        }

        // Entrance Status (when outside)
        if (!isInsideBuilding) {
            item {
                StatusCard(
                    title = "Start Point",
                    content = selectedEntrance?.let {
                        "${it.name}\n(${it.position.x.toInt()}, ${it.position.y.toInt()})"
                    } ?: "No entrance selected"
                )
            }
        }

        // Selected POI
        selectedPOI?.let { poi ->
            item {
                StatusCard(
                    title = "Destination",
                    content = "${poi.name}\nFloor ${poi.position.floor}"
                )
            }
        }

        // Signal Strength
        item {
            StatusCard(
                title = "Signal",
                content = signalStrength.name.replace("_", " ")
            )
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    content: String,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SignalStrengthIndicator(
    signalStrength: SignalStrength,
    modifier: Modifier = Modifier
) {
    val (color, icon, description) = when (signalStrength) {
        SignalStrength.EXCELLENT -> Triple(
            Color(0xFF4CAF50),
            Icons.Default.SignalWifi4Bar,
            "Excellent signal"
        )

        SignalStrength.GOOD -> Triple(
            Color(0xFF8BC34A),
            Icons.Default.Wifi,
            "Good signal"
        )

        SignalStrength.FAIR -> Triple(
            Color(0xFFFF9800),
            Icons.Default.WifiTethering,
            "Fair signal"
        )

        SignalStrength.POOR -> Triple(
            Color(0xFFFF5722),
            Icons.Default.WifiTetheringOff,
            "Poor signal"
        )

        SignalStrength.SEARCHING -> Triple(
            Color(0xFF9E9E9E),
            Icons.Default.WifiFind,
            "Searching..."
        )

        SignalStrength.UNAVAILABLE -> Triple(
            Color(0xFF757575),
            Icons.Default.SignalWifiOff,
            "No signal"
        )
    }

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = when (signalStrength) {
                SignalStrength.EXCELLENT -> "Excellent"
                SignalStrength.GOOD -> "Good"
                SignalStrength.FAIR -> "Fair"
                SignalStrength.POOR -> "Poor"
                SignalStrength.SEARCHING -> "Searching"
                SignalStrength.UNAVAILABLE -> "No Signal"
            },
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun NodePlacementControls(
    isNodePlacementMode: Boolean,
    selectedNode: com.example.indoornavigation20.domain.model.NavNode?,
    onToggleNodePlacement: () -> Unit,
    onDeleteNode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mode indicator card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isNodePlacementMode) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isNodePlacementMode) Icons.Default.Edit else Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isNodePlacementMode) "Node Edit Mode" else "View Mode",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Toggle node placement mode
        FloatingActionButton(
            onClick = onToggleNodePlacement,
            containerColor = if (isNodePlacementMode) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            }
        ) {
            Icon(
                imageVector = if (isNodePlacementMode) Icons.Default.Done else Icons.Default.Edit,
                contentDescription = if (isNodePlacementMode) "Exit node placement" else "Edit nodes"
            )
        }

        // Delete selected node button with confirmation
        selectedNode?.let { node ->
            FloatingActionButton(
                onClick = { showDeleteConfirmation = true },
                containerColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected node",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Node placement instructions
        if (isNodePlacementMode) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "✨ Node Editing Active",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Tap to place new nodes\n• Drag to move nodes\n• Tap node to select",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Selected node info
        selectedNode?.let { node ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Selected Node",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "ID: ${node.id.takeLast(6)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Type: ${node.type.name}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Position: (${node.position.x.toInt()}, ${node.position.y.toInt()})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Connections: ${node.connections.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation && selectedNode != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Navigation Node") },
            text = {
                Text(
                    "Are you sure you want to delete this navigation node?\n\nID: ${
                        selectedNode.id.takeLast(
                            8
                        )
                    }\nPosition: (${selectedNode.position.x.toInt()}, ${selectedNode.position.y.toInt()})\n\nThis action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteNode(selectedNode.id)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun POIManagementControls(
    selectedPOI: PointOfInterest?,
    onDeletePOI: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    selectedPOI?.let { poi ->
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Selected POI info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Selected POI",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = poi.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Category: ${poi.category.name}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Position: (${poi.position.x.toInt()}, ${poi.position.y.toInt()})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Floor: ${poi.position.floor}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Delete POI button
            FloatingActionButton(
                onClick = { showDeleteConfirmation = true },
                containerColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected POI",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Delete confirmation dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Point of Interest") },
                text = {
                    Text("Are you sure you want to delete \"${poi.name}\"?\n\nCategory: ${poi.category.name}\nPosition: (${poi.position.x.toInt()}, ${poi.position.y.toInt()})\n\nThis action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeletePOI(poi.id)
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
