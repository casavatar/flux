package com.example.flux.data.bionic

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.flux.domain.bionic.StyledWord
import javax.inject.Inject

class BionicAnnotator @Inject constructor() {

    /**
     * Converts [BionicEngine] output into a Compose [AnnotatedString].
     *
     * Each non-skipped [StyledWord] gets its [StyledWord.boldPart] wrapped in a
     * [SpanStyle] with [boldWeight]. The [StyledWord.tail] is appended without a
     * span when [normalWeight] is [FontWeight.Normal] (the default), avoiding
     * unnecessary span objects on most of the text. A custom [normalWeight] adds
     * spans on tails too, which lets callers apply a specific weight to all text.
     *
     * Span styles are created once before the loop so that no extra allocation
     * occurs per word.
     */
    fun annotate(
        styledWords: List<StyledWord>,
        boldWeight: FontWeight = FontWeight.Bold,
        normalWeight: FontWeight = FontWeight.Normal,
    ): AnnotatedString {
        val boldStyle = SpanStyle(fontWeight = boldWeight)
        val normalStyle = if (normalWeight != FontWeight.Normal) SpanStyle(fontWeight = normalWeight) else null

        return buildAnnotatedString {
            for (word in styledWords) {
                if (word.isSkipped) {
                    append(word.tail)
                } else {
                    if (word.boldPart.isNotEmpty()) {
                        withStyle(boldStyle) { append(word.boldPart) }
                    }
                    if (word.tail.isNotEmpty()) {
                        if (normalStyle != null) {
                            withStyle(normalStyle) { append(word.tail) }
                        } else {
                            append(word.tail)
                        }
                    }
                }
            }
        }
    }
}
