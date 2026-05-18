package com.example.flux.data.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.flux.data.local.BookDao
import com.example.flux.data.local.BookEntity
import com.example.flux.domain.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfDocumentParserTest {

    private lateinit var context: Context
    private lateinit var parser: PdfDocumentParser
    private lateinit var testPdfFile: File

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Wipe residual cached pages so init block starts from a clean state.
        File(context.cacheDir, "pdf_pages").deleteRecursively()

        testPdfFile = generateTestPdf(context)

        parser = PdfDocumentParser(
            context = context,
            contentResolver = context.contentResolver,
            bookDao = fakeBookDao(Uri.fromFile(testPdfFile).toString()),
            cache = PdfPageCache(context),
            ioDispatcher = Dispatchers.IO,
        )
    }

    @Test
    fun renderPage_returnsNonNullBitmap() = runTest {
        val bitmap = parser.renderPage("test-book", pageIndex = 0)
        assertNotNull(bitmap)
    }

    @Test
    fun renderPage_cacheHitCompletesUnder150ms() = runTest {
        // Cold render populates the disk cache.
        parser.renderPage("test-book", pageIndex = 0)

        // Second call goes through the cache; measure this path.
        val start = SystemClock.elapsedRealtime()
        parser.renderPage("test-book", pageIndex = 0)
        val elapsed = SystemClock.elapsedRealtime() - start

        assertTrue(
            "Cache-hit render took ${elapsed}ms — expected < 150ms",
            elapsed < 150L,
        )
    }

    @Test
    fun getPageCount_returnsAtLeastOnePage() = runTest {
        val count = parser.getPageCount("test-book")
        assertTrue("Expected ≥ 1 page, got $count", count >= 1)
    }

    @Test
    fun diskCache_doesNotExceed50MB_afterRepeatedRenders() = runTest {
        repeat(10) { parser.renderPage("test-book", pageIndex = 0) }

        // A fresh PdfPageCache instance rebuilds its index from disk.
        val diskCache = PdfPageCache(context)
        assertTrue(
            "Disk cache ${diskCache.currentSizeBytes()} bytes exceeded 50 MB",
            diskCache.currentSizeBytes() <= 50L * 1024 * 1024,
        )
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Generates a single-page A4 PDF using the platform [PdfDocument] API. */
    private fun generateTestPdf(context: Context): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        page.canvas.drawColor(Color.WHITE)
        page.canvas.drawText(
            "Test page",
            100f, 100f,
            Paint().apply { color = Color.BLACK; textSize = 24f },
        )
        pdfDoc.finishPage(page)

        val file = File(context.cacheDir, "parser_test.pdf")
        file.outputStream().use { pdfDoc.writeTo(it) }
        pdfDoc.close()
        return file
    }

    private fun fakeBookDao(fileUri: String): BookDao = object : BookDao {
        private val entity = BookEntity(
            id = "test-book",
            title = "Test Book",
            author = null,
            coverUri = null,
            fileUri = fileUri,
            format = BookFormat.PDF,
            addedAt = 0L,
        )

        override fun getAllBooks(): Flow<List<BookEntity>> = flowOf(listOf(entity))
        override fun getBookById(bookId: String): Flow<BookEntity?> = flowOf(entity)
        override suspend fun getBookByIdOnce(bookId: String): BookEntity = entity
        override suspend fun insert(book: BookEntity) = Unit
        override suspend fun delete(bookId: String) = Unit
    }
}
