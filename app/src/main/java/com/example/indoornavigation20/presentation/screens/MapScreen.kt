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
import androidx.compose.ui.focus.onFocusChanged
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
    onNavigateToAdmin: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: MapViewModel = remember { MapViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var userPermissions by remember { mutableStateOf<UserPermissions?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Regular user actions (non-admin)
    var showUserActions by remember { mutableStateOf(false) }
    
    // Admin-specific actions
    var showAdminTools by remember { mutableStateOf(false) }

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

    // Load all POIs when search is opened to show in dropdown
    LaunchedEffect(showSearch) {
        if (showSearch) {
            viewModel.searchPOIs("") // Load all POIs
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.building?.name ?: "Wherezit",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    // Search button - always visible
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (showSearch) {
                            viewModel.searchPOIs("") // Load all POIs for dropdown
                        }
                    }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search locations",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Regular user menu (show for everyone including admins)
                    Box {
                        IconButton(onClick = { showUserActions = !showUserActions }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = showUserActions,
                            onDismissRequest = { showUserActions = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh Data") },
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.reloadPOIs()
                                    }
                                    showUserActions = false
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) }
                            )

                            DropdownMenuItem(
                                text = { Text("My Location") },
                                onClick = {
                                    viewModel.centerOnCurrentLocations()
                                    showUserActions = false
                                },
                                leadingIcon = { Icon(Icons.Default.MyLocation, null) }
                            )

                            Divider()

                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    onLogout()
                                    showUserActions = false
                                },
                                leadingIcon = { Icon(Icons.Default.Logout, null) }
                            )
                        }
                    }

                    // Admin menu - separate and comprehensive
                    if (userPermissions?.canAccessAdmin == true) {
                        Box {
                            IconButton(onClick = { showAdminTools = !showAdminTools }) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Tools",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }

                            DropdownMenu(
                                expanded = showAdminTools,
                                onDismissRequest = { showAdminTools = false }
                            ) {
                                // Data Management
                                Text(
                                    text = "Data Management",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )

                                DropdownMenuItem(
                                    text = { Text("Refresh All Data") },
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.forceReloadAllData()
                                        }
                                        showAdminTools = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Refresh, null) }
                                )

                                Divider()

                                // Network Tools
                                Text(
                                    text = "Network Tools",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )

                                DropdownMenuItem(
                                    text = { Text("Connect All Nodes") },
                                    onClick = {
                                        viewModel.connectAllNodesEnhanced()
                                        showAdminTools = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.AutoFixHigh, null) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Rebuild Network") },
                                    onClick = {
                                        viewModel.rebuildConnectionNetwork()
                                        showAdminTools = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.NetworkCheck, null) }
                                )

                                Divider()

                                // Diagnostics
                                Text(
                                    text = "Diagnostics",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )

                                DropdownMenuItem(
                                    text = { Text("System Diagnostics") },
                                    onClick = {
                                        viewModel.showDiagnostics()
                                        showAdminTools = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Analytics, null) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Node Storage Info") },
                                    onClick = {
                                        viewModel.showNodeStorageInfo()
                                        showAdminTools = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Storage, null) }
                                )

                                Divider()

                                DropdownMenuItem(
                                    text = { Text("Advanced Admin") },
                                    onClick = {
                                        onNavigateToAdmin()
                                        showAdminTools = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                                )

                                Divider()

                                DropdownMenuItem(
                                    text = { Text("Logout") },
                                    onClick = {
                                        onLogout()
                                        showAdminTools = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Logout, null) }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on map */ },
                    icon = {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = "Map",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text("Navigate") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPositioning,
                    icon = { Icon(Icons.Default.MyLocation, contentDescription = "Position") },
                    label = { Text("Position") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToBeacons,
                    icon = { Icon(Icons.Default.Sensors, contentDescription = "Sensors") },
                    label = { Text("Sensors") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        label = { Text("Admin") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                // Enhanced Search Bar with immediate dropdown
                if (showSearch) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.isNotEmpty()) {
                                    viewModel.searchPOIs(it)
                                } else {
                                    viewModel.searchPOIs("") // Show all POIs when empty
                                }
                            },
                            placeholder = {
                                Text("Find classrooms, labs, offices...")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        viewModel.searchPOIs("") // Show all POIs
                                    }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { showSearch = false }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .onFocusChanged { focusState ->
                                    isSearchFocused = focusState.isFocused
                                    if (focusState.isFocused && searchQuery.isEmpty()) {
                                        viewModel.searchPOIs("") // Load all POIs when focused
                                    }
                                },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = MaterialTheme.shapes.large,
                            singleLine = true
                        )
                    }
                }

                // Compact floor selector and status row
                if (uiState.availableFloors.size > 1 || userPermissions?.canAccessAdmin == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Floor selector chips
                        if (uiState.availableFloors.size > 1) {
                            uiState.availableFloors.forEach { floor ->
                                FilterChip(
                                    selected = floor == uiState.currentFloor,
                                    onClick = { viewModel.selectFloor(floor) },
                                    label = {
                                        Text(
                                            text = "Floor $floor",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier.height(32.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Status indicators
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Signal strength indicator
                            val signalColor = when (uiState.signalStrength) {
                                SignalStrength.EXCELLENT, SignalStrength.GOOD -> MaterialTheme.colorScheme.primary
                                SignalStrength.FAIR -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = signalColor,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )

                            // Admin indicator
                            if (userPermissions?.canAccessAdmin == true) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                // Search Results Dropdown - Show immediately when search is open or when typing
                if (showSearch && (uiState.searchResults.isNotEmpty() || isSearchFocused)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .heightIn(max = 300.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (uiState.searchResults.isEmpty() && searchQuery.isEmpty()) {
                                item {
                                    Text(
                                        text = "Loading locations...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else if (uiState.searchResults.isEmpty()) {
                                item {
                                    Text(
                                        text = "No locations found for \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(uiState.searchResults) { poi ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            viewModel.selectPOI(poi)
                                            showSearch = false
                                            searchQuery = ""
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = poi.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = poi.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Floor ${poi.position.floor} • ${poi.category.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
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
                    // Main Floor Plan Area - CLEAN and PROMINENT
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
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
                                viewModel.updateNodePosition(node.id, x, y)
                            },
                            isNodePlacementMode = uiState.isNodePlacementMode,
                            isAdminMode = userPermissions?.canAccessAdmin == true,
                            modifier = Modifier.fillMaxSize()
                        )

                        // MINIMAL bottom sheet for selected items - clean overlay
                        if (uiState.selectedPOI != null || uiState.selectedNode != null) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                ),
                                shape = MaterialTheme.shapes.large,
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    uiState.selectedPOI?.let { poi ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = poi.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${poi.category.name} • Floor ${poi.position.floor}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (userPermissions?.canAccessAdmin == true) {
                                                IconButton(onClick = { viewModel.deletePOI(poi.id) }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color(0xFFEF4444)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    uiState.selectedNode?.let { node ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Navigation Node",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${node.type.name} • ${node.connections.size} connections",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (userPermissions?.canAccessAdmin == true) {
                                                IconButton(onClick = { viewModel.deleteNode(node.id) }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color(0xFFEF4444)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Single minimal admin FAB
                        if (userPermissions?.canAccessAdmin == true) {
                            FloatingActionButton(
                                onClick = { viewModel.toggleNodePlacementMode() },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
                                containerColor = if (uiState.isNodePlacementMode) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            ) {
                                Icon(
                                    imageVector = if (uiState.isNodePlacementMode) Icons.Default.Done else Icons.Default.Edit,
                                    contentDescription = if (uiState.isNodePlacementMode) "Done editing" else "Edit mode"
                                )
                            }
                        }

                        // Error message - overlay on top when present
                        uiState.errorMessage?.let { error ->
                            Card(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (error.startsWith("✅") || error.startsWith("📍")) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else if (error.startsWith("❌")) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                shape = MaterialTheme.shapes.large
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
                                        color = if (error.startsWith("✅") || error.startsWith("📍")) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else if (error.startsWith("❌")) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.clearError() }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = if (error.startsWith("✅") || error.startsWith("📍")) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else if (error.startsWith("❌")) {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
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
            MaterialTheme.colorScheme.primary,
            Icons.Default.SignalWifi4Bar,
            "Excellent"
        )

        SignalStrength.GOOD -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Wifi,
            "Good"
        )

        SignalStrength.FAIR -> Triple(
            Color(0xFFFBBF24), // Yellow from our theme
            Icons.Default.WifiTethering,
            "Fair"
        )

        SignalStrength.POOR -> Triple(
            Color(0xFFEF4444), // Red from our theme
            Icons.Default.WifiTetheringOff,
            "Poor"
        )

        SignalStrength.SEARCHING -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.WifiFind,
            "Searching"
        )

        SignalStrength.UNAVAILABLE -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.SignalWifiOff,
            "No Signal"
        )
    }

    // More compact design with just icon and color indicator
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Color dot indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = color,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
