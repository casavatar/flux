package com.example.flux.data.txt

import com.example.flux.domain.model.Book
import com.example.flux.domain.repository.PreferencesRepository
import com.example.flux.domain.source.DocumentParser
import com.example.flux.domain.source.ParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlainTextParser @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : DocumentParser {

    // Re-paginates automatically whenever defaultFontSizeSp changes.
    override suspend fun parse(book: Book): Flow<ParseResult> =
        preferencesRepository.userPreferences
            .flatMapLatest { prefs -> paginateFile(book, prefs.defaultFontSizeSp) }
            .flowOn(Dispatchers.IO)

    private fun paginateFile(book: Book, fontSizeSp: Int): Flow<ParseResult> = flow {
        emit(ParseResult.Metadata(title = book.title, author = book.author, coverUri = null))

        val file = runCatching { File(URI(book.fileUri)) }.getOrElse {
            emit(ParseResult.Error(it))
            return@flow
        }

        if (!file.exists()) {
            emit(ParseResult.Error(FileNotFoundException(book.fileUri)))
            return@flow
        }

        if (file.length() == 0L) {
            emit(ParseResult.Page(index = 0, text = "", totalPages = 1))
            return@flow
        }

        val paragraphs = readParagraphs(file)
        val pages = paginateParagraphs(paragraphs, charsPerPage(fontSizeSp))
        pages.forEachIndexed { i, text ->
            emit(ParseResult.Page(index = i, text = text, totalPages = pages.size))
        }
    }

    private fun readParagraphs(file: File): List<String> {
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()
        file.bufferedReader().use { reader ->
            for (line in reader.lineSequence()) {
                if (line.isBlank()) {
                    if (current.isNotEmpty()) {
                        paragraphs += current.toString().trim()
                        current.clear()
                    }
                } else {
                    if (current.isNotEmpty()) current.append(' ')
                    current.append(line.trim())
                }
            }
            if (current.isNotEmpty()) paragraphs += current.toString().trim()
        }
        return paragraphs.ifEmpty { listOf("") }
    }

    private fun paginateParagraphs(paragraphs: List<String>, charsPerPage: Int): List<String> {
        val pages = mutableListOf<String>()
        val current = StringBuilder()

        for (paragraph in paragraphs) {
            when {
                // Paragraph exceeds full page — split at sentence boundaries
                paragraph.length > charsPerPage -> {
                    if (current.isNotEmpty()) { pages += current.toString().trimEnd(); current.clear() }
                    pages += splitAtSentences(paragraph, charsPerPage)
                }
                // Paragraph doesn't fit on current page — flush first
                current.length + paragraph.length + 2 > charsPerPage -> {
                    pages += current.toString().trimEnd()
                    current.clear()
                    current.append(paragraph)
                }
                else -> {
                    if (current.isNotEmpty()) current.append("\n\n")
                    current.append(paragraph)
                }
            }
        }

        if (current.isNotEmpty()) pages += current.toString().trimEnd()
        return pages.ifEmpty { listOf("") }
    }

    private fun splitAtSentences(text: String, charsPerPage: Int): List<String> {
        val pages = mutableListOf<String>()
        val current = StringBuilder()

        for (sentence in SENTENCE_BOUNDARY.split(text)) {
            val s = sentence.trim()
            if (s.isEmpty()) continue
            if (current.isNotEmpty() && current.length + s.length + 1 > charsPerPage) {
                pages += current.toString().trimEnd()
                current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(s)
        }

        if (current.isNotEmpty()) pages += current.toString().trimEnd()
        return pages.ifEmpty { listOf(text) }
    }

    // Chars per page scales inversely with font size; calibrated at 18sp on a typical phone.
    private fun charsPerPage(fontSizeSp: Int): Int =
        (BASE_CHARS_PER_PAGE * BASE_FONT_SP / fontSizeSp.coerceAtLeast(MIN_FONT_SP))
            .coerceAtLeast(MIN_CHARS_PER_PAGE)

    companion object {
        private val SENTENCE_BOUNDARY = Regex("(?<=[.!?])\\s+")
        private const val BASE_CHARS_PER_PAGE = 1_500
        private const val BASE_FONT_SP = 18
        private const val MIN_FONT_SP = 8
        private const val MIN_CHARS_PER_PAGE = 200
    }
}
