package com.example.indoornavigation20.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import android.util.Patterns
import androidx.annotation.RequiresOptIn
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.indoornavigation20.data.repository.UserManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()

    fun getReadableErrorMessage(exception: Exception?): String {
        return when (exception?.message) {
            "The email address is already in use by another account." ->
                "This email is already registered. Try signing in instead."

            "The email address is badly formatted." ->
                "Please enter a valid email address."

            "The password is invalid or the user does not have a password." ->
                "Password is too weak. Use at least 6 characters with letters and numbers."

            "The given password is invalid. [ Password should be at least 6 characters ]" ->
                "Password must be at least 6 characters long."

            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." ->
                "Network error. Please check your internet connection and try again."

            "We have blocked all requests from this device due to unusual activity. Try again later." ->
                "Too many attempts. Please wait a few minutes before trying again."

            else -> when {
                exception?.message?.contains(
                    "CONFIGURATION_NOT_FOUND",
                    ignoreCase = true
                ) == true ->
                    "Firebase Authentication is not properly configured. Please contact the app developer to enable Authentication in Firebase Console."

                exception?.message?.contains("internal error", ignoreCase = true) == true &&
                        exception?.message?.contains(
                            "CONFIGURATION_NOT_FOUND",
                            ignoreCase = true
                        ) == true ->
                    "Firebase Authentication is not properly configured. Please contact the app developer to enable Authentication in Firebase Console."

                exception?.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection."

                exception?.message?.contains("weak-password", ignoreCase = true) == true ->
                    "Password is too weak. Use at least 6 characters with letters and numbers."

                exception?.message?.contains("email-already-in-use", ignoreCase = true) == true ->
                    "This email is already registered. Try signing in instead."

                exception?.message?.contains("invalid-email", ignoreCase = true) == true ->
                    "Please enter a valid email address."

                exception?.message?.contains("too-many-requests", ignoreCase = true) == true ->
                    "Too many attempts. Please wait a few minutes before trying again."

                else -> exception?.message ?: "Sign up failed. Please try again."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Join us to save your favorite locations",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = {
                displayName = it
                errorMessage = null // Clear error when user types
            },
            label = { Text("Display Name") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = "Name")
            },
            isError = displayName.isBlank() && errorMessage != null,
            supportingText = if (displayName.isBlank() && errorMessage != null) {
                { Text("Display name is required") }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null // Clear error when user types
            },
            label = { Text("Email") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = email.isBlank() && errorMessage != null,
            supportingText = if (email.isBlank() && errorMessage != null) {
                { Text("Email is required") }
            } else if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                { Text("Please enter a valid email address") }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null // Clear error when user types
            },
            label = { Text("Password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = password.length < 6 && password.isNotBlank(),
            supportingText = if (password.isNotBlank() && password.length < 6) {
                { Text("Password must be at least 6 characters") }
            } else if (password.isBlank() && errorMessage != null) {
                { Text("Password is required") }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null // Clear error when user types
            },
            label = { Text("Confirm Password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Confirm Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = confirmPassword.isNotBlank() && password != confirmPassword,
            supportingText = if (confirmPassword.isNotBlank() && password != confirmPassword) {
                { Text("Passwords do not match") }
            } else if (confirmPassword.isBlank() && errorMessage != null) {
                { Text("Please confirm your password") }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Button(
            onClick = {
                // Validate inputs
                when {
                    displayName.isBlank() -> {
                        errorMessage = "Please enter your display name"
                        return@Button
                    }

                    email.isBlank() -> {
                        errorMessage = "Please enter your email address"
                        return@Button
                    }

                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        errorMessage = "Please enter a valid email address"
                        return@Button
                    }

                    password.isBlank() -> {
                        errorMessage = "Please enter a password"
                        return@Button
                    }

                    password.length < 6 -> {
                        errorMessage = "Password must be at least 6 characters long"
                        return@Button
                    }

                    confirmPassword.isBlank() -> {
                        errorMessage = "Please confirm your password"
                        return@Button
                    }

                    password != confirmPassword -> {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }
                }

                isLoading = true
                errorMessage = null

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            // Update display name
                            val user = auth.currentUser
                            val profileUpdates =
                                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build()
                            user?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        // Create user profile in our database
                                        val userManager = UserManager()
                                        MainScope().launch {
                                            userManager.createOrUpdateUser(user)
                                        }
                                        onSignUpSuccess()
                                    } else {
                                        // Profile update failed but account was created
                                        onSignUpSuccess()
                                    }
                                }
                        } else {
                            errorMessage = getReadableErrorMessage(task.exception)
                        }
                    }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onNavigateToLogin
        ) {
            Text("Already have an account? Sign In")
        }
    }
}
