package com.example.indoornavigation20.data.repository

import com.example.indoornavigation20.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NavigationRepository {

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
        val metersPerPixelX = 75.0f / 1165.149f
        val metersPerPixelY = 30.0f / 760.814f

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
            width = 1165.149f,
            height = 760.814f,
            nodes = createCSBuildingNavigationNodes(),
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
        // All coordinates are NEW ESTIMATES based on visual alignment with:
        // 1. User's blue line drawing ("20-56-...png")
        // 2. User's cleaner POI map ("new_map_layout.png")
        // 3. Target canvas dimensions: 1165.149f (width) x 760.814f (height)
        // The node IDs and general connection structure are from the user's last provided NavNode list.

        return listOf(
            // === ENTRANCES (Green) ===
            // West Entrance (bottom-left of overall structure on blue line map)
            NavNode(
                id = "ENTRANCE_WEST",
                position = Position(80f, 560f, 1),
                connections = listOf("W_CORRIDOR_1"),
                type = NodeType.DOOR
            ),
            // Main South Entrance (bottom-center on blue line map) - THIS WILL BE DEMO START
            NavNode(
                id = "ENTRANCE_MAIN_SOUTH",
                position = Position(582f, 740f, 1),
                connections = listOf("SOUTH_CORRIDOR_1"),
                type = NodeType.DOOR
            ),
            // North-East Entrance (top-right on blue line map)
            NavNode(
                id = "ENTRANCE_EAST",
                position = Position(1100f, 180f, 1),
                connections = listOf("EAST_CORRIDOR_1_FROM_ENTRANCE"),
                type = NodeType.DOOR
            ),

            // === SOUTH CORRIDOR (from main entrance northwards, then west to theaters) ===
            NavNode(
                id = "SOUTH_CORRIDOR_1",
                position = Position(582f, 680f, 1),
                connections = listOf("ENTRANCE_MAIN_SOUTH", "SOUTH_CORRIDOR_2")
            ),
            NavNode(
                id = "SOUTH_CORRIDOR_2",
                position = Position(582f, 620f, 1),
                connections = listOf(
                    "SOUTH_CORRIDOR_1",
                    "TH_HALL_CORRIDOR_EAST_END",
                    "CENTRAL_JUNCTION_SOUTH_POINT"
                )
            ),

            // === THEATER HALLS CORRIDOR (runs west from SOUTH_CORRIDOR_2) ===
            NavNode(
                id = "TH_HALL_CORRIDOR_EAST_END",
                position = Position(500f, 620f, 1),
                connections = listOf("SOUTH_CORRIDOR_2", "TH3_DOOR_NODE", "TH_HALL_CORRIDOR_MID")
            ),
            NavNode(
                id = "TH3_DOOR_NODE",
                position = Position(500f, 670f, 1),
                connections = listOf("TH_HALL_CORRIDOR_EAST_END"),
                type = NodeType.DOOR
            ), // TH3 (est based on relative pos)
            NavNode(
                id = "TH_HALL_CORRIDOR_MID",
                position = Position(380f, 620f, 1),
                connections = listOf(
                    "TH_HALL_CORRIDOR_EAST_END",
                    "TH2_DOOR_NODE",
                    "TH_HALL_CORRIDOR_WEST_END"
                )
            ),
            NavNode(
                id = "TH2_DOOR_NODE",
                position = Position(380f, 670f, 1),
                connections = listOf("TH_HALL_CORRIDOR_MID"),
                type = NodeType.DOOR
            ),    // TH2 (est)
            NavNode(
                id = "TH_HALL_CORRIDOR_WEST_END",
                position = Position(250f, 620f, 1),
                connections = listOf(
                    "TH_HALL_CORRIDOR_MID",
                    "TH1_DOOR_NODE",
                    "W_CORRIDOR_JUNCTION_SOUTH"
                )
            ),
            NavNode(
                id = "TH1_DOOR_NODE",
                position = Position(250f, 670f, 1),
                connections = listOf("TH_HALL_CORRIDOR_WEST_END"),
                type = NodeType.DOOR
            ),    // TH1 (est)

            // === WEST CORRIDOR (from ENTRANCE_WEST northwards, then east) ===
            NavNode(
                id = "W_CORRIDOR_1",
                position = Position(150f, 560f, 1),
                connections = listOf("ENTRANCE_WEST", "W_CORRIDOR_JUNCTION_SOUTH")
            ), // Path from west entrance
            NavNode(
                id = "W_CORRIDOR_JUNCTION_SOUTH",
                position = Position(250f, 560f, 1),
                connections = listOf(
                    "W_CORRIDOR_1",
                    "TH_HALL_CORRIDOR_WEST_END",
                    "PENTADBIRAN_ACCESS_CORRIDOR"
                )
            ),
            NavNode(
                id = "PENTADBIRAN_ACCESS_CORRIDOR",
                position = Position(250f, 400f, 1),
                connections = listOf(
                    "W_CORRIDOR_JUNCTION_SOUTH",
                    "PENTADBIRAN_DOOR_NODE",
                    "TANDAS_L_DOOR_NODE"
                )
            ),
            NavNode(
                id = "PENTADBIRAN_DOOR_NODE",
                position = Position(250f, 270f, 1),
                connections = listOf("PENTADBIRAN_ACCESS_CORRIDOR"),
                type = NodeType.DOOR
            ), // Pejabot Pentadbiran
            NavNode(
                id = "TANDAS_L_DOOR_NODE",
                position = Position(250f, 470f, 1),
                connections = listOf("PENTADBIRAN_ACCESS_CORRIDOR"),
                type = NodeType.DOOR
            ), // Tandas L

            // === CENTRAL JUNCTION & Laman Najib area ===
            // Node representing the southern approach to the central courtyard/junction area from Main South Corridor
            NavNode(
                id = "CENTRAL_JUNCTION_SOUTH_POINT",
                position = Position(582f, 500f, 1),
                connections = listOf(
                    "SOUTH_CORRIDOR_2",
                    "LAMAN_NAJIB_SOUTH_NODE",
                    "LIFT_ACCESS_CORRIDOR_SOUTH"
                )
            ),
            NavNode(
                id = "LAMAN_NAJIB_SOUTH_NODE",
                position = Position(582f, 430f, 1),
                connections = listOf("CENTRAL_JUNCTION_SOUTH_POINT", "LAMAN_NAJIB_CENTER_NODE")
            ), // Path into Laman Najib
            NavNode(
                id = "LAMAN_NAJIB_CENTER_NODE",
                position = Position(582f, 380f, 1),
                connections = listOf(
                    "LAMAN_NAJIB_SOUTH_NODE",
                    "LIFT_ACCESS_CORRIDOR_NORTH",
                    "AKADEMIK_ACCESS_CORRIDOR_SOUTH"
                )
            ), // Center of Laman Najib

            // Lift and Paths around it (West of Laman Najib Center)
            NavNode(
                id = "LIFT_ACCESS_CORRIDOR_SOUTH",
                position = Position(450f, 500f, 1),
                connections = listOf("CENTRAL_JUNCTION_SOUTH_POINT", "LIFT_ACCESS_CORRIDOR_NORTH")
            ), // Path on west side of Laman Najib
            NavNode(
                id = "LIFT_ACCESS_CORRIDOR_NORTH",
                position = Position(450f, 380f, 1),
                connections = listOf(
                    "LIFT_ACCESS_CORRIDOR_SOUTH",
                    "LAMAN_NAJIB_CENTER_NODE",
                    "LIFT_NODE"
                )
            ),
            NavNode(
                id = "LIFT_NODE",
                position = Position(450f, 270f, 1),
                connections = listOf("LIFT_ACCESS_CORRIDOR_NORTH"),
                type = NodeType.ELEVATOR
            ), // RED LIFT (New visually estimated position)

            // Academic Offices & Tandas P (North of Laman Najib / Lift)
            NavNode(
                id = "AKADEMIK_ACCESS_CORRIDOR_SOUTH",
                position = Position(582f, 250f, 1),
                connections = listOf("LAMAN_NAJIB_CENTER_NODE", "AKADEMIK_ACCESS_CORRIDOR_NORTH")
            ),
            NavNode(
                id = "AKADEMIK_ACCESS_CORRIDOR_NORTH",
                position = Position(582f, 180f, 1),
                connections = listOf(
                    "AKADEMIK_ACCESS_CORRIDOR_SOUTH",
                    "AKADEMIK_DOOR_NODE",
                    "TANDAS_P_ADMIN_DOOR_NODE",
                    "NE_CORRIDOR_JUNCTION_WEST"
                )
            ),
            NavNode(
                id = "AKADEMIK_DOOR_NODE",
                position = Position(582f, 100f, 1),
                connections = listOf("AKADEMIK_ACCESS_CORRIDOR_NORTH"),
                type = NodeType.DOOR
            ), // Pejabot Akademik
            NavNode(
                id = "TANDAS_P_ADMIN_DOOR_NODE",
                position = Position(700f, 100f, 1),
                connections = listOf("AKADEMIK_ACCESS_CORRIDOR_NORTH"),
                type = NodeType.DOOR
            ), // Tandas P (Admin)

            // === Path from Central Area (Laman Najib) to East Wing === 
            // (This is the HORIZONTAL BLUE LINE running across the middle of user's drawing)
            NavNode(
                id = "X_TO_COURTYARD_EAST_PATH_1",
                position = Position(800f, 620f, 1),
                connections = listOf("CENTRAL_JUNCTION_SOUTH_POINT", "X_TO_COURTYARD_EAST_PATH_2")
            ), // From main X, going East
            NavNode(
                id = "X_TO_COURTYARD_EAST_PATH_2",
                position = Position(950f, 620f, 1),
                connections = listOf("X_TO_COURTYARD_EAST_PATH_1", "X_TO_COURTYARD_EAST_PATH_3")
            ),
            NavNode(
                id = "X_TO_COURTYARD_EAST_PATH_3",
                position = Position(1080f, 580f, 1),
                connections = listOf("X_TO_COURTYARD_EAST_PATH_2", "EAST_WING_VERTICAL_SOUTH_END")
            ), // End of horizontal, before turning up into East Wing

            // === East Wing Vertical Corridor & Branches ===
            NavNode(
                id = "EAST_WING_VERTICAL_SOUTH_END",
                position = Position(1080f, 500f, 1),
                connections = listOf(
                    "X_TO_COURTYARD_EAST_PATH_3",
                    "EAST_WING_MID_JUNCTION",
                    "CAFE_ACCESS_PATH",
                    "STAIRS_E_S_MID"
                )
            ), // Bottom of East Wing vertical
            NavNode(
                id = "CAFE_ACCESS_PATH",
                position = Position(1080f, 550f, 1),
                connections = listOf("EAST_WING_VERTICAL_SOUTH_END", "CAFE_DOOR_NODE")
            ), // Path to Cafe
            NavNode(
                id = "CAFE_DOOR_NODE",
                position = Position(1100f, 580f, 1),
                connections = listOf("CAFE_ACCESS_PATH"),
                type = NodeType.DOOR
            ), // Cafe Door

            NavNode(
                id = "EAST_WING_MID_JUNCTION",
                position = Position(1080f, 380f, 1),
                connections = listOf(
                    "EAST_WING_VERTICAL_SOUTH_END",
                    "EAST_WING_VERTICAL_NORTH_END",
                    "TH4_ACCESS_PATH"
                )
            ), // Mid-point of East Wing Vertical, TH4 branches West
            NavNode(
                id = "TH4_ACCESS_PATH",
                position = Position(1000f, 380f, 1),
                connections = listOf("EAST_WING_MID_JUNCTION", "TH4_DOOR_NODE")
            ), // Path to TH4 door
            NavNode(
                id = "TH4_DOOR_NODE",
                position = Position(950f, 317f, 1),
                connections = listOf("TH4_ACCESS_PATH"),
                type = NodeType.DOOR
            ), // TH4 Door (Est from POI map)

            NavNode(
                id = "EAST_WING_VERTICAL_NORTH_END",
                position = Position(1080f, 220f, 1),
                connections = listOf(
                    "EAST_WING_MID_JUNCTION",
                    "NE_CORRIDOR_JUNCTION_EAST",
                    "STAIRS_E_N_MID"
                )
            ), // Top of East Wing Vertical

            // === Path from North-East Entrance (ENTRANCE_NORTH_EAST at 1180,80) ===
            NavNode(
                id = "NE_CORRIDOR_1",
                position = Position(1100f, 180f, 1),
                connections = listOf(
                    "ENTRANCE_EAST",
                    "AKADEMIK_ACCESS_CORRIDOR_NORTH",
                    "NE_CORRIDOR_JUNCTION_EAST"
                )
            ),
            NavNode(
                id = "NE_CORRIDOR_JUNCTION_EAST",
                position = Position(1080f, 180f, 1),
                connections = listOf(
                    "NE_CORRIDOR_1",
                    "EAST_WING_VERTICAL_NORTH_END",
                    "UNIT_CAWANGAN_DOOR_NODE",
                    "TH5_DOOR_NODE"
                )
            ), // Junction for Unit Cawangan & TH5
            NavNode(
                id = "UNIT_CAWANGAN_DOOR_NODE",
                position = Position(1000f, 131f, 1),
                connections = listOf("NE_CORRIDOR_JUNCTION_EAST"),
                type = NodeType.DOOR
            ), // Unit Cawangan
            NavNode(
                id = "TH5_DOOR_NODE",
                position = Position(1000f, 222f, 1),
                connections = listOf("NE_CORRIDOR_JUNCTION_EAST"),
                type = NodeType.DOOR
            ), // TH5

            // === YELLOW STAIRS (Re-estimated positions, connected to nearest appropriate node) ===
            NavNode(
                id = "STAIRS_W1",
                position = Position(180f, 620f, 1),
                connections = listOf("W_CORRIDOR_2"),
                type = NodeType.STAIRS
            ),      // Near TH1/West Entrance
            NavNode(
                id = "STAIRS_S1",
                position = Position(730f, 730f, 1),
                connections = listOf("M_CORRIDOR_2_INTERSECTION"),
                type = NodeType.STAIRS
            ), // South near TH3
            NavNode(
                id = "STAIRS_NW1",
                position = Position(700f, 150f, 1),
                connections = listOf("AKADEMIK_ACCESS_CORRIDOR_NORTH"),
                type = NodeType.STAIRS
            ), // Top-Left near Akademik/Tandas P
            NavNode(
                id = "STAIRS_E_S_MID",
                position = Position(1110f, 500f, 1),
                connections = listOf("EAST_WING_VERTICAL_SOUTH_END"),
                type = NodeType.STAIRS
            ), // East Wing, south part of vertical
            NavNode(
                id = "STAIRS_E_N_MID",
                position = Position(1110f, 220f, 1),
                connections = listOf("EAST_WING_VERTICAL_NORTH_END"),
                type = NodeType.STAIRS
            )  // East Wing, north part of vertical
        )
    }

    suspend fun searchPOIs(query: String): Flow<Result<List<PointOfInterest>>> = flow {
        emit(Result.Loading)
        try {
            kotlinx.coroutines.delay(500)

            // POI Coordinates meticulously re-estimated based on the user-provided image 
            // "image_with_poi_labels.png" (showing POIs within their boxes) and scaled 
            // to the 1165.149f (width) x 760.814f (height) canvas.
            val allPOIs = listOf(
                // Bottom Row Theaters (from left to right)
                PointOfInterest(
                    id = "th1_poi",
                    name = "TH 1",
                    description = "Theater Hall 1",
                    category = POICategory.CLASSROOM,
                    position = Position(160f, 705f, 1)
                ),
                PointOfInterest(
                    id = "th2_poi",
                    name = "TH 2",
                    description = "Theater Hall 2",
                    category = POICategory.CLASSROOM,
                    position = Position(320f, 705f, 1)
                ),
                PointOfInterest(
                    id = "th3_poi",
                    name = "TH 3",
                    description = "Theater Hall 3",
                    category = POICategory.CLASSROOM,
                    position = Position(480f, 705f, 1)
                ),

                // Admin Block (Left Side)
                PointOfInterest(
                    id = "pejabot_pentadbiran_poi",
                    name = "Pejabat Pengurusan Pentadbiran FSKM",
                    description = "FSKM Admin Office",
                    category = POICategory.OFFICE,
                    position = Position(230f, 270f, 1)
                ),
                PointOfInterest(
                    id = "tandas_l_poi",
                    name = "Tandas (L)",
                    description = "Ladies Restroom (West)",
                    category = POICategory.RESTROOM,
                    position = Position(230f, 480f, 1)
                ),
                // The "a" region in Pejabat Pentadbiran FSKM is not made a separate POI unless specified.

                // Central Area
                PointOfInterest(
                    id = "lift_poi",
                    name = "Lif",
                    description = "Elevator",
                    category = POICategory.ELEVATOR,
                    position = Position(430f, 270f, 1)
                ),
                PointOfInterest(
                    id = "laman_najib_poi",
                    name = "Laman Najib",
                    description = "Central Courtyard",
                    category = POICategory.LOBBY,
                    position = Position(580f, 380f, 1)
                ),
                PointOfInterest(
                    id = "pejabot_akademik_poi",
                    name = "Pejabot Pengurusan Akademik",
                    description = "Academic Office",
                    category = POICategory.OFFICE,
                    position = Position(530f, 135f, 1)
                ),
                PointOfInterest(
                    id = "tandas_p_admin_poi",
                    name = "Tandas (P) Admin",
                    description = "Men's Restroom (near Akademik)",
                    category = POICategory.RESTROOM,
                    position = Position(700f, 135f, 1)
                ),
                // The "a" region in Pejabot Pengurusan Akademik is not made a separate POI.

                // South-East Area (near bottom-right of Laman Najib)
                PointOfInterest(
                    id = "surau_p_dewan_poi",
                    name = "Surau (P)",
                    description = "Women's Prayer Room (Dewan Area)",
                    category = POICategory.PRAYER_ROOM,
                    position = Position(780f, 560f, 1)
                ),
                PointOfInterest(
                    id = "tandas_p_dewan_poi",
                    name = "Tandas (P) Dewan",
                    description = "Men's Restroom (Dewan Area)",
                    category = POICategory.RESTROOM,
                    position = Position(780f, 600f, 1)
                ),
                PointOfInterest(
                    id = "bilik_karspersky_poi",
                    name = "Bilik Kaspersky",
                    description = "Kaspersky Lab",
                    category = POICategory.LAB,
                    position = Position(900f, 490f, 1)
                ),

                // East Wing (Top Right)
                PointOfInterest(
                    id = "unit_cawangan_poi",
                    name = "Unit Cawangan Zon 4",
                    description = "Zone 4 Unit",
                    category = POICategory.OFFICE,
                    position = Position(930f, 90f, 1)
                ),
                PointOfInterest(
                    id = "th5_poi",
                    name = "TH 5",
                    description = "Theater Hall 5",
                    category = POICategory.CLASSROOM,
                    position = Position(930f, 195f, 1)
                ),
                PointOfInterest(
                    id = "th4_poi",
                    name = "TH 4",
                    description = "Theater Hall 4",
                    category = POICategory.CLASSROOM,
                    position = Position(930f, 300f, 1)
                ),
                PointOfInterest(
                    id = "tandas_l_east_poi",
                    name = "Tandas (L) East",
                    description = "Ladies Restroom (East Wing)",
                    category = POICategory.RESTROOM,
                    position = Position(930f, 405f, 1)
                ), // Labeled Tandas (L) in East Wing
                PointOfInterest(
                    id = "cafe_poi",
                    name = "Cafe",
                    description = "Campus Cafe",
                    category = POICategory.CAFETERIA,
                    position = Position(1030f, 460f, 1)
                ),

                // Entrances (using visual estimations from blue line map for consistency with NavNodes later)
                PointOfInterest(
                    id = "main_entrance_south_poi",
                    name = "Main Entrance (South)",
                    description = "Main South Entrance",
                    category = POICategory.ENTRANCE,
                    position = Position(582f, 740f, 1)
                ),
                PointOfInterest(
                    id = "west_entrance_poi",
                    name = "West Entrance",
                    description = "Side West Entrance",
                    category = POICategory.ENTRANCE,
                    position = Position(80f, 560f, 1)
                ),
                PointOfInterest(
                    id = "east_entrance_ne_poi",
                    name = "NE Entrance",
                    description = "Side North-East Entrance",
                    category = POICategory.ENTRANCE,
                    position = Position(1100f, 180f, 1)
                )
            )

            val filteredPOIs = if (query.isBlank()) {
                allPOIs
            } else {
                allPOIs.filter {
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
}
