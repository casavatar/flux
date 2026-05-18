package com.example.flux.data.parser

import com.example.flux.data.epub.EpubDocumentParser
import com.example.flux.data.txt.PlainTextParser
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.source.DocumentParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentParserFactory @Inject constructor(
    private val plainTextParser: PlainTextParser,
    private val epubDocumentParser: EpubDocumentParser,
) {
    fun get(format: BookFormat): DocumentParser = when (format) {
        BookFormat.TXT -> plainTextParser
        BookFormat.EPUB -> epubDocumentParser
        // GEE-84: wire PdfDocumentParser once implemented
        BookFormat.PDF -> throw NotImplementedError("PdfDocumentParser not yet implemented (GEE-84)")
    }
}
