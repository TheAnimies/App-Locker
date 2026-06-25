package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = HighDensityPrimary,
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFF38BDF8),
    background = Color(0xFF0F172A), // Dark slate background
    surface = Color(0xFF1E293B),    // Slate-800 card elements
    onPrimary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = HighDensityError
  )

private val LightColorScheme =
  lightColorScheme(
    primary = HighDensityPrimary,
    primaryContainer = HighDensityPrimaryContainer,
    onPrimaryContainer = HighDensityOnPrimaryContainer,
    background = HighDensityBackground,
    surface = HighDensitySurface,
    onPrimary = Color.White,
    onBackground = HighDensityOnBackground,
    onSurface = HighDensityOnSurface,
    surfaceVariant = HighDensitySurfaceVariant,
    onSurfaceVariant = HighDensityOnSurfaceVariant,
    error = HighDensityError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve custom High Density theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
