package com.example.flux.domain.bionic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.system.measureTimeMillis

class BionicEngineTest {

    private val engine = BionicEngine()
    private val en = Locale.ENGLISH

    // ── empty input ───────────────────────────────────────────────────────────

    @Test
    fun emptyString_returnsEmptyList() {
        assertEquals(emptyList<StyledWord>(), engine.annotate("", locale = en))
    }

    // ── non-word tokens are skipped ───────────────────────────────────────────

    @Test
    fun whitespaceToken_isSkipped() {
        val result = engine.annotate("hello world", locale = en)
        val spaceToken = result.first { it.boldPart == "" && it.tail == " " }
        assertTrue(spaceToken.isSkipped)
    }

    @Test
    fun punctuationToken_isSkipped() {
        val result = engine.annotate("hello, world", locale = en)
        val comma = result.first { it.tail == "," }
        assertTrue(comma.isSkipped)
        assertEquals("", comma.boldPart)
    }

    @Test
    fun numericToken_isSkipped() {
        val result = engine.annotate("page 42 now", locale = en)
        val number = result.first { it.tail == "42" }
        assertTrue(number.isSkipped)
        assertEquals("", number.boldPart)
    }

    // ── short words are skipped (minWordLength = 3 by default) ───────────────

    @Test
    fun oneLetterWord_isSkipped() {
        val result = engine.annotate("I go", locale = en)
        val word = result.first { it.tail == "I" || it.boldPart + it.tail == "I" }
        assertTrue(word.isSkipped)
    }

    @Test
    fun twoLetterWord_isSkipped() {
        val result = engine.annotate("to go", locale = en)
        // both "to" and "go" are 2 letters and must be skipped
        val words = result.filter { (it.boldPart + it.tail).matches(Regex("\\p{L}+")) }
        words.forEach { assertTrue("'${it.boldPart + it.tail}' should be skipped", it.isSkipped) }
    }

    @Test
    fun threeLetterWord_isNotSkipped() {
        val result = engine.annotate("the", locale = en)
        val word = result.first { (it.boldPart + it.tail) == "the" }
        assertFalse(word.isSkipped)
    }

    // ── bold length uses ceil(length * intensity) ─────────────────────────────

    @Test
    fun fiveLetterWord_boldTwo_atDefaultIntensity() {
        // ceil(5 * 0.4) = ceil(2.0) = 2
        val result = engine.annotate("hello", locale = en)
        val word = result.first { (it.boldPart + it.tail) == "hello" }
        assertEquals("he", word.boldPart)
        assertEquals("llo", word.tail)
        assertFalse(word.isSkipped)
    }

    @Test
    fun fiveLetterWord_boldThree_atHalfIntensity() {
        // ceil(5 * 0.5) = ceil(2.5) = 3
        val result = engine.annotate("hello", intensity = 0.5f, locale = en)
        val word = result.first { (it.boldPart + it.tail) == "hello" }
        assertEquals("hel", word.boldPart)
        assertEquals("lo", word.tail)
    }

    @Test
    fun fiveLetterWord_allBold_atMaxIntensity() {
        // ceil(5 * 1.0) = 5 → coerceIn(1, 5) = 5
        val result = engine.annotate("hello", intensity = BionicEngine.MAX_INTENSITY, locale = en)
        val word = result.first { (it.boldPart + it.tail) == "hello" }
        assertEquals("hello", word.boldPart)
        assertEquals("", word.tail)
        assertFalse(word.isSkipped)
    }

    @Test
    fun fiveLetterWord_boldOne_atMinIntensity() {
        // ceil(5 * 0.1) = ceil(0.5) = 1
        val result = engine.annotate("hello", intensity = BionicEngine.MIN_INTENSITY, locale = en)
        val word = result.first { (it.boldPart + it.tail) == "hello" }
        assertEquals("h", word.boldPart)
        assertEquals("ello", word.tail)
    }

    @Test
    fun sevenLetterWord_boldThree_atDefaultIntensity() {
        // ceil(7 * 0.4) = ceil(2.8) = 3
        val result = engine.annotate("reading", locale = en)
        val word = result.first { (it.boldPart + it.tail) == "reading" }
        assertEquals("rea", word.boldPart)
        assertEquals("ding", word.tail)
    }

    @Test
    fun threeLetterWord_boldTwo_atDefaultIntensity() {
        // ceil(3 * 0.4) = ceil(1.2) = 2
        val result = engine.annotate("the", locale = en)
        val word = result.first { (it.boldPart + it.tail) == "the" }
        assertEquals("th", word.boldPart)
        assertEquals("e", word.tail)
    }

    // ── Spanish / accented characters ─────────────────────────────────────────

    @Test
    fun accentedWord_treatedAsNormalWord() {
        // "café" = 4 Unicode letters; ceil(4 * 0.4) = ceil(1.6) = 2
        val result = engine.annotate("café", locale = en)
        val word = result.first { (it.boldPart + it.tail) == "café" }
        assertEquals("ca", word.boldPart)
        assertEquals("fé", word.tail)
        assertFalse(word.isSkipped)
    }

