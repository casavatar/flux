package com.example.flux.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class FluxThemeScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        showSystemUi = false,
    )

    @Test
    fun light_fontScale1() {
        paparazzi.snapshot {
            FluxTheme(darkTheme = false, dynamicColor = false) {
                ThemePreviewContent()
            }
        }
    }

    @Test
    fun dark_fontScale1() {
        paparazzi.snapshot {
            FluxTheme(darkTheme = true, dynamicColor = false) {
                ThemePreviewContent()
            }
        }
    }

    @Test
    fun light_fontScale1_5() {
        paparazzi.snapshot {
            FluxTheme(darkTheme = false, dynamicColor = false) {
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = 1.5f,
                    )
                ) {
                    ThemePreviewContent()
                }
            }
        }
    }

    @Test
    fun dark_fontScale1_5() {
        paparazzi.snapshot {
            FluxTheme(darkTheme = true, dynamicColor = false) {
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = 1.5f,
                    )
                ) {
                    ThemePreviewContent()
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewContent() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Flux E-Reader", style = MaterialTheme.typography.displaySmall)
            Text("Chapter One", style = MaterialTheme.typography.titleLarge)
            Text(
                "It was the best of times, it was the worst of times, it was the age of wisdom.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Secondary body text at medium scale for metadata and captions.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
