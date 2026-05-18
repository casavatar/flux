package com.example.flux.data.parser

import com.example.flux.data.txt.PlainTextParser
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.source.DocumentParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentParserFactory @Inject constructor(
    private val plainTextParser: PlainTextParser,
) {
    fun get(format: BookFormat): DocumentParser = when (format) {
        BookFormat.TXT -> plainTextParser
        // GEE-87: wire EpubDocumentParser (Readium navigator-backed) once implemented
        BookFormat.EPUB -> throw NotImplementedError("EpubDocumentParser not yet implemented (GEE-87)")
        // GEE-84: wire PdfDocumentParser once implemented
        BookFormat.PDF -> throw NotImplementedError("PdfDocumentParser not yet implemented (GEE-84)")
    }
}
