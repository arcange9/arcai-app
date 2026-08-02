package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ArcPrimary,
    onPrimary = Color.Black,
    secondary = ArcSecondary,
    onSecondary = Color.White,
    tertiary = ArcTertiary,
    onTertiary = Color.Black,
    background = ObsidianBackground,
    onBackground = Color.White,
    surface = ObsidianSurface,
    onSurface = Color.White,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = Color(0xFFC4CDE0),
    outline = Color(0xFF2C3954)
)

private val LightColorScheme = lightColorScheme(
    primary = ArcPrimaryDark,
    onPrimary = Color.White,
    secondary = ArcSecondary,
    onSecondary = Color.White,
    tertiary = ArcTertiary,
    onTertiary = Color.Black,
    background = LightBackground,
    onBackground = Color(0xFF111827),
    surface = LightSurface,
    onSurface = Color(0xFF111827),
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun ArcAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false by default for ArcAI custom brand colors
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = ArcAiTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
