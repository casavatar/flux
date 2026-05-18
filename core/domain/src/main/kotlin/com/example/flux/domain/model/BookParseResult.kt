package com.example.flux.domain.model

sealed class BookParseResult {

    data class Success(
        val book: Book,
        val chapters: List<Chapter>,
        val readingOrder: List<ReadingOrderItem>,
        val estimatedPageCount: Int?,
    ) : BookParseResult()

    sealed class Error : BookParseResult() {
        data object DrmProtected : Error()
        data class CorruptFile(val cause: Throwable) : Error()
        data class Unknown(val cause: Throwable) : Error()
    }
}
