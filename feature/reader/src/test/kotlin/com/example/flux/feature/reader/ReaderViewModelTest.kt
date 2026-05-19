package com.example.flux.feature.reader

import androidx.lifecycle.SavedStateHandle
import com.example.flux.data.parser.DocumentParserFactory
import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.model.Progress
import com.example.flux.domain.source.DocumentParser
import com.example.flux.domain.source.ParseResult
import com.example.flux.domain.usecase.DeleteBookUseCase
import com.example.flux.domain.usecase.GetBookByIdUseCase
import com.example.flux.domain.usecase.GetReadingProgressUseCase
import com.example.flux.domain.usecase.SaveReadingProgressUseCase
import com.example.flux.feature.reader.model.ReaderIntent
import com.example.flux.feature.reader.model.ReaderUiState
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies debounced progress persistence and saved-page restoration.
 *
 * Virtual time is controlled by [StandardTestDispatcher]: rapid page-flip events are
 * emitted at T=0, then [advanceTimeBy] drives the scheduler past the debounce window to
 * confirm exactly one DB write occurs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getBookById = mockk<GetBookByIdUseCase>()
    private val documentParserFactory = mockk<DocumentParserFactory>()
    private val getReadingProgress = mockk<GetReadingProgressUseCase>()
    private val saveReadingProgress = mockk<SaveReadingProgressUseCase>()
    private val deleteBook = mockk<DeleteBookUseCase>()

    private val fakeBook = Book(
        id = "book-1",
        title = "Test Book",
        author = "Author",
        coverUri = null,
        fileUri = "file:///test.txt",
        format = BookFormat.TXT,
        addedAt = 0L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coJustRun { saveReadingProgress(any()) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── test helpers ──────────────────────────────────────────────────────────

    private fun fakeParser(pageCount: Int): DocumentParser {
        val parser = mockk<DocumentParser>()
        coEvery { parser.parse(any()) } returns flow {
            repeat(pageCount) { i -> emit(ParseResult.Page(i, "page $i", pageCount)) }
            awaitCancellation()
        }
        return parser
    }

    private fun setupBookLoad(pageCount: Int = 100, savedProgress: Progress? = null) {
        every { getBookById("book-1") } returns flowOf(fakeBook)
        every { documentParserFactory.get(BookFormat.TXT) } returns fakeParser(pageCount)
        coEvery { getReadingProgress("book-1") } returns savedProgress
    }

    private fun createViewModel(bookId: String = "book-1") = ReaderViewModel(
        savedStateHandle = SavedStateHandle(mapOf("bookId" to bookId)),
        getBookById = getBookById,
        documentParserFactory = documentParserFactory,
        getReadingProgress = getReadingProgress,
        saveReadingProgress = saveReadingProgress,
        deleteBook = deleteBook,
    )

    // ── debounce ──────────────────────────────────────────────────────────────

    @Test
    fun `rapid page flips result in zero saves before debounce window`() = runTest {
        setupBookLoad(pageCount = 100)
        val vm = createViewModel()
        advanceUntilIdle() // settle book load + page accumulation

        (1..20).forEach { i -> vm.onIntent(ReaderIntent.PageChanged(i)) }

        advanceTimeBy(ReaderViewModel.DEBOUNCE_MS - 1)
        coVerify(exactly = 0) { saveReadingProgress(any()) }
    }

    @Test
    fun `rapid page flips result in exactly one save after debounce window`() = runTest {
        setupBookLoad(pageCount = 100)
        val vm = createViewModel()
        advanceUntilIdle()

        (1..20).forEach { i -> vm.onIntent(ReaderIntent.PageChanged(i)) }

        advanceTimeBy(ReaderViewModel.DEBOUNCE_MS)
        coVerify(exactly = 1) { saveReadingProgress(any()) }
    }

    @Test
    fun `debounce persists the last page in a rapid flip storm`() = runTest {
        setupBookLoad(pageCount = 100)
        val vm = createViewModel()
        advanceUntilIdle()

        (1..20).forEach { i -> vm.onIntent(ReaderIntent.PageChanged(i)) }
        advanceTimeBy(ReaderViewModel.DEBOUNCE_MS)

        coVerify { saveReadingProgress(match { it.currentPage == 20 }) }
    }

    @Test
    fun `two page changes separated by debounce window each trigger a save`() = runTest {
        setupBookLoad(pageCount = 100)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onIntent(ReaderIntent.PageChanged(5))
        advanceTimeBy(ReaderViewModel.DEBOUNCE_MS)

        vm.onIntent(ReaderIntent.PageChanged(10))
        advanceTimeBy(ReaderViewModel.DEBOUNCE_MS)

        coVerify(exactly = 2) { saveReadingProgress(any()) }
    }

    @Test
    fun `page change within debounce window resets timer and delays save`() = runTest {
        setupBookLoad(pageCount = 100)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onIntent(ReaderIntent.PageChanged(3))
        advanceTimeBy(ReaderViewModel.DEBOUNCE_MS - 100) // not yet past window

        vm.onIntent(ReaderIntent.PageChanged(7)) // resets timer
        advanceTimeBy(ReaderViewModel.DEBOUNCE_MS - 1) // still not past new window
        coVerify(exactly = 0) { saveReadingProgress(any()) }

        advanceTimeBy(1) // now past new window
        coVerify(exactly = 1) { saveReadingProgress(match { it.currentPage == 7 }) }
    }

    // ── progress restore ──────────────────────────────────────────────────────

    @Test
    fun `saved progress page is restored when book opens`() = runTest {
        setupBookLoad(
            pageCount = 100,
            savedProgress = Progress("book-1", currentPage = 47, totalPages = 100, lastReadAt = 0L),
        )
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as ReaderUiState.Success
        assertEquals(47, state.currentPageIndex)
    }

    @Test
    fun `no saved progress opens book at page 0`() = runTest {
        setupBookLoad(pageCount = 100, savedProgress = null)
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as ReaderUiState.Success
        assertEquals(0, state.currentPageIndex)
    }

    @Test
    fun `saved progress page beyond new total is clamped to last page`() = runTest {
        // Font grew — only 10 pages now, but progress was saved at page 50 with 100 pages
        setupBookLoad(
            pageCount = 10,
            savedProgress = Progress("book-1", currentPage = 50, totalPages = 100, lastReadAt = 0L),
        )
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as ReaderUiState.Success
        assertEquals(9, state.currentPageIndex)
    }

    // ── ui state correctness ──────────────────────────────────────────────────

    @Test
    fun `ui state is Success with correct page count after book loads`() = runTest {
        setupBookLoad(pageCount = 50)
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is ReaderUiState.Success)
        assertEquals(50, (state as ReaderUiState.Success).totalPages)
        assertEquals(50, state.pages.size)
    }

    @Test
    fun `book not found emits Error state`() = runTest {
        every { getBookById("book-1") } returns flowOf(null)
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is ReaderUiState.Error)
    }

    @Test
    fun `unsupported format emits Error state`() = runTest {
        every { getBookById("book-1") } returns flowOf(fakeBook)
        every { documentParserFactory.get(BookFormat.TXT) } throws NotImplementedError("unsupported")
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is ReaderUiState.Error)
    }

    @Test
    fun `page change updates currentPageIndex in ui state immediately`() = runTest {
        setupBookLoad(pageCount = 100)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onIntent(ReaderIntent.PageChanged(33))
        advanceUntilIdle()

        val state = vm.uiState.value as ReaderUiState.Success
        assertEquals(33, state.currentPageIndex)
    }

    @Test
    fun `toggle controls flips controlsVisible in ui state`() = runTest {
        setupBookLoad(pageCount = 100)
        val vm = createViewModel()
        advanceUntilIdle()

        val before = (vm.uiState.value as ReaderUiState.Success).controlsVisible
        vm.onIntent(ReaderIntent.ToggleControls)
        val after = (vm.uiState.value as ReaderUiState.Success).controlsVisible

        assertEquals(!before, after)
    }

    // ── error recovery ────────────────────────────────────────────────────────

    @Test
    fun `SecurityException from parser emits Error state with canDelete true`() = runTest {
        val parser = mockk<DocumentParser>()
        coEvery { parser.parse(any()) } returns flow {
            emit(ParseResult.Error(SecurityException("Permission denied")))
        }
        every { getBookById("book-1") } returns flowOf(fakeBook)
        every { documentParserFactory.get(BookFormat.TXT) } returns parser
        coEvery { getReadingProgress(any()) } returns null

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("Expected Error, got $state", state is ReaderUiState.Error)
        assertTrue("Expected canDelete=true", (state as ReaderUiState.Error).canDelete)
    }

    @Test
    fun `FileNotFoundException from parser emits Error state with canDelete true`() = runTest {
        val parser = mockk<DocumentParser>()
        coEvery { parser.parse(any()) } returns flow {
            emit(ParseResult.Error(java.io.FileNotFoundException("file gone")))
        }
        every { getBookById("book-1") } returns flowOf(fakeBook)
        every { documentParserFactory.get(BookFormat.TXT) } returns parser
        coEvery { getReadingProgress(any()) } returns null

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as ReaderUiState.Error
        assertTrue(state.canDelete)
    }

    @Test
    fun `DeleteBook intent calls DeleteBookUseCase and emits navigation event`() = runTest {
        setupBookLoad(pageCount = 5)
        coJustRun { deleteBook(any()) }

        val vm = createViewModel()
        advanceUntilIdle()

        val events = mutableListOf<Unit>()
        val job = launch { vm.navigationEvents.collect { events.add(it) } }

        vm.onIntent(ReaderIntent.DeleteBook)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteBook("book-1") }
        assertEquals(1, events.size)
        job.cancel()
    }
}
