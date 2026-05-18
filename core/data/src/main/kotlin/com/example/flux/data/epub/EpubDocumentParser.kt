package com.example.flux.data.epub

import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookParseResult
import com.example.flux.domain.source.DocumentParser
import com.example.flux.domain.source.ParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubDocumentParser @Inject constructor(
    private val epubParser: ReadiumEpubParser,
) : DocumentParser {

    override suspend fun parse(book: Book): Flow<ParseResult> = flow {
        when (val result = epubParser.parse(bookId = book.id, fileUri = book.fileUri)) {
            is BookParseResult.Error.CorruptFile ->
                emit(ParseResult.Error(result.cause))
            is BookParseResult.Error.DrmProtected ->
                emit(ParseResult.Error(Exception("Book is DRM-protected and cannot be read")))
            is BookParseResult.Error.Unknown ->
                emit(ParseResult.Error(result.cause))
            is BookParseResult.Success -> {
                emit(ParseResult.Metadata(
                    title = result.book.title,
                    author = result.book.author,
                    coverUri = result.book.coverUri,
                ))
                val chapters = result.chapters
                val total = chapters.size.coerceAtLeast(1)
                if (chapters.isEmpty()) {
                    emit(ParseResult.Page(index = 0, text = "", totalPages = 1))
                } else {
                    chapters.forEachIndexed { index, chapter ->
                        emit(ParseResult.Page(
                            index = index,
                            text = chapter.title ?: "Chapter ${index + 1}",
                            totalPages = total,
                        ))
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
