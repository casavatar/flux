package com.example.flux.feature.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import com.example.flux.data.bionic.BionicAnnotator
import com.example.flux.data.parser.DocumentParserFactory
import com.example.flux.domain.bionic.BionicEngine
import com.example.flux.domain.model.Book
import com.example.flux.domain.model.BookFormat
import com.example.flux.domain.model.NightMode
import com.example.flux.domain.model.Progress
import com.example.flux.domain.source.DocumentParser
import com.example.flux.domain.source.ParseResult
import com.example.flux.domain.model.UserPreferences
import com.example.flux.domain.usecase.DeleteBookUseCase
import com.example.flux.domain.usecase.GetBookByIdUseCase
import com.example.flux.domain.usecase.GetReadingProgressUseCase
import com.example.flux.domain.usecase.GetUserPreferencesUseCase
import com.example.flux.domain.usecase.PreferenceUpdate
import com.example.flux.domain.usecase.SaveReadingProgressUseCase
import com.example.flux.domain.usecase.SaveUserPreferencesUseCase
import com.example.flux.feature.reader.model.ReaderIntent
import com.example.flux.feature.reader.model.ReaderUiState
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Verifies debounced progress persistence, saved-page restoration, and slider debounce for
 * bionic/font-size preferences.
 *
 * Virtual time is controlled by [UnconfinedTestDispatcher] via [MainDispatcherRule]: coroutines
 * start eagerly so delays are registered immediately, making [advanceTimeBy] fire them
 * precisely. Passing [MainDispatcherRule.testDispatcher] to every [runTest] shares the single
 * [TestCoroutineScheduler] between the test body and the ViewModel's viewModelScope.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getBookById = mockk<GetBookByIdUseCase>()
    private val bionicEngine = mockk<BionicEngine>()
    private val bionicAnnotator = mockk<BionicAnnotator>()
    private val documentParserFactory = mockk<DocumentParserFactory>()
    private val getReadingProgress = mockk<GetReadingProgressUseCase>()
    private val saveReadingProgress = mockk<SaveReadingProgressUseCase>()
    private val deleteBook = mockk<DeleteBookUseCase>()
    private val getUserPreferences = mockk<GetUserPreferencesUseCase>()
    private val saveUserPreferences = mockk<SaveUserPreferencesUseCase>()
    private val fakePrefs = MutableStateFlow(UserPreferences())

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
        coJustRun { saveReadingProgress(any()) }
        coJustRun { saveUserPreferences(any()) }
        every { getUserPreferences() } returns fakePrefs
        every { bionicEngine.annotate(any(), any(), any(), any()) } returns emptyList()
        every { bionicAnnotator.annotate(any(), any(), any()) } returns AnnotatedString("")
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
        getUserPreferences = getUserPreferences,
        saveUserPreferences = saveUserPreferences,
        bionicEngine = bionicEngine,
        bionicAnnotator = bionicAnnotator,
    )

    // ── debounce (reading progress) ───────────────────────────────────────────

    @Test
    fun `rapid page flips result in zero saves before debounce window`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 100)
            val vm = createViewModel()
            advanceUntilIdle() // settle book load + page accumulation

            (1..20).forEach { i -> vm.onIntent(ReaderIntent.PageChanged(i)) }

            advanceTimeBy(ReaderViewModel.DEBOUNCE_MS - 1)
            coVerify(exactly = 0) { saveReadingProgress(any()) }
        }

    @Test
    fun `rapid page flips result in exactly one save after debounce window`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 100)
            val vm = createViewModel()
            advanceUntilIdle()

            (1..20).forEach { i -> vm.onIntent(ReaderIntent.PageChanged(i)) }

            advanceUntilIdle() // advance past debounce window
            coVerify(exactly = 1) { saveReadingProgress(any()) }
        }

    @Test
    fun `debounce persists the last page in a rapid flip storm`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 100)
            val vm = createViewModel()
            advanceUntilIdle()

            (1..20).forEach { i -> vm.onIntent(ReaderIntent.PageChanged(i)) }
            advanceUntilIdle() // advance past debounce window

            coVerify { saveReadingProgress(match { it.currentPage == 20 }) }
        }

    @Test
    fun `two page changes separated by debounce window each trigger a save`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 100)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onIntent(ReaderIntent.PageChanged(5))
            advanceUntilIdle() // fire first debounce window

            vm.onIntent(ReaderIntent.PageChanged(10))
            advanceUntilIdle() // fire second debounce window

            coVerify(exactly = 2) { saveReadingProgress(any()) }
        }

    @Test
    fun `page change within debounce window resets timer and delays save`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 100)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onIntent(ReaderIntent.PageChanged(3))
            advanceTimeBy(ReaderViewModel.DEBOUNCE_MS - 100) // not yet past window

            vm.onIntent(ReaderIntent.PageChanged(7)) // resets timer
            advanceTimeBy(ReaderViewModel.DEBOUNCE_MS - 1) // still not past new window
            coVerify(exactly = 0) { saveReadingProgress(any()) }

            advanceUntilIdle() // now past new window
            coVerify(exactly = 1) { saveReadingProgress(match { it.currentPage == 7 }) }
        }

    // ── progress restore ──────────────────────────────────────────────────────

    @Test
    fun `saved progress page is restored when book opens`() =
        runTest(mainDispatcherRule.testDispatcher) {
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
    fun `no saved progress opens book at page 0`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 100, savedProgress = null)
            val vm = createViewModel()
            advanceUntilIdle()

            val state = vm.uiState.value as ReaderUiState.Success
            assertEquals(0, state.currentPageIndex)
        }

    @Test
    fun `saved progress page beyond new total is clamped to last page`() =
        runTest(mainDispatcherRule.testDispatcher) {
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
    fun `ui state is Success with correct page count after book loads`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 50)
            val vm = createViewModel()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(state is ReaderUiState.Success)
            assertEquals(50, (state as ReaderUiState.Success).totalPages)
            assertEquals(50, state.pages.size)
        }

    @Test
    fun `book not found emits Error state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getBookById("book-1") } returns flowOf(null)
            val vm = createViewModel()
            advanceUntilIdle()

            assertTrue(vm.uiState.value is ReaderUiState.Error)
        }

    @Test
    fun `unsupported format emits Error state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getBookById("book-1") } returns flowOf(fakeBook)
            every { documentParserFactory.get(BookFormat.TXT) } throws NotImplementedError("unsupported")
            val vm = createViewModel()
            advanceUntilIdle()

            assertTrue(vm.uiState.value is ReaderUiState.Error)
        }

    @Test
    fun `page change updates currentPageIndex in ui state immediately`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 100)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onIntent(ReaderIntent.PageChanged(33))

            val state = vm.uiState.value as ReaderUiState.Success
            assertEquals(33, state.currentPageIndex)
        }

    @Test
    fun `toggle controls flips controlsVisible in ui state`() =
        runTest(mainDispatcherRule.testDispatcher) {
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
    fun `SecurityException from parser emits Error state with canDelete true`() =
        runTest(mainDispatcherRule.testDispatcher) {
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
    fun `FileNotFoundException from parser emits Error state with canDelete true`() =
        runTest(mainDispatcherRule.testDispatcher) {
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

    // ── font size preference ──────────────────────────────────────────────────

    @Test
    fun `font size from preferences is reflected in Success state on load`() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakePrefs.value = UserPreferences(defaultFontSizeSp = 20)
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            val state = vm.uiState.value as ReaderUiState.Success
            assertEquals(20, state.fontSizeSp)
        }

    @Test
    fun `changing defaultFontSizeSp updates fontSizeSp in Success state after debounce`() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakePrefs.value = UserPreferences(defaultFontSizeSp = 16)
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            fakePrefs.value = UserPreferences(defaultFontSizeSp = 24)
            advanceUntilIdle() // advance past the 300ms debounce window

            val state = vm.uiState.value as ReaderUiState.Success
            assertEquals(24, state.fontSizeSp)
        }

    @Test
    fun `font size change before debounce window does not update state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakePrefs.value = UserPreferences(defaultFontSizeSp = 16)
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            fakePrefs.value = UserPreferences(defaultFontSizeSp = 24)
            advanceTimeBy(ReaderViewModel.FONT_DEBOUNCE_MS - 1) // 299ms — debounce not yet fired

            val state = vm.uiState.value as ReaderUiState.Success
            assertEquals(16, state.fontSizeSp)
        }

    // ── navigation ────────────────────────────────────────────────────────────

    @Test
    fun `NavigateBack flushes in-flight progress save before navigation event`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 100)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onIntent(ReaderIntent.PageChanged(5))
            // pending save is scheduled but delay has not elapsed

            val events = mutableListOf<Unit>()
            val job = launch { vm.navigationEvents.collect { events.add(it) } }

            vm.onIntent(ReaderIntent.NavigateBack)
            advanceUntilIdle()

            coVerify(exactly = 1) { saveReadingProgress(match { it.currentPage == 5 }) }
            assertEquals(1, events.size)
            job.cancel()
        }

    @Test
    fun `NavigateBack with no pending save emits navigation event without save`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 100)
            val vm = createViewModel()
            advanceUntilIdle()

            val events = mutableListOf<Unit>()
            val job = launch { vm.navigationEvents.collect { events.add(it) } }

            vm.onIntent(ReaderIntent.NavigateBack)
            advanceUntilIdle()

            coVerify(exactly = 0) { saveReadingProgress(any()) }
            assertEquals(1, events.size)
            job.cancel()
        }

    @Test
    fun `DeleteBook intent calls DeleteBookUseCase and emits navigation event`() =
        runTest(mainDispatcherRule.testDispatcher) {
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

    // ── bionic intensity debounce ─────────────────────────────────────────────

    @Test
    fun `rapid intensity changes result in zero preference saves before slider debounce`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            listOf(0.3f, 0.4f, 0.5f, 0.6f, 0.7f).forEach {
                vm.onIntent(ReaderIntent.SetBionicIntensity(it))
            }
            advanceTimeBy(ReaderViewModel.SLIDER_DEBOUNCE_MS - 1)

            coVerify(exactly = 0) { saveUserPreferences(any()) }
        }

    @Test
    fun `rapid intensity changes result in exactly one preference save after slider debounce`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            listOf(0.3f, 0.4f, 0.5f, 0.6f, 0.7f).forEach {
                vm.onIntent(ReaderIntent.SetBionicIntensity(it))
            }
            advanceUntilIdle()

            coVerify(exactly = 1) { saveUserPreferences(any()) }
        }

    @Test
    fun `slider debounce persists the last intensity in a rapid storm`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            listOf(0.3f, 0.4f, 0.5f, 0.6f, 0.7f).forEach {
                vm.onIntent(ReaderIntent.SetBionicIntensity(it))
            }
            advanceUntilIdle()

            coVerify { saveUserPreferences(PreferenceUpdate.BionicIntensity(0.7f)) }
        }

    @Test
    fun `SetBionicIntensity updates bionicIntensity in Success state immediately`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onIntent(ReaderIntent.SetBionicIntensity(0.6f))

            val state = vm.uiState.value as ReaderUiState.Success
            assertEquals(0.6f, state.bionicIntensity, 0.001f)
        }

    // ── font size slider debounce ─────────────────────────────────────────────

    @Test
    fun `rapid SetFontSize changes result in zero saves before slider debounce`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            (14..20).forEach { sp -> vm.onIntent(ReaderIntent.SetFontSize(sp)) }
            advanceTimeBy(ReaderViewModel.SLIDER_DEBOUNCE_MS - 1)

            coVerify(exactly = 0) { saveUserPreferences(any()) }
        }

    @Test
    fun `rapid SetFontSize changes result in exactly one save after slider debounce`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            (14..20).forEach { sp -> vm.onIntent(ReaderIntent.SetFontSize(sp)) }
            advanceUntilIdle()

            coVerify(exactly = 1) { saveUserPreferences(any()) }
        }

    @Test
    fun `SetFontSize updates fontSizeSp in Success state immediately`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onIntent(ReaderIntent.SetFontSize(22))

            val state = vm.uiState.value as ReaderUiState.Success
            assertEquals(22, state.fontSizeSp)
        }

    // ── bionic enabled / night mode ───────────────────────────────────────────

    @Test
    fun `SetBionicEnabled false updates state immediately and saves without debounce`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onIntent(ReaderIntent.SetBionicEnabled(false))
            advanceUntilIdle()

            val state = vm.uiState.value as ReaderUiState.Success
            assertEquals(false, state.bionicEnabled)
            coVerify(exactly = 1) { saveUserPreferences(PreferenceUpdate.BionicEnabled(false)) }
        }

    @Test
    fun `SetNightMode updates nightMode in state and saves without debounce`() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onIntent(ReaderIntent.SetNightMode(NightMode.SEPIA))
            advanceUntilIdle()

            val state = vm.uiState.value as ReaderUiState.Success
            assertEquals(NightMode.SEPIA, state.nightMode)
            coVerify(exactly = 1) { saveUserPreferences(PreferenceUpdate.Theme(NightMode.SEPIA)) }
        }

    @Test
    fun `bionic preferences from UserPreferences are reflected in Success state on load`() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakePrefs.value = UserPreferences(bionicIntensity = 0.7f, bionicEnabled = false, nightMode = NightMode.DARK)
            setupBookLoad(pageCount = 5)
            val vm = createViewModel()
            advanceUntilIdle()

            val state = vm.uiState.value as ReaderUiState.Success
            assertEquals(0.7f, state.bionicIntensity, 0.001f)
            assertEquals(false, state.bionicEnabled)
            assertEquals(NightMode.DARK, state.nightMode)
        }
}
