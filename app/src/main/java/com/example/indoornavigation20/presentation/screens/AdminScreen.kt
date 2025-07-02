package com.example.indoornavigation20.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.indoornavigation20.domain.model.POICategory
import com.example.indoornavigation20.domain.model.PointOfInterest
import com.example.indoornavigation20.domain.model.UserPermissions
import com.example.indoornavigation20.presentation.components.FloorPlanViewer
import com.example.indoornavigation20.presentation.viewmodel.MapViewModel
import androidx.compose.material.icons.filled.Lock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDebug: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    viewModel: MapViewModel = viewModel()
) {
    var isEditMode by remember { mutableStateOf(false) }
    var showAddPOIDialog by remember { mutableStateOf(false) }
    var selectedPOI by remember { mutableStateOf<PointOfInterest?>(null) }
    var newPOIPosition by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var userPermissions by remember { mutableStateOf<UserPermissions?>(null) }
    var currentMode by remember { mutableStateOf("POI") } // "POI" or "NODE"

    val uiState by viewModel.uiState.collectAsState()

    // Load user permissions
    LaunchedEffect(Unit) {
        userPermissions = viewModel.getUserPermissions()
        viewModel.getAllPOIs()
    }

    // Check if user has admin access
    if (userPermissions?.canAccessAdmin != true) {
        // Modern access denied screen
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Access Denied",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Access Restricted",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Admin privileges required",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Admin Panel",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    onNavigateToDebug?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.BugReport, contentDescription = "Debug")
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (isEditMode) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Mode-specific actions
                    if (currentMode == "POI") {
                        selectedPOI?.let {
                            SmallFloatingActionButton(
                                onClick = {
                                    viewModel.deletePOI(it.id)
                                    selectedPOI = null
                                },
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete POI",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        uiState.selectedNode?.let {
                            SmallFloatingActionButton(
                                onClick = { viewModel.deleteNode(it.id) },
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Node",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    // Main edit toggle FAB
                    FloatingActionButton(
                        onClick = { isEditMode = !isEditMode },
                        containerColor = if (isEditMode) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(
                            if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Done editing" else "Start editing"
                        )
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = { isEditMode = true },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Start editing")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mode selector and status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mode selector
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text(
                                text = "Mode",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = currentMode == "POI",
                                onClick = { currentMode = "POI" },
                                label = { Text("Locations") },
                                leadingIcon = if (currentMode == "POI") {
                                    { Icon(Icons.Default.Place, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = currentMode == "NODE",
                                onClick = { currentMode = "NODE" },
                                label = { Text("Navigation") },
                                leadingIcon = if (currentMode == "NODE") {
                                    { Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                    
                    // Edit mode indicator
                    if (isEditMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (currentMode == "POI") "Tap map to add locations" else "Tap map to add nodes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Floor Plan with Admin Mode
            Box(modifier = Modifier.weight(1f)) {
                FloorPlanViewer(
                    floorPlan = uiState.currentFloorPlan,
                    currentPosition = uiState.currentPosition,
                    selectedPOI = selectedPOI,
                    pointsOfInterest = uiState.searchResults,
                    navigationPath = uiState.navigationPath,
                    onPOIClick = { poi ->
                        if (currentMode == "POI") {
                            selectedPOI = if (selectedPOI == poi) null else poi
                        }
                    },
                    isAdminMode = isEditMode,
                    onAddPOI = { x, y ->
                        if (isEditMode && currentMode == "POI") {
                            newPOIPosition = Pair(x, y)
                            showAddPOIDialog = true
                        }
                    },
                    onMovePOI = { poi, x, y ->
                        if (isEditMode && currentMode == "POI") {
                            viewModel.updatePOIPosition(poi.id, x, y)
                        }
                    },
                    // Node-related parameters
                    userNodes = uiState.userNodes,
                    selectedNode = uiState.selectedNode,
                    onNodeClick = { node ->
                        if (currentMode == "NODE") {
                            viewModel.selectNode(node)
                        }
                    },
                    onAddNode = { x, y ->
                        if (isEditMode && currentMode == "NODE") {
                            viewModel.addNode(x, y)
                        }
                    },
                    onMoveNode = { node, x, y ->
                        if (isEditMode && currentMode == "NODE") {
                            viewModel.updateNodePosition(node.id, x, y)
                        }
                    },
                    isNodePlacementMode = currentMode == "NODE" && isEditMode,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Selection info overlay
                if (currentMode == "POI") {
                    selectedPOI?.let { poi ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = poi.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${poi.category.name} • Floor ${poi.position.floor}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                } else {
                    uiState.selectedNode?.let { node ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Navigation Node",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${node.type.name} • ${node.connections.size} connections",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add POI Dialog
    if (showAddPOIDialog && newPOIPosition != null) {
        ModernAddPOIDialog(
            position = newPOIPosition!!,
            onConfirm = { name, category ->
                viewModel.addPOI(name, newPOIPosition!!.first, newPOIPosition!!.second, category)
                showAddPOIDialog = false
                newPOIPosition = null
            },
            onDismiss = {
                showAddPOIDialog = false
                newPOIPosition = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAddPOIDialog(
    position: Pair<Float, Float>,
    onConfirm: (String, POICategory) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(POICategory.CLASSROOM) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Add Location",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Position: (${position.first.toInt()}, ${position.second.toInt()})",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name") },
                    placeholder = { Text("e.g., Room 101, Main Lab") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.large,
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.large
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        POICategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedCategory)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Add Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = MaterialTheme.shapes.large
    )
}
