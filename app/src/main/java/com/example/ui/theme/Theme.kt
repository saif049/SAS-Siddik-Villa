package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = VillaTealPrimary,
    onPrimary = VillaTealOnPrimary,
    primaryContainer = VillaTealPrimaryContainer,
    onPrimaryContainer = VillaTealOnPrimaryContainer,
    secondary = VillaAmberSecondary,
    onSecondary = VillaAmberOnSecondary,
    secondaryContainer = VillaAmberSecondaryContainer,
    onSecondaryContainer = VillaAmberOnSecondaryContainer,
    tertiary = VillaNavyTertiary,
    onTertiary = VillaNavyOnTertiary,
    tertiaryContainer = VillaNavyTertiaryContainer,
    onTertiaryContainer = VillaNavyOnTertiaryContainer,
    background = VillaBackground,
    surface = VillaSurface,
    onSurface = VillaOnSurface,
    surfaceVariant = VillaSurfaceVariant,
    onSurfaceVariant = VillaOnSurfaceVariant,
    outline = VillaOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = VillaDarkPrimary,
    onPrimary = VillaDarkOnPrimary,
    primaryContainer = VillaDarkPrimaryContainer,
    onPrimaryContainer = VillaDarkPrimaryContainer,
    secondary = VillaDarkSecondary,
    onSecondary = VillaDarkOnSecondary,
    secondaryContainer = VillaDarkSecondaryContainer,
    onSecondaryContainer = VillaDarkOnSecondaryContainer,
    tertiary = VillaDarkTertiary,
    onTertiary = VillaDarkOnTertiary,
    tertiaryContainer = VillaDarkTertiaryContainer,
    onTertiaryContainer = VillaDarkOnTertiaryContainer,
    background = VillaDarkBackground,
    surface = VillaDarkSurface,
    onSurface = VillaDarkOnSurface,
    surfaceVariant = VillaDarkSurfaceVariant,
    onSurfaceVariant = VillaDarkOnSurfaceVariant,
    outline = VillaDarkOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded luxury colors consistent
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
