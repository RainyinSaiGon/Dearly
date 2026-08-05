package com.dearly.app

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.dearly.app.navigation.DearlyNavGraph
import com.dearly.app.ui.theme.DearlyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            DearlyTheme {
                ResponsiveDearlyLayout {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        DearlyNavGraph()
                    }
                }
            }
        }
    }
}

/**
 * Keeps the phone-first screens visually proportional across Android device widths.
 * The designs were made against a 375dp-wide reference device; system font scaling
 * remains intact for accessibility.
 */
@Composable
private fun ResponsiveDearlyLayout(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val baseDensity = LocalDensity.current
    val scale = (configuration.screenWidthDp / 375f).coerceIn(0.85f, 1.20f)

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density * scale,
            fontScale = baseDensity.fontScale
        )
    ) {
        content()
    }
}
