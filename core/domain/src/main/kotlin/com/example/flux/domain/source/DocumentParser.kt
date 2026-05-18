package com.example.flux.domain.source

import com.example.flux.domain.model.Book
import kotlinx.coroutines.flow.Flow

/**
 * Returns parsed document content as a stream of [ParseResult].
 * The first emission is always [ParseResult.Metadata], followed by
 * one [ParseResult.Page] per logical page. Re-emits from scratch
 * whenever display preferences (e.g. font size) change.
 * Implementations must perform all I/O on Dispatchers.IO.
 */
interface DocumentParser {
    suspend fun parse(book: Book): Flow<ParseResult>
}
