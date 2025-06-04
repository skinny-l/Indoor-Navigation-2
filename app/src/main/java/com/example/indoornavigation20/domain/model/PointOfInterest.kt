package com.example.indoornavigation20.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PointOfInterest(
    val id: String,
    val name: String,
    val description: String,
    val category: POICategory,
    val position: Position, // Relative to its floor plan
    val floor: Int? = null, // Optional: if not part of position
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val accessibility: AccessibilityInfo? = null,
    val openingHours: String? = null,
    val contact: String? = null
)

@Serializable
data class AccessibilityInfo(
    val wheelchairAccessible: Boolean = false,
    val hasElevatorAccess: Boolean = false,
    val hasRamp: Boolean = false
)

@Serializable
enum class POICategory {
    CLASSROOM,
    OFFICE,
    LAB,
    CAFETERIA,
    RESTROOM,
    LIBRARY,
    AUDITORIUM,
    ENTRANCE,
    EXIT,
    STAIRS,
    ELEVATOR,
    PARKING,
    INFO_DESK,
    MEETING_ROOM,
    STUDY_AREA,
    LOUNGE,
    SPORTS_FACILITY,
    HALL,        // Added for Dewan Al-Ghazali
    PRAYER_ROOM, // Added for Surau
    LOBBY,       // Added for Laman Najib
    OTHER
}
