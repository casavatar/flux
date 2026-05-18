package com.example.flux.data.txt

import app.cash.turbine.test
import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.model.UserPreferences
import com.example.flux.domain.repository.PreferencesRepository
import com.example.flux.domain.source.ParseResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class PlainTextParserTest {

    private lateinit var tmpFile: File
    private val prefsRepo = mockk<PreferencesRepository>()
    private val parser = PlainTextParser(prefsRepo)

    @Before
    fun setUp() {
        tmpFile = File.createTempFile("flux_test_", ".txt")
        every { prefsRepo.userPreferences } returns flowOf(UserPreferences(defaultFontSizeSp = 18))
    }

    @After
    fun tearDown() {
        tmpFile.delete()
    }

    // GIVEN an empty file
    // WHEN parse() is called
    // THEN one empty page is emitted
    @Test
    fun parse_emptyFile_emitsOneEmptyPage() = runTest {
        tmpFile.writeText("")
        parser.parse(aBook()).test {
            awaitItem() as ParseResult.Metadata
            val page = awaitItem() as ParseResult.Page
            assertEquals(0, page.index)
            assertEquals("", page.text)
            assertEquals(1, page.totalPages)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun parse_singleParagraph_emitsMetadataThenOnePage() = runTest {
        tmpFile.writeText("Hello world. This is a test.")
        parser.parse(aBook()).test {
            val meta = awaitItem() as ParseResult.Metadata
            assertEquals("Test Book", meta.title)
            val page = awaitItem() as ParseResult.Page
            assertEquals(0, page.index)
            assertTrue(page.text.contains("Hello world"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun parse_multipleParagraphs_groupsIntoPages() = runTest {
        val paragraphs = (1..10).joinToString("\n\n") { "Paragraph $it. " + "word ".repeat(50) }
        tmpFile.writeText(paragraphs)

        val pages = mutableListOf<ParseResult.Page>()
        parser.parse(aBook()).test {
            awaitItem() // metadata
            while (true) {
                val item = awaitItem()
                if (item is ParseResult.Page) pages += item else break
            }
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue("Expected multiple pages, got ${pages.size}", pages.size > 1)
        pages.forEachIndexed { i, p -> assertEquals(i, p.index) }
        assertEquals(pages.last().totalPages, pages.size)
    }

    // GIVEN a file with no paragraph breaks (wall of text)
    // WHEN parse() is called
    // THEN content is split into pages at estimated line count
    @Test
    fun parse_noParagraphBreaks_stillPaginates() = runTest {
        // ~3000 chars, no blank lines — should produce at least 2 pages at 18sp (~1500 chars/page)
        val text = "word ".repeat(600)
        tmpFile.writeText(text)

        val pages = collectPages()
        assertTrue("Expected ≥ 2 pages, got ${pages.size}", pages.size >= 2)
    }

    // GIVEN a single paragraph longer than one page
    // WHEN parse() is called
    // THEN it is split at sentence boundaries
    @Test
    fun parse_singleOversizedParagraph_splitsAtSentenceBoundaries() = runTest {
        // Build a paragraph with clear sentence boundaries, > 1500 chars
        val sentences = (1..40).joinToString(" ") { "This is sentence number $it." }
        tmpFile.writeText(sentences)

        val pages = collectPages()
        assertTrue("Expected > 1 pages, got ${pages.size}", pages.size > 1)
        // No page should exceed 2× the char limit (sentences shouldn't be glued together)
        pages.forEach { p ->
            assertTrue("Page ${p.index} exceeds char limit: ${p.text.length}", p.text.length <= 3_500)
        }
    }

    // GIVEN font size doubles from 18sp to 36sp
    // WHEN preferences change mid-stream
    // THEN parse() re-emits with fewer chars per page (more pages)
    @Test
    fun parse_fontSizeChange_repaginates() = runTest {
        val text = "word ".repeat(600) // ~3000 chars
        tmpFile.writeText(text)

        val prefsFlow = MutableStateFlow(UserPreferences(defaultFontSizeSp = 18))
        every { prefsRepo.userPreferences } returns prefsFlow

        parser.parse(aBook()).test {
            awaitItem() as ParseResult.Metadata
            val firstPage = awaitItem() as ParseResult.Page
            val pagesAt18sp = firstPage.totalPages

            // Change font size — triggers re-pagination
            prefsFlow.value = UserPreferences(defaultFontSizeSp = 36)

            // New metadata emitted for fresh pagination
            val newMeta = awaitItem()
            assertTrue(newMeta is ParseResult.Metadata)
            val newFirstPage = awaitItem() as ParseResult.Page
            val pagesAt36sp = newFirstPage.totalPages

            // Larger font → fewer chars per page → more total pages
            assertTrue(
                "Expected more pages at 36sp ($pagesAt36sp) than at 18sp ($pagesAt18sp)",
                pagesAt36sp > pagesAt18sp,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Definition of Done: 1 MB TXT file paginated in < 300 ms on JVM
    @Test
    fun parse_1mbFile_paginatesWithin300ms() = runTest {
        val content = "word ".repeat(200_000) // ~1 MB
        tmpFile.writeText(content)

        val elapsed = kotlin.system.measureTimeMillis {
            collectPages()
        }
        assertTrue("Pagination took ${elapsed}ms, expected < 300ms", elapsed < 300L)
    }

    // --- helpers ---

    private fun aBook() = Book(
        id = "test-id",
        title = "Test Book",
        author = "Test Author",
        coverUri = null,
        fileUri = tmpFile.toURI().toString(),
        format = BookFormat.TXT,
        addedAt = 0L,
    )

    private suspend fun collectPages(): List<ParseResult.Page> {
        val pages = mutableListOf<ParseResult.Page>()
        parser.parse(aBook()).test {
            awaitItem() // metadata
            var keepGoing = true
            while (keepGoing) {
                val item = awaitItem()
                when (item) {
                    is ParseResult.Page -> {
                        pages += item
                        if (item.index == item.totalPages - 1) keepGoing = false
                    }
                    is ParseResult.Error -> keepGoing = false
                    else -> {}
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
        return pages
    }
}
