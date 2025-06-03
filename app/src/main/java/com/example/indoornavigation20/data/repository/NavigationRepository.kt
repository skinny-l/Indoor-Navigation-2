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
        // SVG viewport is 1550.9 x 835.2, building is 75m x 30m
        val metersPerPixelX = 75.0f / 1550.9f
        val metersPerPixelY = 30.0f / 835.2f

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
            imageUrl = "android.resource://com.example.indoornavigation20/drawable/ground_floor",
            svgUrl = "android.resource://com.example.indoornavigation20/drawable/ground_floor",
            width = 1550.9f,
            height = 835.2f,
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
                id = "pejabat_akademik",
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
        return listOf(
            // Main entrance and central circulation
            NavNode(
                "entrance_main",
                Position(650f, 700f, 1),
                listOf("central_lobby", "main_corridor"),
                type = NodeType.DOOR
            ),
            NavNode(
                "central_lobby",
                Position(650f, 650f, 1),
                listOf("entrance_main", "laman_najib", "lift_access")
            ),
            NavNode(
                "laman_najib_center",
                Position(800f, 450f, 1),
                listOf("central_lobby", "th4_corridor", "th5_corridor", "cafe_access")
            ),

            // Theater Halls access points
            NavNode(
                "th1_entrance",
                Position(444f, 737f, 1),
                listOf("th_corridor_main"),
                type = NodeType.DOOR
            ),
            NavNode(
                "th2_entrance",
                Position(560f, 737f, 1),
                listOf("th_corridor_main"),
                type = NodeType.DOOR
            ),
            NavNode(
                "th3_entrance",
                Position(680f, 737f, 1),
                listOf("th_corridor_main"),
                type = NodeType.DOOR
            ),
            NavNode(
                "th_corridor_main",
                Position(560f, 700f, 1),
                listOf("th1_entrance", "th2_entrance", "th3_entrance", "central_lobby")
            ),

            // Right side theater halls
            NavNode(
                "th4_entrance",
                Position(1256f, 228f, 1),
                listOf("th4_corridor"),
                type = NodeType.DOOR
            ),
            NavNode(
                "th5_entrance",
                Position(1256f, 130f, 1),
                listOf("th5_corridor"),
                type = NodeType.DOOR
            ),
            NavNode(
                "th4_corridor",
                Position(1200f, 228f, 1),
                listOf("th4_entrance", "laman_najib_center", "unit_cawangan_access")
            ),
            NavNode(
                "th5_corridor",
                Position(1200f, 130f, 1),
                listOf("th5_entrance", "pejabot_akademik_access")
            ),

            // Administrative offices access
            NavNode(
                "pejabot_akademik_entrance",
                Position(700f, 113f, 1),
                listOf("pejabot_akademik_access"),
                type = NodeType.DOOR
            ),
            NavNode(
                "pejabot_akademik_access",
                Position(700f, 150f, 1),
                listOf("pejabot_akademik_entrance", "th5_corridor", "lift_access")
            ),
            NavNode(
                "pejabot_pentadbiran_entrance",
                Position(509f, 238f, 1),
                listOf("main_corridor"),
                type = NodeType.DOOR
            ),
            NavNode(
                "unit_cawangan_entrance",
                Position(1356f, 316f, 1),
                listOf("unit_cawangan_access"),
                type = NodeType.DOOR
            ),
            NavNode(
                "unit_cawangan_access",
                Position(1200f, 316f, 1),
                listOf("unit_cawangan_entrance", "th4_corridor", "cafe_access")
            ),

            // Utilities and facilities
            NavNode(
                "lift_entrance",
                Position(580f, 200f, 1),
                listOf("lift_access"),
                type = NodeType.ELEVATOR
            ),
            NavNode(
                "lift_access",
                Position(600f, 220f, 1),
                listOf("lift_entrance", "central_lobby", "pejabot_akademik_access")
            ),
            NavNode(
                "cafe_entrance",
                Position(1425f, 450f, 1),
                listOf("cafe_access"),
                type = NodeType.DOOR
            ),
            NavNode(
                "cafe_access",
                Position(1350f, 450f, 1),
                listOf("cafe_entrance", "laman_najib_center", "unit_cawangan_access")
            ),

            // Restroom access
            NavNode(
                "tandas_l_entrance",
                Position(425f, 350f, 1),
                listOf("restroom_corridor"),
                type = NodeType.DOOR
            ),
            NavNode(
                "tandas_p_entrance",
                Position(425f, 420f, 1),
                listOf("restroom_corridor"),
                type = NodeType.DOOR
            ),
            NavNode(
                "restroom_corridor",
                Position(450f, 385f, 1),
                listOf("tandas_l_entrance", "tandas_p_entrance", "main_corridor")
            ),

            // Main corridors
            NavNode(
                "main_corridor",
                Position(550f, 450f, 1),
                listOf(
                    "central_lobby",
                    "pejabot_pentadbiran_entrance",
                    "restroom_corridor",
                    "laman_najib_center"
                )
            )
        )
    }

    suspend fun searchPOIs(query: String): Flow<Result<List<PointOfInterest>>> = flow {
        emit(Result.Loading)
        try {
            kotlinx.coroutines.delay(500)

            val allPOIs = listOf(
                // Theater Halls
                PointOfInterest(
                    id = "th1_poi",
                    name = "TH 1",
                    description = "Theater Hall 1 - Large lecture theater with stadium seating",
                    category = POICategory.CLASSROOM,
                    position = Position(x = 480.15f, y = 779.25f, floor = 1)
                ),
                PointOfInterest(
                    id = "th2_poi",
                    name = "TH 2",
                    description = "Theater Hall 2 - Large lecture theater with stadium seating",
                    category = POICategory.CLASSROOM,
                    position = Position(x = 641.35f, y = 779.25f, floor = 1)
                ),
                PointOfInterest(
                    id = "th3_poi",
                    name = "TH 3",
                    description = "Theater Hall 3 - Large lecture theater with stadium seating",
                    category = POICategory.CLASSROOM,
                    position = Position(x = 807.65f, y = 779.25f, floor = 1)
                ),
                PointOfInterest(
                    id = "th4_poi",
                    name = "TH 4",
                    description = "Theater Hall 4 - Medium lecture theater",
                    category = POICategory.CLASSROOM,
                    position = Position(x = 1356.7f, y = 317.75f, floor = 1)
                ),
                PointOfInterest(
                    id = "th5_poi",
                    name = "TH 5",
                    description = "Theater Hall 5 - Medium lecture theater",
                    category = POICategory.CLASSROOM,
                    position = Position(x = 1356.7f, y = 222.5f, floor = 1)
                ),

                // Administrative Offices
                PointOfInterest(
                    id = "pejabot_akademik_poi",
                    name = "Pejabot Pengurusan Akademik",
                    description = "Academic Management Office - Student academic services",
                    category = POICategory.OFFICE,
                    position = Position(x = 700f, y = 113.5f, floor = 1)
                ),
                PointOfInterest(
                    id = "pejabot_pentadbiran_poi",
                    name = "Pejabot Pengurusan Pentadbiran FSKM",
                    description = "FSKM Administrative Management Office",
                    category = POICategory.OFFICE,
                    position = Position(x = 509.15f, y = 257f, floor = 1)
                ),
                PointOfInterest(
                    id = "unit_cawangan_poi",
                    name = "Unit Cawangan Zon 4",
                    description = "Zone 4 Branch Unit - Student support services",
                    category = POICategory.OFFICE,
                    position = Position(x = 1356.7f, y = 131.75f, floor = 1)
                ),

                // Facilities
                PointOfInterest(
                    id = "cafe_poi",
                    name = "Cafe",
                    description = "Campus cafe - Food, beverages and student gathering space",
                    category = POICategory.CAFETERIA,
                    position = Position(
                        x = (1339.1f + 1549.7f) / 2f,
                        y = (473.7f + 558.2f) / 2f,
                        floor = 1
                    )
                ),
                PointOfInterest(
                    id = "laman_najib_poi",
                    name = "Laman Najib",
                    description = "Central courtyard - Open space for student activities",
                    category = POICategory.LOBBY,
                    position = Position(x = 800f, y = 450f, floor = 1)
                ),

                // Utilities
                PointOfInterest(
                    id = "lift_poi",
                    name = "Lif",
                    description = "Elevator access to all floors",
                    category = POICategory.ELEVATOR,
                    position = Position(x = 695.35f, y = 276.15f, floor = 1)
                ),
                PointOfInterest(
                    id = "tandas_l_poi",
                    name = "Tandas (L)",
                    description = "Ladies restroom facilities",
                    category = POICategory.RESTROOM,
                    position = Position(x = 509.15f, y = 470.45f, floor = 1),
                    accessibility = AccessibilityInfo(wheelchairAccessible = true)
                ),
                PointOfInterest(
                    id = "tandas_p_poi",
                    name = "Tandas (P)",
                    description = "Men's restroom facilities",
                    category = POICategory.RESTROOM,
                    position = Position(x = 855.65f, y = 113.5f, floor = 1),
                    accessibility = AccessibilityInfo(wheelchairAccessible = true)
                ),

                // Main entrance
                PointOfInterest(
                    id = "main_entrance_poi",
                    name = "Main Entrance",
                    description = "Primary building entrance",
                    category = POICategory.ENTRANCE,
                    position = Position(x = 650f, y = 700f, floor = 1)
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
