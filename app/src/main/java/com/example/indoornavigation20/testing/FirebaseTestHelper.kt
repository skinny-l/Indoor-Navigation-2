package com.example.indoornavigation20.testing

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseTestHelper {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun testFirebaseConnection(): Boolean {
        return try {
            // Test Firestore connection by creating a test document
            val testData = hashMapOf(
                "test" to "connection",
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("test")
                .document("connection_test")
                .set(testData)
                .await()

            Log.d("FirebaseTest", "Firestore connection successful")
            true
        } catch (e: Exception) {
            Log.e("FirebaseTest", "Firestore connection failed: ${e.message}")
            false
        }
    }

    suspend fun testPOICreation(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: "guest_user"

            val testPOI = hashMapOf(
                "name" to "Test POI",
                "description" to "Test POI for verification",
                "category" to "OTHER",
                "x" to 500.0,
                "y" to 500.0,
                "floor" to 1,
                "accuracy" to 0.0,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("pois")
                .document(userId)
                .collection("user_pois")
                .document("test_poi")
                .set(testPOI)
                .await()

            Log.d("FirebaseTest", "POI creation successful for user: $userId")
            true
        } catch (e: Exception) {
            Log.e("FirebaseTest", "POI creation failed: ${e.message}")
            false
        }
    }

    suspend fun getCurrentUserInfo(): String {
        val user = auth.currentUser
        return if (user != null) {
            val userManager = com.example.indoornavigation20.data.repository.UserManager()
            val userProfile = userManager.getCurrentUser()
            val permissions = userManager.getUserPermissions()

            "User: ${user.email} (${user.uid})\n" +
                    "Display Name: ${user.displayName ?: "None"}\n" +
                    "Role: ${userProfile?.role?.name ?: "Unknown"}\n" +
                    "Can Access Admin: ${permissions.canAccessAdmin}\n" +
                    "Can Add POI: ${permissions.canAddPOI}"
        } else {
            "No user signed in (guest mode)"
        }
    }
}