    @Test
    fun spanishSentence_accentedCharsCount() {
        // "niño" = 4 Unicode letters; ceil(4 * 0.4) = 2
        val result = engine.annotate("niño bonito", locale = Locale("es", "ES"))
        val nino = result.first { (it.boldPart + it.tail) == "niño" }
        assertEquals("ni", nino.boldPart)
    }

    // ── German long compound word ─────────────────────────────────────────────

    @Test
    fun longGermanCompoundWord_boldedCorrectly() {
        // "Donaudampfschifffahrt" = 21 chars; ceil(21 * 0.4) = ceil(8.4) = 9
        val result = engine.annotate("Donaudampfschifffahrt", locale = Locale.GERMAN)
        val word = result.first { (it.boldPart + it.tail) == "Donaudampfschifffahrt" }
        assertEquals("Donaudamp", word.boldPart)
        assertEquals("fschifffahrt", word.tail)
    }

    // ── CJK characters ────────────────────────────────────────────────────────

    @Test
    fun cjkToken_isNotSkipped() {
        val result = engine.annotate("日本語")
        val cjkTokens = result.filter { it.boldPart.isNotEmpty() || it.tail.isNotEmpty() }
        assertTrue("Expected at least one CJK token", cjkTokens.isNotEmpty())
        cjkTokens.forEach { token ->
            assertFalse("CJK token should not be skipped: $token", token.isSkipped)
        }
    }

    @Test
    fun cjkToken_boldPartIsNonEmpty() {
        val result = engine.annotate("日本語")
        val boldCjk = result.filter { !it.isSkipped && it.boldPart.isNotEmpty() }
        assertTrue("CJK tokens should have non-empty boldPart", boldCjk.isNotEmpty())
    }

    // ── intensity clamping ────────────────────────────────────────────────────

    @Test
    fun intensityBelowMin_clampedToMin() {
        assertEquals(
            engine.annotate("hello", BionicEngine.MIN_INTENSITY, locale = en),
            engine.annotate("hello", -99f, locale = en),
        )
    }

    @Test
    fun intensityAboveMax_clampedToMax() {
        assertEquals(
            engine.annotate("hello", BionicEngine.MAX_INTENSITY, locale = en),
            engine.annotate("hello", 99f, locale = en),
        )
    }

    // ── custom minWordLength ──────────────────────────────────────────────────

    @Test
    fun minWordLength1_singleLetterNotSkipped() {
        val result = engine.annotate("I go", minWordLength = 1, locale = en)
        val iWord = result.first { (it.boldPart + it.tail) == "I" }
        assertFalse(iWord.isSkipped)
    }

    @Test
    fun minWordLength5_fourLetterWordSkipped() {
        val result = engine.annotate("word", minWordLength = 5, locale = en)
        val word = result.first { (it.boldPart + it.tail) == "word" }
        assertTrue(word.isSkipped)
    }

    // ── roundtrip fidelity ────────────────────────────────────────────────────

    @Test
    fun segments_concatenateToOriginalText() {
        val inputs = listOf(
            "The quick brown fox jumps over the lazy dog.",
            "  leading spaces",
            "trailing spaces  ",
            "Hello, World! How are you?",
            "page 42 of 100",
            "don't stop believing",
            "niño, café, résumé",
        )
        for (text in inputs) {
            val reconstructed = engine.annotate(text, locale = en).joinToString("") { it.boldPart + it.tail }
            assertEquals("roundtrip failed for: '$text'", text, reconstructed)
        }
    }

    // ── correctness invariants ────────────────────────────────────────────────

    @Test
    fun boldPart_isAlwaysPrefixOfOriginalWord() {
        val text = "The quick brown fox jumps over the lazy dog."
        val words = Regex("""\p{L}+""").findAll(text).map { it.value }.toList()
        val annotated = engine.annotate(text, locale = en)
        val nonSkipped = annotated.filter { !it.isSkipped }
        nonSkipped.zip(words.filter { it.length >= BionicEngine.DEFAULT_MIN_WORD_LENGTH }).forEach { (sw, word) ->
            assertTrue("'${sw.boldPart}' should be a prefix of '$word'", word.startsWith(sw.boldPart))
        }
    }

    @Test
    fun allNonSkipped_haveLengthAtLeastMinWordLength() {
        val result = engine.annotate("I go to the supermarket now.", locale = en)
        result.filter { !it.isSkipped && (it.boldPart + it.tail).matches(Regex("\\p{L}+")) }
            .forEach { sw ->
                val word = sw.boldPart + sw.tail
                assertTrue("'$word' shorter than minWordLength", word.length >= BionicEngine.DEFAULT_MIN_WORD_LENGTH)
            }
    }

    // ── determinism ───────────────────────────────────────────────────────────

    @Test
    fun sameInput_alwaysProducesSameOutput() {
        val text = "word ".repeat(300)
        assertEquals(
            engine.annotate(text, locale = en),
            engine.annotate(text, locale = en),
        )
    }

    // ── performance: 10k words < 20ms on JVM ─────────────────────────────────

    @Test
    fun annotate_10kWords_completesWithin20ms() {
        val text = "word ".repeat(10_000)
        val elapsed = measureTimeMillis { engine.annotate(text, locale = en) }
        assertTrue("annotation took ${elapsed}ms, expected < 20ms", elapsed < 20L)
    }
}
