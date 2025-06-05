package com.example.indoornavigation20.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: UserRole = UserRole.USER,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

@Serializable
enum class UserRole {
    USER,       // Normal users - can view POIs, navigate, search
    ADMIN       // Admins - can add/edit/delete POIs, access debug features, manage users
}

data class UserPermissions(
    val canAddPOI: Boolean,
    val canEditPOI: Boolean,
    val canDeletePOI: Boolean,
    val canAccessAdmin: Boolean,
    val canAccessDebug: Boolean,
    val canManageUsers: Boolean,
    val canViewAllPOIs: Boolean
) {
    companion object {
        fun forRole(role: UserRole): UserPermissions {
            return when (role) {
                UserRole.USER -> UserPermissions(
                    canAddPOI = false,
                    canEditPOI = false,
                    canDeletePOI = false,
                    canAccessAdmin = false,
                    canAccessDebug = false,
                    canManageUsers = false,
                    canViewAllPOIs = true
                )

                UserRole.ADMIN -> UserPermissions(
                    canAddPOI = true,
                    canEditPOI = true,
                    canDeletePOI = true,
                    canAccessAdmin = true,
                    canAccessDebug = true,
                    canManageUsers = true,
                    canViewAllPOIs = true
                )
            }
        }
    }
}