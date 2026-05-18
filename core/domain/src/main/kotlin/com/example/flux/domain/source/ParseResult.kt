package com.example.flux.domain.source

sealed class ParseResult {
    data class Page(val index: Int, val text: String, val totalPages: Int) : ParseResult()
    data class Metadata(val title: String, val author: String?, val coverUri: String?) : ParseResult()
    data class Error(val cause: Throwable) : ParseResult()
}
