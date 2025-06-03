package com.example.indoornavigation20.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.indoornavigation20.presentation.components.FloorPlanViewer
import com.example.indoornavigation20.presentation.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToPositioning: () -> Unit = {},
    onNavigateToBeacons: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: MapViewModel = remember { MapViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

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
                    IconButton(onClick = onNavigateToPositioning) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Positioning")
                    }
                    IconButton(onClick = onNavigateToBeacons) {
                        Icon(Icons.Default.Sensors, contentDescription = "Beacons")
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
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                FloorPlanViewer(
                    floorPlan = uiState.currentFloorPlan,
                    currentPosition = uiState.currentPosition,
                    selectedPOI = uiState.selectedPOI,
                    pointsOfInterest = uiState.searchResults,
                    navigationPath = uiState.navigationPath,
                    onPOIClick = { poi -> viewModel.selectPOI(poi) },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp)
                )
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
    }
}
