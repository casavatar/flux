package com.example.flux.data.parser

import com.example.flux.data.txt.PlainTextParser
import com.example.flux.domain.model.BookFormat
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentParserFactoryTest {

    private val plainTextParser = mockk<PlainTextParser>()
    private val factory = DocumentParserFactory(plainTextParser)

    @Test
    fun get_txt_returnsPlainTextParser() {
        assertSame(plainTextParser, factory.get(BookFormat.TXT))
    }

    @Test
    fun get_epub_throwsNotImplementedError() {
        val ex = runCatching { factory.get(BookFormat.EPUB) }.exceptionOrNull()
        assertTrue("Expected NotImplementedError, got $ex", ex is NotImplementedError)
    }

    @Test
    fun get_pdf_throwsNotImplementedError() {
        val ex = runCatching { factory.get(BookFormat.PDF) }.exceptionOrNull()
        assertTrue("Expected NotImplementedError, got $ex", ex is NotImplementedError)
    }
}
