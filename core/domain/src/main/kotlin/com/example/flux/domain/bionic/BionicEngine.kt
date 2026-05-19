package com.example.flux.domain.bionic

import java.text.BreakIterator
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil

class BionicEngine @Inject constructor() {

    /**
     * Annotates [text] with Bionic Reading markers using locale-aware word segmentation.
     *
     * Each word token gets its first [intensity] fraction bolded (via `ceil`). Tokens that
     * are non-alphabetic, shorter than [minWordLength], or purely numeric/punctuation are
     * emitted with [StyledWord.isSkipped] = true. Concatenating `boldPart + tail` for every
     * item in the result reconstructs [text] exactly.
     */
    fun annotate(
        text: String,
        intensity: Float = DEFAULT_INTENSITY,
        minWordLength: Int = DEFAULT_MIN_WORD_LENGTH,
        locale: Locale = Locale.getDefault(),
    ): List<StyledWord> {
        if (text.isEmpty()) return emptyList()

        val clampedIntensity = intensity.coerceIn(MIN_INTENSITY, MAX_INTENSITY)
        val result = mutableListOf<StyledWord>()

        val bi = BreakIterator.getWordInstance(locale)
        bi.setText(text)

        var start = bi.first()
        var end = bi.next()
        while (end != BreakIterator.DONE) {
            result += processToken(text.substring(start, end), clampedIntensity, minWordLength)
            start = end
            end = bi.next()
        }

        return result
    }

    private fun processToken(token: String, intensity: Float, minWordLength: Int): StyledWord {
        if (token.none { it.isLetter() }) {
            return StyledWord(boldPart = "", tail = token, isSkipped = true)
        }

        if (isCjk(token.first())) {
            return StyledWord(boldPart = token, tail = "", isSkipped = false)
        }

        if (token.length < minWordLength) {
            return StyledWord(boldPart = "", tail = token, isSkipped = true)
        }

        val boldLength = ceil(token.length * intensity).toInt().coerceIn(1, token.length)
        return StyledWord(
            boldPart = token.substring(0, boldLength),
            tail = token.substring(boldLength),
            isSkipped = false,
        )
    }

    private fun isCjk(char: Char): Boolean {
        val block = Character.UnicodeBlock.of(char)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.HIRAGANA ||
            block == Character.UnicodeBlock.KATAKANA ||
            block == Character.UnicodeBlock.HANGUL_SYLLABLES
    }

    companion object {
        const val DEFAULT_INTENSITY = 0.4f
        const val MIN_INTENSITY = 0.1f
        const val MAX_INTENSITY = 1.0f
        const val DEFAULT_MIN_WORD_LENGTH = 3
    }
}
