package com.example.flux.data.parser

import com.example.flux.data.epub.EpubDocumentParser
import com.example.flux.data.txt.PlainTextParser
import com.example.flux.domain.model.BookFormat
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentParserFactoryTest {

    private val plainTextParser = mockk<PlainTextParser>()
    private val epubDocumentParser = mockk<EpubDocumentParser>()
    private val factory = DocumentParserFactory(plainTextParser, epubDocumentParser)

    @Test
    fun get_txt_returnsPlainTextParser() {
        assertSame(plainTextParser, factory.get(BookFormat.TXT))
    }

    @Test
    fun get_epub_returnsEpubDocumentParser() {
        assertSame(epubDocumentParser, factory.get(BookFormat.EPUB))
    }

    @Test
    fun get_pdf_throwsNotImplementedError() {
        val ex = runCatching { factory.get(BookFormat.PDF) }.exceptionOrNull()
        assertTrue("Expected NotImplementedError, got $ex", ex is NotImplementedError)
    }
}
