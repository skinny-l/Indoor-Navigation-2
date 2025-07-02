package com.example.indoornavigation20.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Wherezit Brand Colors - Modern Purple & Yellow Theme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),           // Brand Purple Primary
    onPrimary = Color.White,
    secondary = Color(0xFFFBBF24),         // Brand Yellow Primary  
    onSecondary = Color(0xFF1F2937),
    tertiary = Color(0xFFA78BFA),          // Brand Purple Secondary
    onTertiary = Color.White,
    background = Color(0xFF0F172A),        // Deep dark blue-grey
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),           // Slightly lighter surface
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    // Additional modern colors
    primaryContainer = Color(0xFF7C3AED),  // Darker purple
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFFD97706), // Darker yellow
    onSecondaryContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B5CF6),           // Brand Purple Primary
    onPrimary = Color.White,
    secondary = Color(0xFFFBBF24),         // Brand Yellow Primary
    onSecondary = Color(0xFF1F2937),
    tertiary = Color(0xFFA78BFA),          // Brand Purple Secondary
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),        // Clean white background
    onBackground = Color(0xFF1E293B),
    surface = Color.White,                 // Pure white surface
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),    // Very light grey
    onSurfaceVariant = Color(0xFF64748B),
    // Additional modern colors
    primaryContainer = Color(0xFFEDE9FE),  // Light purple tint
    onPrimaryContainer = Color(0xFF5B21B6),
    secondaryContainer = Color(0xFFFEF3C7), // Light yellow tint
    onSecondaryContainer = Color(0xFF92400E)
)

@Composable
fun WherezitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
