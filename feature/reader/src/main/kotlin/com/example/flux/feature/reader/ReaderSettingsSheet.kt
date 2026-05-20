package com.example.flux.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flux.domain.model.NightMode
import com.example.flux.feature.reader.model.ReaderIntent
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    bionicEnabled: Boolean,
    bionicIntensity: Float,
    fontSizeSp: Int,
    nightMode: NightMode,
    onIntent: (ReaderIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Bionic Reading", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = bionicEnabled,
                    onCheckedChange = { onIntent(ReaderIntent.SetBionicEnabled(it)) },
                )
            }

            AnimatedVisibility(visible = bionicEnabled) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Intensity", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${(bionicIntensity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Slider(
                        value = bionicIntensity,
                        onValueChange = { onIntent(ReaderIntent.SetBionicIntensity(it)) },
                        valueRange = 0.3f..0.8f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Font Size", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${fontSizeSp}sp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = fontSizeSp.toFloat(),
                onValueChange = { onIntent(ReaderIntent.SetFontSize(it.roundToInt())) },
                valueRange = 14f..28f,
                steps = 13,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            Text("Theme", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NightMode.entries.forEach { mode ->
                    FilterChip(
                        selected = nightMode == mode,
                        onClick = { onIntent(ReaderIntent.SetNightMode(mode)) },
                        label = {
                            Text(mode.name.lowercase().replaceFirstChar { it.titlecase() })
                        },
                    )
                }
            }
        }
    }
}
