package com.example.indoornavigation20.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String,
    val name: String,
    val type: RoomType,
    val bounds: RoomBounds,
    val entrances: List<Position> = emptyList(),
    val description: String = "",
    val capacity: Int? = null,
    val features: List<String> = emptyList()
)

@Serializable
data class RoomBounds(
    val topLeft: Position,
    val bottomRight: Position
)

@Serializable
enum class RoomType {
    CLASSROOM,
    OFFICE,
    HALLWAY,
    STAIRCASE,
    ELEVATOR_SHAFT,
    RESTROOM,
    STORAGE,
    LOBBY,
    CONFERENCE_ROOM,
    LABORATORY,
    LIBRARY,
    CAFETERIA,
    UNKNOWN
}

@Serializable
data class Wall(
    val id: String,
    val startPosition: Position,
    val endPosition: Position,
    val thickness: Float = 1.0f,
    val type: WallType = WallType.INTERIOR
)

@Serializable
enum class WallType {
    EXTERIOR,
    INTERIOR,
    LOAD_BEARING,
    PARTITION
}