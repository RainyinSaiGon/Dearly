package com.dearly.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLightTeal,
    secondary = SecondaryAmber,
    tertiary = AccentCoral,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryAmber,
    tertiary = AccentCoral,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

@Composable
fun DearlyTheme(
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
        typography = DearlyTypography,
        content = content
    )
}
