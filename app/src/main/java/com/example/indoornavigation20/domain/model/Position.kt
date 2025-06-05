package com.example.indoornavigation20.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Position(
    val x: Float,
    val y: Float,
    val floor: Int,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class NavNode(
    val id: String,
    val position: Position,
    val connections: List<String> = emptyList(),
    val isWalkable: Boolean = true,
    val type: NodeType = NodeType.WALKWAY,
    val isUserCreated: Boolean = false
)

@Serializable
enum class NodeType {
    WALKWAY, DOOR, ELEVATOR, STAIRS, OBSTACLE
}
