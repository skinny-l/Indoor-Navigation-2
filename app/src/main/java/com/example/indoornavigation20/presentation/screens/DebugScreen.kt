package com.example.indoornavigation20.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.indoornavigation20.testing.FirebaseTestHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firebaseTestHelper = FirebaseTestHelper()
    val scope = rememberCoroutineScope()

    var testResults by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isRunningTests by remember { mutableStateOf(false) }
    var userInfo by remember { mutableStateOf("Loading...") }

    // Update user info when auth state changes
    LaunchedEffect(Unit) {
        userInfo = firebaseTestHelper.getCurrentUserInfo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug & Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Authentication Status
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (auth.currentUser != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Authentication Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = userInfo,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                userInfo = firebaseTestHelper.getCurrentUserInfo()
                            }
                        }
                    ) {
                        Text("Refresh Auth Status")
                    }
                }
            }

            // Firebase Connection Tests
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firebase Tests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test Results
                    testResults.forEach { (testName, success) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (success)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$testName: ${if (success) "PASS" else "FAIL"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isRunningTests = true
                                testResults = emptyMap()

                                try {
                                    // Test Firestore connection
                                    val firestoreTest = firebaseTestHelper.testFirebaseConnection()
                                    testResults =
                                        testResults + ("Firestore Connection" to firestoreTest)

                                    // Test POI creation
                                    val poiTest = firebaseTestHelper.testPOICreation()
                                    testResults = testResults + ("POI Creation" to poiTest)

                                } catch (e: Exception) {
                                    testResults = testResults + ("General Test" to false)
                                } finally {
                                    isRunningTests = false
                                }
                            }
                        },
                        enabled = !isRunningTests
                    ) {
                        if (isRunningTests) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Run Firebase Tests")
                    }
                }
            }

            // Common Error Solutions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Common Issues & Solutions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val commonIssues = listOf(
                        "Network Error" to "Check your internet connection and try again",
                        "Email Already Exists" to "Use the Sign In option instead of Sign Up",
                        "Weak Password" to "Use at least 6 characters with letters and numbers",
                        "Invalid Email" to "Make sure your email address is properly formatted",
                        "Too Many Requests" to "Wait a few minutes before trying again",
                        "POIs Not Saving" to "Check if you're signed in and have internet connection"
                    )

                    commonIssues.forEach { (issue, solution) ->
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "• $issue:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "  $solution",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
