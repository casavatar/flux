package com.example.flux.domain.paginator

/**
 * Abstracts text-layout measurement so [TextPaginator] stays free of Compose imports
 * and is independently unit-testable with a synthetic implementation.
 *
 * Implementations must be thread-safe; [TextPaginator] dispatches calls to
 * [Dispatchers.Default] and may invoke this from multiple coroutines.
 */
fun interface LineMeasurer {
    /**
     * Lay out [text] constrained to [maxWidthPx] pixels at [fontSizeSp] scaled-pixels
     * and return one [LineInfo] per wrapped line.
     */
    fun measureLines(text: String, fontSizeSp: Int, maxWidthPx: Int): List<LineInfo>
}
