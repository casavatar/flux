package com.example.flux.feature.reader.model

data class PageCacheKey(
    val bookId: String,
    val pageIndex: Int,
    val intensity: Float,
    val fontSizeSp: Int,
)
