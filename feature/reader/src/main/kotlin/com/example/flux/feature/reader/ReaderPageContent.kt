package com.example.flux.feature.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.flux.feature.reader.model.ReaderPage

@Composable
fun ReaderPageContent(
    page: ReaderPage,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onTap,
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = page.text,
            style = MaterialTheme.typography.bodyLarge,
            overflow = TextOverflow.Clip,
        )
    }
}
