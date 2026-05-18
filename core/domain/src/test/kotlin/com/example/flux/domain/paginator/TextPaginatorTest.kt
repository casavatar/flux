package com.example.flux.domain.paginator

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Tests the pagination algorithm using a synthetic [LineMeasurer] that models a
 * monospaced font: charWidth = fontSizeSp * 0.5 px, lineHeight = fontSizeSp * 1.5 px.
 * This removes any Compose or Android dependency from the unit test.
 */
class TextPaginatorTest {

    // Viewport: 360 × 600 px (matches a typical phone at 1× density for easy maths)
    private val viewportWidth = 360
    private val viewportHeight = 600

    // ── helpers ──────────────────────────────────────────────────────────────────

    /** Fake measurer: monospaced, charWidthPx = sp*0.5, lineHeightPx = sp*1.5. */
    private fun fakeMeasurer(): LineMeasurer = LineMeasurer { text, fontSizeSp, maxWidthPx ->
        val charWidthPx = fontSizeSp * 0.5f
        val lineHeightPx = fontSizeSp * 1.5f
        val charsPerLine = (maxWidthPx / charWidthPx).toInt().coerceAtLeast(1)

        val lines = mutableListOf<LineInfo>()
        var pos = 0
        var lineNum = 0

        while (pos < text.length) {
            var lineEnd = minOf(pos + charsPerLine, text.length)
            // Preserve word boundary
            if (lineEnd < text.length) {
                val lastSpace = text.lastIndexOf(' ', lineEnd - 1)
                if (lastSpace > pos) lineEnd = lastSpace + 1
            }
            // Guard: never stall on a word longer than the line width
            if (lineEnd == pos) lineEnd = pos + 1

            lines += LineInfo(
                start = pos,
                end = lineEnd,
                topPx = lineNum * lineHeightPx,
                bottomPx = (lineNum + 1) * lineHeightPx,
            )
            pos = lineEnd
            lineNum++
        }
        lines
    }

    private fun paginator() = TextPaginator(fakeMeasurer())

    // ── correctness tests ─────────────────────────────────────────────────────

    @Test
    fun emptyText_returnsOneEmptyPage() = runTest {
        val pages = paginator().paginate("", viewportWidth, viewportHeight, fontSizeSp = 18)
        assertEquals(listOf(""), pages)
    }

    @Test
    fun shortText_fitsOnOnePage() = runTest {
        val pages = paginator().paginate("Hello world.", viewportWidth, viewportHeight, fontSizeSp = 18)
        assertEquals(1, pages.size)
        assertTrue(pages[0].contains("Hello world"))
    }

    @Test
    fun longText_splitsIntoMultiplePages() = runTest {
        // ~5 000 chars → well above one page at 18sp
        val text = "word ".repeat(1_000)
        val pages = paginator().paginate(text, viewportWidth, viewportHeight, fontSizeSp = 18)
        assertTrue("Expected > 1 page, got ${pages.size}", pages.size > 1)
    }

    @Test
    fun pageIndicesAreContiguousAndCoverAllText() = runTest {
        val text = "word ".repeat(500)
        val pages = paginator().paginate(text, viewportWidth, viewportHeight, fontSizeSp = 18)
        // Reconstructed text should contain all tokens from the original
        val reconstructed = pages.joinToString(" ").replace(Regex("\\s+"), " ").trim()
        val original = text.trim().replace(Regex("\\s+"), " ")
        assertEquals(original, reconstructed)
    }

    // ── 10 font sizes (14sp – 36sp) ──────────────────────────────────────────

    private val fontSizes = listOf(14, 16, 18, 20, 22, 24, 26, 28, 32, 36)
    private val referenceText = "word ".repeat(500)

    @Test
    fun largerFontProducesMorePages() = runTest {
        val pageCounts = fontSizes.map { sp ->
            paginator().paginate(referenceText, viewportWidth, viewportHeight, fontSizeSp = sp).size
        }
        // Each larger font must produce >= pages as the previous size
        for (i in 1 until pageCounts.size) {
            assertTrue(
                "Font ${fontSizes[i]}sp (${pageCounts[i]} pages) should have >= pages than " +
                    "${fontSizes[i - 1]}sp (${pageCounts[i - 1]} pages)",
                pageCounts[i] >= pageCounts[i - 1],
            )
        }
    }

    @Test
    fun allFontSizes_produceAtLeastOnePage() = runTest {
        for (sp in fontSizes) {
            val pages = paginator().paginate(referenceText, viewportWidth, viewportHeight, fontSizeSp = sp)
            assertTrue("Font ${sp}sp produced 0 pages", pages.isNotEmpty())
        }
    }

    // ── determinism ───────────────────────────────────────────────────────────

    @Test
    fun paginate_isIdempotentForSameInput() = runTest {
        val text = "word ".repeat(300)
        val p = paginator()
        val first = p.paginate(text, viewportWidth, viewportHeight, fontSizeSp = 18)
        val second = p.paginate(text, viewportWidth, viewportHeight, fontSizeSp = 18)
        assertEquals(first, second)
    }

    // ── performance: DoD requires 50k chars in < 50ms on JVM ─────────────────

    @Test
    fun paginate_50kChars_paginatesWithin50ms() {
        val text = "word ".repeat(10_000) // ~50 k chars
        val p = paginator()
        val elapsed = measureTimeMillis {
            kotlinx.coroutines.runBlocking {
                p.paginate(text, viewportWidth, viewportHeight, fontSizeSp = 18)
            }
        }
        assertTrue("Pagination took ${elapsed}ms, expected < 50ms", elapsed < 50L)
    }

    // ── edge cases ────────────────────────────────────────────────────────────

    @Test
    fun zeroViewportWidth_returnsRawText() = runTest {
        val text = "Hello"
        val pages = paginator().paginate(text, viewportWidth = 0, viewportHeight = 600, fontSizeSp = 18)
        assertEquals(listOf(text), pages)
    }

    @Test
    fun zeroViewportHeight_returnsRawText() = runTest {
        val text = "Hello"
        val pages = paginator().paginate(text, viewportWidth = 360, viewportHeight = 0, fontSizeSp = 18)
        assertEquals(listOf(text), pages)
    }

    @Test
    fun singleVeryLongWord_doesNotLoopInfinitely() = runTest {
        val text = "a".repeat(10_000) // no spaces → line width exceeded on first line
        val pages = paginator().paginate(text, viewportWidth, viewportHeight, fontSizeSp = 18)
        assertTrue(pages.isNotEmpty())
    }
}
