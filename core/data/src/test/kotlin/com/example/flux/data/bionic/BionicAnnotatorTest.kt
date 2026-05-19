package com.example.flux.data.bionic

import androidx.compose.ui.text.font.FontWeight
import com.example.flux.domain.bionic.StyledWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class BionicAnnotatorTest {

    private val annotator = BionicAnnotator()

    // ── empty input ───────────────────────────────────────────────────────────

    @Test
    fun emptyList_returnsEmptyAnnotatedString() {
        val result = annotator.annotate(emptyList())
        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    // ── single words ──────────────────────────────────────────────────────────

    @Test
    fun wordAllBold_oneBoldSpanCoversWholeWord() {
        val result = annotator.annotate(listOf(StyledWord("hello", "", isSkipped = false)))
        assertEquals("hello", result.text)
        assertEquals(1, result.spanStyles.size)
        with(result.spanStyles[0]) {
            assertEquals(FontWeight.Bold, item.fontWeight)
            assertEquals(0, start)
            assertEquals(5, end)
        }
    }

    @Test
    fun wordWithBoldAndTail_boldSpanCoversOnlyBoldPart() {
        val result = annotator.annotate(listOf(StyledWord("he", "llo", isSkipped = false)))
        assertEquals("hello", result.text)
        assertEquals(1, result.spanStyles.size)
        with(result.spanStyles[0]) {
            assertEquals(FontWeight.Bold, item.fontWeight)
            assertEquals(0, start)
            assertEquals(2, end)
        }
    }

    @Test
    fun skippedWord_noSpans() {
        val result = annotator.annotate(listOf(StyledWord("", "the", isSkipped = true)))
        assertEquals("the", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    // ── multi-word text ───────────────────────────────────────────────────────

    @Test
    fun twoWordsWithSpace_twoBoldSpans_spaceHasNone() {
        val words = listOf(
            StyledWord("he", "llo", false),
            StyledWord("", " ", true),
            StyledWord("wo", "rld", false),
        )
        val result = annotator.annotate(words)
        assertEquals("hello world", result.text)
        assertEquals(2, result.spanStyles.size)
        with(result.spanStyles[0]) {
            assertEquals(FontWeight.Bold, item.fontWeight)
            assertEquals(0, start); assertEquals(2, end)
        }
        with(result.spanStyles[1]) {
            assertEquals(FontWeight.Bold, item.fontWeight)
            assertEquals(6, start); assertEquals(8, end)
        }
    }

    @Test
    fun allSkippedWords_noSpans() {
        val words = listOf(
            StyledWord("", "I", true),
            StyledWord("", " ", true),
            StyledWord("", "go", true),
        )
        val result = annotator.annotate(words)
        assertEquals("I go", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun sentenceWithPunctuation_textIsCorrect() {
        val words = listOf(
            StyledWord("he", "llo", false),
            StyledWord("", ",", true),
            StyledWord("", " ", true),
            StyledWord("wo", "rld", false),
            StyledWord("", "!", true),
        )
        val result = annotator.annotate(words)
        assertEquals("hello, world!", result.text)
        assertEquals(2, result.spanStyles.size)
    }

    // ── bold span index precision ─────────────────────────────────────────────

    @Test
    fun boldSpanStartsAfterSkippedPrefix() {
        // "the " = 4 chars skipped, then "quick" with bold="qui"
        val words = listOf(
            StyledWord("", "the", true),
            StyledWord("", " ", true),
            StyledWord("qui", "ck", false),
        )
        val result = annotator.annotate(words)
        assertEquals("the quick", result.text)
        assertEquals(1, result.spanStyles.size)
        with(result.spanStyles[0]) {
            assertEquals(4, start)
            assertEquals(7, end)
        }
    }

    @Test
    fun multipleWordsSpanIndicesAreContiguous() {
        val words = listOf(
            StyledWord("", "the", true),      // [0,3)  skipped
            StyledWord("", " ", true),         // [3,4)  skipped
            StyledWord("qui", "ck", false),    // [4,9) → bold [4,7)
            StyledWord("", " ", true),         // [9,10) skipped
            StyledWord("br", "own", false),    // [10,15) → bold [10,12)
        )
        val result = annotator.annotate(words)
        assertEquals("the quick brown", result.text)
        assertEquals(2, result.spanStyles.size)
        assertEquals(4, result.spanStyles[0].start); assertEquals(7, result.spanStyles[0].end)
        assertEquals(10, result.spanStyles[1].start); assertEquals(12, result.spanStyles[1].end)
    }

    // ── custom weights ────────────────────────────────────────────────────────

    @Test
    fun customBoldWeight_usedInSpan() {
        val result = annotator.annotate(
            listOf(StyledWord("he", "llo", false)),
            boldWeight = FontWeight.ExtraBold,
        )
        assertEquals(FontWeight.ExtraBold, result.spanStyles[0].item.fontWeight)
    }

    @Test
    fun customNormalWeight_addsSpanOnTail() {
        val result = annotator.annotate(
            listOf(StyledWord("he", "llo", false)),
            normalWeight = FontWeight.Light,
        )
        assertEquals(2, result.spanStyles.size)
        with(result.spanStyles[0]) {
            assertEquals(FontWeight.Bold, item.fontWeight)
            assertEquals(0, start); assertEquals(2, end)
        }
        with(result.spanStyles[1]) {
            assertEquals(FontWeight.Light, item.fontWeight)
            assertEquals(2, start); assertEquals(5, end)
        }
    }

    @Test
    fun defaultNormalWeight_noExtraSpanOnTail() {
        // FontWeight.Normal is the default — tail should get no span
        val result = annotator.annotate(listOf(StyledWord("he", "llo", false)))
        assertEquals(1, result.spanStyles.size)
    }

    @Test
    fun customNormalWeight_appliedToSkippedWordTail() {
        // Skipped words are just appended — normalWeight does NOT apply to them
        val result = annotator.annotate(
            listOf(StyledWord("", "the", true)),
            normalWeight = FontWeight.Light,
        )
        assertTrue(result.spanStyles.isEmpty())
    }

    // ── roundtrip fidelity ────────────────────────────────────────────────────

    @Test
    fun annotatedText_reconstructsOriginalText() {
        val inputs = listOf(
            listOf(StyledWord("he", "llo", false), StyledWord("", " ", true), StyledWord("wo", "rld", false)),
            listOf(StyledWord("", "I", true), StyledWord("", " ", true), StyledWord("go", "", false)),
            listOf(StyledWord("Th", "e", false), StyledWord("", " ", true), StyledWord("qui", "ck", false), StyledWord("", ".", true)),
        )
        val originals = listOf("hello world", "I go", "The quick.")
        inputs.zip(originals).forEach { (words, expected) ->
            assertEquals(expected, annotator.annotate(words).text)
        }
    }

    // ── performance: 10k words < 20ms on JVM ─────────────────────────────────

    @Test
    fun annotate_10kWords_completesWithin20ms() {
        val words = buildList {
            repeat(10_000) {
                add(StyledWord("wo", "rd", false))
                add(StyledWord("", " ", true))
            }
        }
        annotator.annotate(words) // JVM warmup
        val elapsed = measureTimeMillis { annotator.annotate(words) }
        assertTrue("annotation took ${elapsed}ms, expected < 20ms", elapsed < 20L)
    }
}
