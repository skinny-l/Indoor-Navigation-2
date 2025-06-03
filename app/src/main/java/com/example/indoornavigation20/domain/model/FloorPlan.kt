package com.example.indoornavigation20.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FloorPlan(
    val id: String,
    val buildingId: String,
    val floorNumber: Int,
    val name: String,
    val imageUrl: String,
    val svgUrl: String? = null,
    val width: Float,
    val height: Float,
    val scale: Float = 1.0f,
    val nodes: List<NavNode> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val walls: List<Wall> = emptyList(),
    val coordinateSystem: CoordinateSystem? = null
)
