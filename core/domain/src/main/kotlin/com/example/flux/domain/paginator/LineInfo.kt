package com.example.flux.domain.paginator

/** Metrics for a single wrapped line as returned by the text layout engine. */
data class LineInfo(
    val start: Int,   // inclusive char offset in the full text
    val end: Int,     // exclusive char offset (== next line's start)
    val topPx: Float,
    val bottomPx: Float,
)
