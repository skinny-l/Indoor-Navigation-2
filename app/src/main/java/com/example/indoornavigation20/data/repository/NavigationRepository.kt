package com.example.indoornavigation20.data.repository

import com.example.indoornavigation20.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class NavigationRepository {
    // Store POIs in memory for manual editing
    private val editablePOIs = mutableListOf<PointOfInterest>()

    // Store user-created navigation nodes in memory
    private val userNodes = mutableListOf<NavNode>()

    // Firebase instances
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        // Don't load default POIs anymore - only user-created ones
        // Load POIs from Firestore when repository is initialized
        loadPOIsFromFirestore()
        loadNodesFromFirestore()
    }

    // Add method to get debug info about stored nodes and their connections
    fun getNodeStorageInfo(): String {
        val userId = auth.currentUser?.uid ?: "guest_user"
        return """
            🔗 NODE STORAGE INFO 🔗
            
            📍 Storage Path: nodes/$userId/user_nodes/
            📊 Total Nodes: ${userNodes.size}
            
            📋 Node Details:
            ${
            userNodes.joinToString("\n") { node ->
                "• ${node.id.takeLast(8)}: (${node.position.x.toInt()}, ${node.position.y.toInt()}) - ${node.connections.size} connections"
            }
        }
            
            🔗 Connection Network:
            ${
            userNodes.joinToString("\n") { node ->
                if (node.connections.isNotEmpty()) {
                    "• ${node.id.takeLast(8)} → [${
                        node.connections.map { it.takeLast(4) }.joinToString(", ")
                    }]"
                } else {
                    "• ${node.id.takeLast(8)} → [no connections]"
                }
            }
        }
        """.trimIndent()
    }

    // Add method to get user nodes
    fun getUserNodes(): List<NavNode> {
        return userNodes.toList()
    }

    // Method to manually reload nodes (useful for debugging)
    fun reloadNodes() {
        loadNodesFromFirestore()
    }

    // Method to manually reload POIs (useful for debugging)
    fun reloadPOIs() {
        loadPOIsFromFirestore()
    }

    // Load POIs from Firestore
    private fun loadPOIsFromFirestore() {
        val userId = auth.currentUser?.uid ?: "guest_user"

        // Load POIs from the current user's collection
        firestore.collection("pois")
            .document(userId)
            .collection("user_pois")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e(
                        "NavigationRepository",
                        "Error loading POIs: ${error.message}"
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    editablePOIs.clear()
                    android.util.Log.d(
                        "NavigationRepository",
                        "Loading POIs for user: $userId"
                    )
                    android.util.Log.d(
                        "NavigationRepository",
                        "Found ${snapshot.documents.size} POI documents"
                    )

                    for (document in snapshot.documents) {
                        try {
                            val poi = PointOfInterest(
                                id = document.id,
                                name = document.getString("name") ?: "",
                                description = document.getString("description") ?: "",
                                category = POICategory.valueOf(
                                    document.getString("category") ?: "OTHER"
                                ),
                                position = Position(
                                    x = document.getDouble("x")?.toFloat() ?: 0f,
                                    y = document.getDouble("y")?.toFloat() ?: 0f,
                                    floor = document.getLong("floor")?.toInt() ?: 1,
                                    accuracy = document.getDouble("accuracy")?.toFloat() ?: 0f,
                                    timestamp = document.getLong("timestamp")
                                        ?: System.currentTimeMillis()
                                )
                            )
                            editablePOIs.add(poi)
                            android.util.Log.d(
                                "NavigationRepository",
                                "Loaded POI: ${poi.name} at (${poi.position.x}, ${poi.position.y})"
                            )
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "NavigationRepository",
                                "Error parsing POI document ${document.id}: ${e.message}"
                            )
                        }
                    }

                    android.util.Log.d(
                        "NavigationRepository",
                        "Total POIs loaded: ${editablePOIs.size}"
                    )
                }
            }
    }

    // Load user navigation nodes from Firestore
    private fun loadNodesFromFirestore() {
        val userId = auth.currentUser?.uid ?: "guest_user"

        // Load nodes from the current user's collection
        firestore.collection("nodes")
            .document(userId)
            .collection("user_nodes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e(
                        "NavigationRepository",
                        "Error loading navigation nodes: ${error.message}"
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    userNodes.clear()
                    android.util.Log.d(
                        "NavigationRepository",
                        "Loading navigation nodes for user: $userId"
                    )
                    android.util.Log.d(
                        "NavigationRepository",
                        "Found ${snapshot.documents.size} node documents"
                    )

                    for (document in snapshot.documents) {
                        try {
                            val node = NavNode(
                                id = document.id,
                                position = Position(
                                    x = document.getDouble("x")?.toFloat() ?: 0f,
                                    y = document.getDouble("y")?.toFloat() ?: 0f,
                                    floor = document.getLong("floor")?.toInt() ?: 1,
                                    accuracy = document.getDouble("accuracy")?.toFloat() ?: 0f,
                                    timestamp = document.getLong("timestamp")
                                        ?: System.currentTimeMillis()
                                ),
                                connections = document.get("connections") as? List<String>
                                    ?: emptyList(),
                                isWalkable = document.getBoolean("isWalkable") ?: true,
                                type = NodeType.valueOf(document.getString("type") ?: "WALKWAY"),
                                isUserCreated = true
                            )
                            userNodes.add(node)
                            android.util.Log.d(
                                "NavigationRepository",
                                "Loaded navigation node: ${node.id} at (${node.position.x}, ${node.position.y})"
                            )
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "NavigationRepository",
                                "Error parsing node document ${document.id}: ${e.message}"
                            )
                        }
                    }

                    android.util.Log.d(
                        "NavigationRepository",
                        "Total navigation nodes loaded: ${userNodes.size}"
                    )
                }
            }
    }

    // Simplified method to connect all existing nodes (works offline first)
    fun connectAllNodesOffline(): Int {
        val connectionDistance = 150f
        var connectionsAdded = 0

        println("🔗 Starting to connect ${userNodes.size} nodes offline...")

        userNodes.forEachIndexed { index, node ->
            val nearbyNodes = userNodes.filter { otherNode ->
                otherNode.id != node.id &&
                        distance(node.position, otherNode.position) <= connectionDistance &&
                        otherNode.isWalkable &&
                        otherNode.type != NodeType.OBSTACLE
            }

            val newConnections = nearbyNodes.map { it.id }.filter { !node.connections.contains(it) }
            if (newConnections.isNotEmpty()) {
                val updatedNode = node.copy(connections = node.connections + newConnections)
                userNodes[index] = updatedNode
                connectionsAdded += newConnections.size
                println("✅ Connected node ${node.id} to ${newConnections.size} new nodes")
            }
        }

        println("🔗 Offline connection completed. Added $connectionsAdded connections total.")
        return connectionsAdded
    }

    // Add method to connect all existing nodes retroactively
    suspend fun connectAllNodes(): Int {
        val connectionDistance = 150f
        var connectionsAdded = 0

        try {
            println("🔗 Starting to connect ${userNodes.size} nodes...")

            userNodes.forEachIndexed { index, node ->
                println("🔗 Processing node ${index + 1}/${userNodes.size}: ${node.id}")

                val nearbyNodes = userNodes.filter { otherNode ->
                    otherNode.id != node.id &&
                            distance(node.position, otherNode.position) <= connectionDistance &&
                            otherNode.isWalkable &&
                            otherNode.type != NodeType.OBSTACLE
                }

                println("🔗 Found ${nearbyNodes.size} nearby nodes for ${node.id}")

                val newConnections =
                    nearbyNodes.map { it.id }.filter { !node.connections.contains(it) }
                if (newConnections.isNotEmpty()) {
                    val updatedNode = node.copy(connections = node.connections + newConnections)
                    userNodes[index] = updatedNode

                    // Try to save to Firestore with error handling
                    try {
                        val saved = saveNodeToFirestore(updatedNode)
                        if (saved) {
                            connectionsAdded += newConnections.size
                            println("✅ Connected node ${node.id} to ${newConnections.size} new nodes")
                        } else {
                            println("❌ Failed to save node ${node.id} to Firestore")
                        }
                    } catch (e: Exception) {
                        println("❌ Error saving node ${node.id}: ${e.message}")
                    }
                } else {
                    println("ℹ️ Node ${node.id} already has all possible connections")
                }
            }

            println("🔗 Connection process completed. Added $connectionsAdded connections total.")
            return connectionsAdded

        } catch (e: Exception) {
            println("❌ Error in connectAllNodes: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    // Save POI to Firestore
    private suspend fun savePOIToFirestore(poi: PointOfInterest): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: "guest_user"

            val poiData = hashMapOf(
                "name" to poi.name,
                "description" to poi.description,
                "category" to poi.category.name,
                "x" to poi.position.x,
                "y" to poi.position.y,
                "floor" to poi.position.floor,
                "accuracy" to poi.position.accuracy,
                "timestamp" to poi.position.timestamp
            )

            firestore.collection("pois")
                .document(userId)
                .collection("user_pois")
                .document(poi.id)
                .set(poiData)
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Save navigation node to Firestore
    private suspend fun saveNodeToFirestore(node: NavNode): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: "guest_user"

            val nodeData = hashMapOf(
                "x" to node.position.x,
                "y" to node.position.y,
                "floor" to node.position.floor,
                "accuracy" to node.position.accuracy,
                "timestamp" to node.position.timestamp,
                "connections" to node.connections,
                "isWalkable" to node.isWalkable,
                "type" to node.type.name
            )

            firestore.collection("nodes")
                .document(userId)
                .collection("user_nodes")
                .document(node.id)
                .set(nodeData)
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Delete POI from Firestore
    private suspend fun deletePOIFromFirestore(poiId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: "guest_user"

            firestore.collection("pois")
                .document(userId)
                .collection("user_pois")
                .document(poiId)
                .delete()
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Delete navigation node from Firestore
    private suspend fun deleteNodeFromFirestore(nodeId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: "guest_user"

            firestore.collection("nodes")
                .document(userId)
                .collection("user_nodes")
                .document(nodeId)
                .delete()
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Add method to update POI position
    suspend fun updatePOIPosition(poiId: String, x: Float, y: Float): Boolean {
        val poi = editablePOIs.find { it.id == poiId }
        return if (poi != null) {
            val updatedPOI = poi.copy(position = Position(x, y, poi.position.floor))
            val index = editablePOIs.indexOf(poi)
            editablePOIs[index] = updatedPOI

            // Save to Firestore
            savePOIToFirestore(updatedPOI)
        } else {
            false
        }
    }

    // Add method to add new POI
    suspend fun addPOI(name: String, x: Float, y: Float, category: POICategory): PointOfInterest {
        val newPOI = PointOfInterest(
            id = "poi_${System.currentTimeMillis()}",
            name = name,
            description = "User added POI",
            category = category,
            position = Position(x, y, 1)
        )
        editablePOIs.add(newPOI)

        // Save to Firestore
        savePOIToFirestore(newPOI)

        return newPOI
    }

    // Add method to add new navigation node
    suspend fun addNode(x: Float, y: Float): NavNode {
        val newNode = NavNode(
            id = "node_${System.currentTimeMillis()}",
            position = Position(x, y, 1),
            connections = emptyList(),
            isWalkable = true,
            type = NodeType.WALKWAY,
            isUserCreated = true
        )
        userNodes.add(newNode)

        // Auto-connect to nearby nodes
        val updatedNode = autoConnectNode(newNode)
        val index = userNodes.indexOf(newNode)
        userNodes[index] = updatedNode

        // Save to Firestore
        saveNodeToFirestore(updatedNode)

        return updatedNode
    }

    // Add method to delete POI
    suspend fun deletePOI(poiId: String): Boolean {
        val removed = editablePOIs.removeIf { it.id == poiId }
        if (removed) {
            // Delete from Firestore
            deletePOIFromFirestore(poiId)
        }
        return removed
    }

    // Add method to delete navigation node
    suspend fun deleteNode(nodeId: String): Boolean {
        val removed = userNodes.removeIf { it.id == nodeId }
        if (removed) {
            // Delete from Firestore
            deleteNodeFromFirestore(nodeId)
        }
        return removed
    }

    // Add method to update navigation node position
    suspend fun updateNodePosition(nodeId: String, x: Float, y: Float): Boolean {
        val node = userNodes.find { it.id == nodeId }
        return if (node != null) {
            val updatedNode = node.copy(position = Position(x, y, node.position.floor))
            val index = userNodes.indexOf(node)
            userNodes[index] = updatedNode

            // Save to Firestore
            saveNodeToFirestore(updatedNode)
        } else {
            false
        }
    }

    // Add method to update navigation node connections
    suspend fun updateNodeConnections(nodeId: String, connections: List<String>): Boolean {
        val node = userNodes.find { it.id == nodeId }
        return if (node != null) {
            val updatedNode = node.copy(connections = connections)
            val index = userNodes.indexOf(node)
            userNodes[index] = updatedNode

            // Save to Firestore
            saveNodeToFirestore(updatedNode)
        } else {
            false
        }
    }

    // Add method to update navigation node properties
    suspend fun updateNodeProperties(nodeId: String, isWalkable: Boolean, type: NodeType): Boolean {
        val node = userNodes.find { it.id == nodeId }
        return if (node != null) {
            val updatedNode = node.copy(isWalkable = isWalkable, type = type)
            val index = userNodes.indexOf(node)
            userNodes[index] = updatedNode

            // Save to Firestore
            saveNodeToFirestore(updatedNode)
        } else {
            false
        }
    }

    // Auto-connect nodes within reasonable distance
    private fun autoConnectNode(newNode: NavNode): NavNode {
        val connectionDistance = 150f // Maximum connection distance in pixels
        val nearbyNodes = userNodes.filter { existingNode ->
            existingNode.id != newNode.id &&
                    distance(newNode.position, existingNode.position) <= connectionDistance &&
                    existingNode.isWalkable &&
                    existingNode.type != NodeType.OBSTACLE
        }

        val connections = nearbyNodes.map { it.id }.toMutableList()

        // Also update the nearby nodes to connect back to this new node
        nearbyNodes.forEach { nearbyNode ->
            if (!nearbyNode.connections.contains(newNode.id)) {
                val updatedConnections = nearbyNode.connections + newNode.id
                val updatedNearbyNode = nearbyNode.copy(connections = updatedConnections)
                val nearbyIndex = userNodes.indexOf(nearbyNode)
                userNodes[nearbyIndex] = updatedNearbyNode

                // Save updated nearby node to Firestore
                kotlin.runCatching {
                    kotlinx.coroutines.runBlocking { saveNodeToFirestore(updatedNearbyNode) }
                }
            }
        }

        println("🔗 Auto-connected node ${newNode.id} to ${connections.size} nearby nodes")
        return newNode.copy(connections = connections)
    }

    private fun distance(pos1: Position, pos2: Position): Float {
        val dx = pos1.x - pos2.x
        val dy = pos1.y - pos2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    suspend fun getFloorPlans(buildingId: String): Flow<Result<List<FloorPlan>>> = flow {
        emit(Result.Loading)
        try {
            // Simulate network delay
            kotlinx.coroutines.delay(1000)

            // Real Computer Science Building data
            val realFloorPlans = listOf(
                createComputerScienceBuildingGroundFloor()
            )

            emit(Result.Success(realFloorPlans))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    private fun createComputerScienceBuildingGroundFloor(): FloorPlan {
        val metersPerPixelX = 75.0f / 1165.1f
        val metersPerPixelY = 30.0f / 760.8f

        val coordinateSystem = CoordinateSystem(
            originLatitude = 3.071421,
            originLongitude = 101.500136,
            metersPerPixelX = metersPerPixelX,
            metersPerPixelY = metersPerPixelY
        )

        return FloorPlan(
            id = "cs_building_ground_floor",
            buildingId = "computer_science_building",
            floorNumber = 1,
            name = "Computer Science Building - Ground Floor",
            imageUrl = "android.resource://com.example.indoornavigation20/drawable/plain_svg",
            svgUrl = "android.resource://com.example.indoornavigation20/drawable/plain_svg",
            width = 1165.1f, // Use actual SVG viewBox width
            height = 760.8f, // Use actual SVG viewBox height
            nodes = userNodes.filter { it.position.floor == 1 }, // Use user-created nodes
            rooms = createCSBuildingRooms(),
            walls = createCSBuildingWalls(),
            coordinateSystem = coordinateSystem
        )
    }

    private fun createCSBuildingRooms(): List<Room> {
        return listOf(
            // Theater Halls (bottom area)
            Room(
                id = "th1",
                name = "TH 1",
                type = RoomType.CLASSROOM,
                bounds = RoomBounds(
                    topLeft = Position(388.6f, 737f, 1),
                    bottomRight = Position(500f, 821.5f, 1)
                ),
                capacity = 150,
                features = listOf("Theater Seating", "Projector", "Audio System", "Stage")
            ),
            Room(
                id = "th2",
                name = "TH 2",
                type = RoomType.CLASSROOM,
                bounds = RoomBounds(
                    topLeft = Position(500f, 737f, 1),
                    bottomRight = Position(620f, 821.5f, 1)
                ),
                capacity = 150,
                features = listOf("Theater Seating", "Projector", "Audio System", "Stage")
            ),
            Room(
                id = "th3",
                name = "TH 3",
                type = RoomType.CLASSROOM,
                bounds = RoomBounds(
                    topLeft = Position(620f, 737f, 1),
                    bottomRight = Position(740f, 821.5f, 1)
                ),
                capacity = 150,
                features = listOf("Theater Seating", "Projector", "Audio System", "Stage")
            ),

            // Theater Halls (right side)
            Room(
                id = "th4",
                name = "TH 4",
                type = RoomType.CLASSROOM,
                bounds = RoomBounds(
                    topLeft = Position(1256f, 186.5f, 1),
                    bottomRight = Position(1457.4f, 271f, 1)
                ),
                capacity = 120,
                features = listOf("Theater Seating", "Projector", "Audio System", "Stage")
            ),
            Room(
                id = "th5",
                name = "TH 5",
                type = RoomType.CLASSROOM,
                bounds = RoomBounds(
                    topLeft = Position(1256f, 89.5f, 1),
                    bottomRight = Position(1457.4f, 174f, 1)
                ),
                capacity = 120,
                features = listOf("Theater Seating", "Projector", "Audio System", "Stage")
            ),

            // Administrative Offices
            Room(
                id = "pejabot_akademik",
                name = "Pejabot Pengurusan Akademik",
                type = RoomType.OFFICE,
                bounds = RoomBounds(
                    topLeft = Position(629.7f, 77f, 1),
                    bottomRight = Position(772.2f, 150f, 1)
                ),
                capacity = 20,
                features = listOf("Administrative Services", "Student Services", "Reception")
            ),
            Room(
                id = "pejabot_pentadbiran",
                name = "Pejabot Pengurusan Pentadbiran FSKM",
                type = RoomType.OFFICE,
                bounds = RoomBounds(
                    topLeft = Position(401.1f, 197f, 1),
                    bottomRight = Position(617.2f, 280f, 1)
                ),
                capacity = 15,
                features = listOf("FSKM Administration", "Faculty Management", "Academic Affairs")
            ),
            Room(
                id = "unit_cawangan_zon4",
                name = "Unit Cawangan Zon 4",
                type = RoomType.OFFICE,
                bounds = RoomBounds(
                    topLeft = Position(1256f, 283.5f, 1),
                    bottomRight = Position(1457.4f, 350f, 1)
                ),
                capacity = 10,
                features = listOf("Zone 4 Branch Services", "Student Support")
            ),

            // Facilities
            Room(
                id = "cafe",
                name = "Cafe",
                type = RoomType.CAFETERIA,
                bounds = RoomBounds(
                    topLeft = Position(1350f, 400f, 1),
                    bottomRight = Position(1500f, 500f, 1)
                ),
                capacity = 80,
                features = listOf("Food & Beverages", "Seating Area", "Student Gathering Space")
            ),

            // Central Courtyard
            Room(
                id = "laman_najib",
                name = "Laman Najib",
                type = RoomType.LOBBY,
                bounds = RoomBounds(
                    topLeft = Position(650f, 300f, 1),
                    bottomRight = Position(950f, 600f, 1)
                ),
                features = listOf(
                    "Open Courtyard",
                    "Student Gathering",
                    "Natural Lighting",
                    "Landscaping"
                )
            ),

            // Toilets
            Room(
                id = "tandas_l",
                name = "Tandas (L)",
                type = RoomType.RESTROOM,
                bounds = RoomBounds(
                    topLeft = Position(401.1f, 320f, 1),
                    bottomRight = Position(450f, 380f, 1)
                ),
                features = listOf("Ladies Restroom", "Wheelchair Accessible")
            ),
            Room(
                id = "tandas_p",
                name = "Tandas (P)",
                type = RoomType.RESTROOM,
                bounds = RoomBounds(
                    topLeft = Position(401.1f, 390f, 1),
                    bottomRight = Position(450f, 450f, 1)
                ),
                features = listOf("Men's Restroom", "Wheelchair Accessible")
            ),

            // Elevator
            Room(
                id = "lift_area",
                name = "Lift",
                type = RoomType.ELEVATOR_SHAFT,
                bounds = RoomBounds(
                    topLeft = Position(580f, 200f, 1),
                    bottomRight = Position(620f, 240f, 1)
                ),
                features = listOf("Elevator Access", "Multi-floor Access")
            ),

            // Main circulation areas
            Room(
                id = "main_corridor",
                name = "Main Corridor",
                type = RoomType.HALLWAY,
                bounds = RoomBounds(
                    topLeft = Position(450f, 200f, 1),
                    bottomRight = Position(650f, 700f, 1)
                ),
                features = listOf("Main Circulation", "Information Displays")
            ),
            Room(
                id = "east_corridor",
                name = "East Corridor",
                type = RoomType.HALLWAY,
                bounds = RoomBounds(
                    topLeft = Position(1200f, 200f, 1),
                    bottomRight = Position(1256f, 500f, 1)
                ),
                features = listOf("Secondary Circulation", "Natural Lighting")
            )
        )
    }

    private fun createCSBuildingWalls(): List<Wall> {
        return listOf(
            // Exterior walls
            Wall(
                "ext_wall_north",
                Position(388.6f, 697f, 1),
                Position(963.5f, 697f, 1),
                2.0f,
                WallType.EXTERIOR
            ),
            Wall(
                "ext_wall_south",
                Position(401.1f, 821.5f, 1),
                Position(891.7f, 821.5f, 1),
                2.0f,
                WallType.EXTERIOR
            ),
            Wall(
                "ext_wall_west",
                Position(401.1f, 697f, 1),
                Position(401.1f, 834f, 1),
                2.0f,
                WallType.EXTERIOR
            ),
            Wall(
                "ext_wall_east",
                Position(1469.9f, 77f, 1),
                Position(1469.9f, 377f, 1),
                2.0f,
                WallType.EXTERIOR
            ),

            // Interior partition walls
            Wall(
                "partition_1",
                Position(617.2f, 197f, 1),
                Position(617.2f, 498.6f, 1),
                1.0f,
                WallType.PARTITION
            ),
            Wall(
                "partition_2",
                Position(772.2f, 77f, 1),
                Position(772.2f, 197f, 1),
                1.0f,
                WallType.PARTITION
            ),
            Wall(
                "partition_3",
                Position(939.8f, 77f, 1),
                Position(939.8f, 209.5f, 1),
                1.0f,
                WallType.PARTITION
            ),
            Wall(
                "partition_4",
                Position(1256f, 89.5f, 1),
                Position(1256f, 364.5f, 1),
                1.0f,
                WallType.PARTITION
            )
        )
    }

    private fun createCSBuildingNavigationNodes(): List<NavNode> {
        return emptyList() // Remove all hardcoded nodes - user will place their own
    }

    suspend fun searchPOIs(query: String): Flow<Result<List<PointOfInterest>>> = flow {
        emit(Result.Loading)
        try {
            kotlinx.coroutines.delay(500)

            // Use the editable POIs instead of hardcoded ones
            val filteredPOIs = if (query.isBlank()) {
                editablePOIs.toList()
            } else {
                editablePOIs.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true) ||
                            it.category.name.contains(query, ignoreCase = true)
                }
            }

            emit(Result.Success(filteredPOIs))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    suspend fun getBeaconsForFloor(floor: Int): List<Beacon> {
        // Updated beacon positions based on the actual POI layout
        return listOf(
            Beacon(
                id = "beacon_entrance",
                uuid = "550e8400-e29b-41d4-a716-446655440000",
                major = 1,
                minor = 1,
                macAddress = "AA:BB:CC:DD:EE:01",
                position = Position(x = 650f, y = 700f, floor = floor) // Main entrance
            ),
            Beacon(
                id = "beacon_th_area",
                uuid = "550e8400-e29b-41d4-a716-446655440000",
                major = 1,
                minor = 2,
                macAddress = "AA:BB:CC:DD:EE:02",
                position = Position(x = 560f, y = 737f, floor = floor) // Theater halls area
            ),
            Beacon(
                id = "beacon_admin_area",
                uuid = "550e8400-e29b-41d4-a716-446655440000",
                major = 1,
                minor = 3,
                macAddress = "AA:BB:CC:DD:EE:03",
                position = Position(x = 700f, y = 113f, floor = floor) // Academic office area
            ),
            Beacon(
                id = "beacon_laman_najib",
                uuid = "550e8400-e29b-41d4-a716-446655440000",
                major = 1,
                minor = 4,
                macAddress = "AA:BB:CC:DD:EE:04",
                position = Position(x = 800f, y = 450f, floor = floor) // Central courtyard
            ),
            Beacon(
                id = "beacon_east_wing",
                uuid = "550e8400-e29b-41d4-a716-446655440000",
                major = 1,
                minor = 5,
                macAddress = "AA:BB:CC:DD:EE:05",
                position = Position(x = 1356f, y = 228f, floor = floor) // East wing with TH4/TH5
            ),
            Beacon(
                id = "beacon_cafe",
                uuid = "550e8400-e29b-41d4-a716-446655440000",
                major = 1,
                minor = 6,
                macAddress = "AA:BB:CC:DD:EE:06",
                position = Position(x = 1425f, y = 450f, floor = floor) // Cafe area
            )
        )
    }

    suspend fun getBuildingInfo(buildingId: String): Building {
        return Building(
            id = buildingId,
            name = "Computer Science Building",
            coordinates = BuildingCoordinates(
                latitude = 3.071421,
                longitude = 101.500136
            ),
            dimensions = BuildingDimensions(
                width = 75.0f,
                height = 30.0f,
                length = 75.0f
            ),
            address = "Computer Science Faculty, University Campus",
            description = "Modern computer science facility with research labs, lecture halls, and student areas",
            floors = listOf(1, 2, 3)
        )
    }

    private fun getDefaultPOIs(): List<PointOfInterest> {
        return emptyList() // Only show user-added POIs
    }
}
