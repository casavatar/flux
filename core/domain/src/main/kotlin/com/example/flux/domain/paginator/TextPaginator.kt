package com.example.flux.domain.paginator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Splits raw text into viewport-sized pages using accurate line metrics from [LineMeasurer].
 *
 * The algorithm walks lines in O(n) time, accumulates height, and cuts a new page
 * whenever the next line would exceed [viewportHeight]. Word boundaries are always
 * preserved — Compose's soft-wrap guarantees lines never break mid-word.
 *
 * Callers should wrap results in [PaginationCache] to avoid re-computing on every
 * recomposition. Re-call [paginate] when the viewport or font size changes.
 */
@Singleton
class TextPaginator @Inject constructor(
    private val measurer: LineMeasurer,
) {
    /**
     * @param rawText       Full document text (may be a single chapter or the whole file).
     * @param viewportWidth Available width in pixels (dp × density).
     * @param viewportHeight Available height in pixels (dp × density).
     * @param fontSizeSp    Font size in scaled pixels; drives line-height estimation.
     * @return              Non-empty list of page strings. Each string contains all
     *                      characters that fit within one viewport without overflow.
     */
    suspend fun paginate(
        rawText: String,
        viewportWidth: Int,
        viewportHeight: Int,
        fontSizeSp: Int,
    ): List<String> = withContext(Dispatchers.Default) {
        if (rawText.isEmpty()) return@withContext listOf("")
        if (viewportWidth <= 0 || viewportHeight <= 0) return@withContext listOf(rawText)

        val lines = measurer.measureLines(rawText, fontSizeSp, viewportWidth)
        if (lines.isEmpty()) return@withContext listOf(rawText)

        val pages = mutableListOf<String>()
        var pageFirstLine = 0
        var pageTopY = lines[0].topPx

        for (i in lines.indices) {
            val lineBottom = lines[i].bottomPx
            // Line i overflows the current page — commit everything up to line i-1
            if (lineBottom - pageTopY > viewportHeight && i > pageFirstLine) {
                pages += rawText.substring(lines[pageFirstLine].start, lines[i - 1].end).trimEnd()
                pageFirstLine = i
                pageTopY = lines[i].topPx
            }
        }

        // Remaining lines form the last page
        if (pageFirstLine < lines.size) {
            val tail = rawText.substring(lines[pageFirstLine].start, lines.last().end).trimEnd()
            if (tail.isNotEmpty() || pages.isEmpty()) pages += tail
        }

        pages.ifEmpty { listOf(rawText) }
    }
}
