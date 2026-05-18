package com.example.flux.data.epub

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.flux.domain.model.BookParseResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.streamer.Readium

@RunWith(AndroidJUnit4::class)
class ReadiumEpubParserTest {

    private lateinit var context: Context
    private lateinit var parser: ReadiumEpubParser

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        parser = ReadiumEpubParser(Readium(context))
    }

    // GIVEN a valid EPUB with 50+ chapters
    // WHEN parse() is called
    // THEN a Success result is returned with all chapters mapped
    @Test
    fun parse_validEpubWith50PlusChapters_returnsSuccessWithAllChapters() = runTest {
        val file = EpubTestFixture.createWithChapters(context, chapterCount = 52)
        val result = parser.parse(bookId = "test-52ch", fileUri = file.toURI().toString())

        assertTrue("Expected Success but was: $result", result is BookParseResult.Success)
        val success = result as BookParseResult.Success
        assertEquals("test-52ch", success.book.id)
        assertEquals("Test EPUB — 52 Chapters", success.book.title)
        assertEquals("Flux Test Fixture", success.book.author)
        assertEquals(52, success.chapters.size)
        assertTrue(success.readingOrder.isNotEmpty())
    }

    @Test
    fun parse_chaptersHaveCorrectPositionsAndNonEmptyHrefs() = runTest {
        val file = EpubTestFixture.createWithChapters(context, chapterCount = 5)
        val result = parser.parse(bookId = "test-5ch", fileUri = file.toURI().toString()) as BookParseResult.Success

        result.chapters.forEachIndexed { index, chapter ->
            assertEquals(index, chapter.position)
            assertTrue("href must not be empty", chapter.href.isNotEmpty())
            assertNotNull(chapter.title)
        }
    }

    @Test
    fun parse_readingOrderItemsHaveNonEmptyHrefsAndMediaTypes() = runTest {
        val file = EpubTestFixture.createWithChapters(context, chapterCount = 3)
        val result = parser.parse(bookId = "test-ro", fileUri = file.toURI().toString()) as BookParseResult.Success

        assertTrue(result.readingOrder.isNotEmpty())
        result.readingOrder.forEach { item ->
            assertTrue(item.href.isNotEmpty())
            assertTrue(item.mediaType.isNotEmpty())
        }
    }

    // GIVEN a corrupted (non-EPUB) file
    // WHEN parse() is called
    // THEN a CorruptFile error is returned without a crash
    @Test
    fun parse_corruptedFile_returnsCorruptFileError() = runTest {
        val file = EpubTestFixture.createCorrupted(context)
        val result = parser.parse(bookId = "test-corrupt", fileUri = file.toURI().toString())

        assertTrue("Expected CorruptFile error but was: $result", result is BookParseResult.Error.CorruptFile)
    }

    @Test
    fun parse_invalidUri_returnsUnknownError() = runTest {
        val result = parser.parse(bookId = "test-bad-uri", fileUri = "not a url")

        assertTrue("Expected Unknown error but was: $result", result is BookParseResult.Error.Unknown)
    }

    // GIVEN a DRM-protected EPUB
    // WHEN parse() is called
    // THEN a DrmProtected error is returned without a crash
    //
    // To run this test: place a real LCP-protected .epub in
    // core/data/src/androidTest/assets/drm_protected.epub
    @Test
    fun parse_drmProtectedEpub_returnsDrmProtectedError() = runTest {
        val stream = runCatching { context.assets.open("drm_protected.epub") }.getOrNull()
            ?: return@runTest // asset not present — skip

        val file = java.io.File(context.cacheDir, "drm_protected.epub")
        stream.use { input -> file.outputStream().use(input::copyTo) }

        val result = parser.parse(bookId = "test-drm", fileUri = file.toURI().toString())
        assertTrue("Expected DrmProtected but was: $result", result is BookParseResult.Error.DrmProtected)
    }
}
