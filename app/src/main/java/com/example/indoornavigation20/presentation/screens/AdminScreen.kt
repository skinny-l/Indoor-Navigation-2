package com.example.indoornavigation20.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.indoornavigation20.domain.model.POICategory
import com.example.indoornavigation20.domain.model.PointOfInterest
import com.example.indoornavigation20.domain.model.UserPermissions
import com.example.indoornavigation20.presentation.components.FloorPlanViewer
import com.example.indoornavigation20.presentation.viewmodel.MapViewModel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDebug: (() -> Unit)? = null,
    viewModel: MapViewModel = viewModel()
) {
    var isAdminMode by remember { mutableStateOf(false) }
    var showAddPOIDialog by remember { mutableStateOf(false) }
    var selectedPOI by remember { mutableStateOf<PointOfInterest?>(null) }
    var newPOIPosition by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var userPermissions by remember { mutableStateOf<UserPermissions?>(null) }
    var showNodeManagement by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    // Load user permissions
    LaunchedEffect(Unit) {
        userPermissions = viewModel.getUserPermissions()
        viewModel.getAllPOIs()
    }

    // Check if user has admin access
    if (userPermissions?.canAccessAdmin != true) {
        // Show access denied screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Access Denied",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Access Denied",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You don't have permission to access the admin panel.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNavigateBack) {
                Text("Go Back")
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text(if (showNodeManagement) "Node Admin" else "POI Admin") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                onNavigateToDebug?.let {
                    IconButton(onClick = it) {
                        Text("Debug")
                    }
                }
                Button(
                    onClick = { showNodeManagement = !showNodeManagement }
                ) {
                    Text(if (showNodeManagement) "POI Mode" else "Node Mode")
                }
                Switch(
                    checked = isAdminMode,
                    onCheckedChange = { isAdminMode = it }
                )
                Text("Edit Mode", modifier = Modifier.padding(end = 8.dp))
            }
        )

        if (isAdminMode) {
            // Instructions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (showNodeManagement) {
                        "Node Mode: Tap on map to add navigation nodes, tap existing nodes to select"
                    } else {
                        "POI Mode: Tap on map to add POI, tap existing POI to select"
                    },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Floor Plan with Admin Mode
        FloorPlanViewer(
            floorPlan = uiState.currentFloorPlan,
            currentPosition = uiState.currentPosition,
            selectedPOI = selectedPOI,
            pointsOfInterest = uiState.searchResults,
            navigationPath = uiState.navigationPath,
            onPOIClick = { poi ->
                if (!showNodeManagement) {
                    selectedPOI = if (selectedPOI == poi) null else poi
                }
            },
            isAdminMode = isAdminMode,
            onAddPOI = { x, y ->
                if (isAdminMode && !showNodeManagement) {
                    newPOIPosition = Pair(x, y)
                    showAddPOIDialog = true
                }
            },
            onMovePOI = { poi, x, y ->
                if (isAdminMode && !showNodeManagement) {
                    viewModel.updatePOIPosition(poi.id, x, y)
                }
            },
            // Node-related parameters
            userNodes = uiState.userNodes,
            selectedNode = uiState.selectedNode,
            onNodeClick = { node ->
                if (showNodeManagement) {
                    viewModel.selectNode(node)
                }
            },
            onAddNode = { x, y ->
                if (isAdminMode && showNodeManagement) {
                    viewModel.addNode(x, y)
                }
            },
            onMoveNode = { node, x, y ->
                if (isAdminMode && showNodeManagement) {
                    viewModel.updateNodePosition(node.id, x, y)
                }
            },
            isNodePlacementMode = showNodeManagement && isAdminMode,
            modifier = Modifier.weight(1f)
        )

        // Selected POI Actions
        selectedPOI?.let { poi ->
            if (!showNodeManagement) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = poi.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Position: (${poi.position.x.toInt()}, ${poi.position.y.toInt()})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.deletePOI(poi.id)
                                    selectedPOI = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }

        // Selected Node Actions
        uiState.selectedNode?.let { node ->
            if (showNodeManagement) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Node ${node.id}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Position: (${node.position.x.toInt()}, ${node.position.y.toInt()})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Type: ${node.type.name}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Connections: ${node.connections.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.deleteNode(node.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    // Add POI Dialog
    if (showAddPOIDialog && newPOIPosition != null) {
        AddPOIDialog(
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
fun AddPOIDialog(
    position: Pair<Float, Float>,
    onConfirm: (String, POICategory) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(POICategory.CLASSROOM) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add POI") },
        text = {
            Column {
                Text("Position: (${position.first.toInt()}, ${position.second.toInt()})")
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("POI Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                            .fillMaxWidth()
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
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedCategory)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
