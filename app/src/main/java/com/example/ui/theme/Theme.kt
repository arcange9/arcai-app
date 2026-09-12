package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val ArcAiTypography = Typography()

private val DarkColorScheme = darkColorScheme(
    primary = ArcPrimary,
    onPrimary = Color(0xFF001018),
    secondary = ArcSecondary,
    onSecondary = Color.White,
    tertiary = ArcTertiary,
    onTertiary = Color(0xFF001014),
    background = ObsidianBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = ObsidianSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = Color(0xFFB8C5D9),
    outline = Color(0xFF33445F),
    outlineVariant = Color(0xFF22314A)
)

private val LightColorScheme = lightColorScheme(
    primary = ArcPrimaryDark,
    onPrimary = Color.White,
    secondary = ArcSecondary,
    onSecondary = Color.White,
    tertiary = ArcIndigo,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF101828),
    surface = LightSurface,
    onSurface = Color(0xFF101828),
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun ArcAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        typography = ArcAiTypography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = ArcAiTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
