package com.example.indoornavigation20.data.repository

import com.example.indoornavigation20.domain.model.User
import com.example.indoornavigation20.domain.model.UserRole
import com.example.indoornavigation20.domain.model.UserPermissions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createOrUpdateUser(firebaseUser: com.google.firebase.auth.FirebaseUser): User {
        val displayName =
            firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User"
        val email = firebaseUser.email ?: ""

        // Check if user already exists in database and preserve their role
        val role = try {
            val existingUser = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            if (existingUser.exists()) {
                val existingRole = existingUser.getString("role")
                if (existingRole == "ADMIN") UserRole.ADMIN else UserRole.USER
            } else {
                UserRole.USER // Default role for new users
            }
        } catch (e: Exception) {
            UserRole.USER
        }

        val user = User(
            uid = firebaseUser.uid,
            email = email,
            displayName = displayName,
            role = role,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        // Save user to Firestore
        try {
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()
        } catch (e: Exception) {
            // Handle error silently for now
        }

        return user
    }

    suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null

        return try {
            val doc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            if (doc.exists()) {
                // Read role from Firestore document
                val roleString = doc.getString("role") ?: "USER"
                val role = try {
                    UserRole.valueOf(roleString)
                } catch (e: Exception) {
                    UserRole.USER // Default to USER if role parsing fails
                }

                User(
                    uid = doc.getString("uid") ?: firebaseUser.uid,
                    email = doc.getString("email") ?: firebaseUser.email ?: "",
                    displayName = doc.getString("displayName") ?: firebaseUser.displayName
                    ?: "User",
                    role = role,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    lastLoginAt = doc.getLong("lastLoginAt") ?: System.currentTimeMillis()
                )
            } else {
                // Create user if doesn't exist
                createOrUpdateUser(firebaseUser)
            }
        } catch (e: Exception) {
            // Fallback - try to create user
            try {
                createOrUpdateUser(firebaseUser)
            } catch (e2: Exception) {
                // Final fallback to basic user info
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName
                        ?: firebaseUser.email?.substringBefore("@") ?: "User",
                    role = UserRole.USER
                )
            }
        }
    }

    suspend fun getUserPermissions(): UserPermissions {
        val user = getCurrentUser()
        val permissions = UserPermissions.forRole(user?.role ?: UserRole.USER)

        // Add logging for debugging
        android.util.Log.d(
            "UserManager",
            "User: ${user?.email}, Role: ${user?.role}, Permissions: canAccessAdmin=${permissions.canAccessAdmin}"
        )

        return permissions
    }

    fun isCurrentUserAdmin(): Boolean {
        // This will be checked async, but for quick checks we can try
        return false // Will be properly checked via getUserPermissions()
    }
}
