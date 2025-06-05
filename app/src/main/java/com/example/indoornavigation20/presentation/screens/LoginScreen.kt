package com.example.indoornavigation20.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
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
import com.example.indoornavigation20.data.repository.UserManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val userManager = UserManager()

    fun getReadableErrorMessage(exception: Exception?): String {
        return when (exception?.message) {
            "There is no user record corresponding to this identifier. The user may have been deleted." ->
                "No account found with this email address. Please check your email or sign up."

            "The password is invalid or the user does not have a password." ->
                "Incorrect password. Please try again."

            "The email address is badly formatted." ->
                "Please enter a valid email address."

            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." ->
                "Network error. Please check your internet connection and try again."

            "We have blocked all requests from this device due to unusual activity. Try again later." ->
                "Too many failed attempts. Please wait a few minutes before trying again."

            "The user account has been disabled by an administrator." ->
                "This account has been disabled. Please contact support."

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

                exception?.message?.contains("user-not-found", ignoreCase = true) == true ->
                    "No account found with this email. Please check your email or sign up."

                exception?.message?.contains("wrong-password", ignoreCase = true) == true ->
                    "Incorrect password. Please try again."

                exception?.message?.contains("invalid-email", ignoreCase = true) == true ->
                    "Please enter a valid email address."

                exception?.message?.contains("user-disabled", ignoreCase = true) == true ->
                    "This account has been disabled. Please contact support."

                exception?.message?.contains("too-many-requests", ignoreCase = true) == true ->
                    "Too many failed attempts. Please wait before trying again."

                else -> exception?.message ?: "Sign in failed. Please try again."
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
            text = "Sign In",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Access your saved locations and preferences",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

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
            isError = password.isBlank() && errorMessage != null,
            supportingText = if (password.isBlank() && errorMessage != null) {
                { Text("Password is required") }
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
                    email.isBlank() -> {
                        errorMessage = "Please enter your email address"
                        return@Button
                    }

                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        errorMessage = "Please enter a valid email address"
                        return@Button
                    }

                    password.isBlank() -> {
                        errorMessage = "Please enter your password"
                        return@Button
                    }
                }

                isLoading = true
                errorMessage = null

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                MainScope().launch {
                                    userManager.createOrUpdateUser(user)
                                }
                            }
                            onLoginSuccess()
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
                Text("Sign In")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onNavigateToSignUp
        ) {
            Text("Don't have an account? Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                if (email.isBlank()) {
                    errorMessage = "Please enter your email address first"
                    return@TextButton
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    errorMessage = "Please enter a valid email address"
                    return@TextButton
                }

                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            errorMessage = "Password reset email sent to $email"
                        } else {
                            errorMessage = when (task.exception?.message) {
                                "There is no user record corresponding to this identifier. The user may have been deleted." ->
                                    "No account found with this email address."

                                else -> "Failed to send reset email. Please try again."
                            }
                        }
                    }
            }
        ) {
            Text("Forgot Password?")
        }
    }
}
