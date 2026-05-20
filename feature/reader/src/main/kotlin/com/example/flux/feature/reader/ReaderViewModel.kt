package com.example.flux.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.data.bionic.BionicAnnotator
import com.example.flux.data.parser.DocumentParserFactory
import com.example.flux.domain.bionic.BionicEngine
import com.example.flux.domain.model.NightMode
import com.example.flux.domain.model.Progress
import com.example.flux.domain.source.ParseResult
import com.example.flux.domain.usecase.DeleteBookUseCase
import com.example.flux.domain.usecase.GetBookByIdUseCase
import com.example.flux.domain.usecase.GetReadingProgressUseCase
import com.example.flux.domain.usecase.GetUserPreferencesUseCase
import com.example.flux.domain.usecase.PreferenceUpdate
import com.example.flux.domain.usecase.SaveReadingProgressUseCase
import com.example.flux.domain.usecase.SaveUserPreferencesUseCase
import com.example.flux.feature.reader.model.PageCacheKey
import com.example.flux.feature.reader.model.ReaderIntent
import com.example.flux.feature.reader.model.ReaderPage
import com.example.flux.feature.reader.model.ReaderUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getBookById: GetBookByIdUseCase,
    private val documentParserFactory: DocumentParserFactory,
    private val getReadingProgress: GetReadingProgressUseCase,
    private val saveReadingProgress: SaveReadingProgressUseCase,
    private val deleteBook: DeleteBookUseCase,
    private val getUserPreferences: GetUserPreferencesUseCase,
    private val saveUserPreferences: SaveUserPreferencesUseCase,
    bionicEngine: BionicEngine,
    bionicAnnotator: BionicAnnotator,
) : ViewModel() {

    private val bionicCache = BionicPageCache(bionicEngine, bionicAnnotator)

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<Unit> = _navigationEvents.asSharedFlow()

    private var pendingSaveJob: Job? = null
    private var latestPendingProgress: Progress? = null
    private var preWarmJob: Job? = null
    private var pendingIntensityJob: Job? = null
    private var pendingFontSizeJob: Job? = null

    private var currentFontSizeSp: Int = ReaderUiState.DEFAULT_FONT_SIZE_SP
    private var currentBionicIntensity: Float = BionicEngine.DEFAULT_INTENSITY
    private var currentBionicEnabled: Boolean = true
    private var currentNightMode: NightMode = NightMode.SYSTEM

    init {
        loadBook()
        collectUserPreferences()
    }

    private fun collectUserPreferences() {
        viewModelScope.launch {
            getUserPreferences()
                .map { it.defaultFontSizeSp }
                .distinctUntilChanged()
                .debounce(FONT_DEBOUNCE_MS)
                .collect { fontSizeSp ->
                    if (currentFontSizeSp == fontSizeSp) return@collect
                    currentFontSizeSp = fontSizeSp
                    bionicCache.evictAll()
                    val current = _uiState.value as? ReaderUiState.Success ?: return@collect
                    _uiState.value = current.copy(fontSizeSp = fontSizeSp)
                    warmCacheAround(current.pages, current.currentPageIndex)
                }
        }
        viewModelScope.launch {
            getUserPreferences()
                .map { prefs -> Triple(prefs.bionicIntensity, prefs.bionicEnabled, prefs.nightMode) }
                .distinctUntilChanged()
                .collect { (intensity, enabled, mode) ->
                    val intensityChanged = currentBionicIntensity != intensity
                    currentBionicIntensity = intensity
                    currentBionicEnabled = enabled
                    currentNightMode = mode
                    if (intensityChanged) bionicCache.evictAll()
                    val current = _uiState.value as? ReaderUiState.Success ?: return@collect
                    _uiState.value = current.copy(
                        bionicIntensity = intensity,
                        bionicEnabled = enabled,
                        nightMode = mode,
                    )
                    if (intensityChanged) warmCacheAround(current.pages, current.currentPageIndex)
                }
        }
    }

    private fun loadBook() {
        viewModelScope.launch {
            getBookById(bookId).collectLatest { book ->
                if (book == null) {
                    _uiState.value = ReaderUiState.Error("Book not found")
                    return@collectLatest
                }
                val parser = try {
                    documentParserFactory.get(book.format)
                } catch (_: NotImplementedError) {
                    _uiState.value = ReaderUiState.Error("Format '${book.format}' is not yet supported")
                    return@collectLatest
                }

                var initialProgress: Progress? = null
                val buffer = mutableListOf<ReaderPage>()

                try {
                    parser.parse(book).collect { result ->
                        when (result) {
                            is ParseResult.Metadata -> Unit
                            is ParseResult.Page -> {
                                if (result.index == 0) {
                                    buffer.clear()
                                    if (initialProgress == null) {
                                        initialProgress = getReadingProgress(bookId)
                                    }
                                }
                                buffer.add(ReaderPage(result.index, result.text))
                                if (buffer.size == result.totalPages) {
                                    val previous = _uiState.value as? ReaderUiState.Success
                                    val startPage = initialProgress?.currentPage
                                        ?.coerceIn(0, result.totalPages - 1) ?: 0
                                    _uiState.value = ReaderUiState.Success(
                                        book = book,
                                        pages = buffer.toList(),
                                        currentPageIndex = previous?.currentPageIndex
                                            ?.coerceIn(0, result.totalPages - 1) ?: startPage,
                                        totalPages = result.totalPages,
                                        fontSizeSp = currentFontSizeSp,
                                        controlsVisible = previous?.controlsVisible ?: false,
                                        bionicIntensity = currentBionicIntensity,
                                        bionicEnabled = currentBionicEnabled,
                                        nightMode = currentNightMode,
                                    )
                                }
                            }
                            is ParseResult.Error -> _uiState.value = mapError(result.cause)
                        }
                    }
                } catch (e: SecurityException) {
                    _uiState.value = mapError(e)
                } catch (e: FileNotFoundException) {
                    _uiState.value = mapError(e)
                } catch (e: IOException) {
                    _uiState.value = mapError(e)
                } catch (e: OutOfMemoryError) {
                    _uiState.value = mapError(e)
                }
            }
        }
    }

    private fun mapError(cause: Throwable): ReaderUiState.Error = when (cause) {
        is SecurityException -> ReaderUiState.Error(
            message = "The app no longer has permission to access this file.",
            canDelete = true,
        )
        is FileNotFoundException -> ReaderUiState.Error(
            message = "The file may have been moved or deleted.",
            canDelete = true,
        )
        is IOException -> ReaderUiState.Error(
            message = "Failed to read the file.",
            canDelete = true,
        )
        is OutOfMemoryError -> ReaderUiState.Error(
            message = "Not enough memory to open this book.",
            canDelete = false,
        )
        else -> ReaderUiState.Error(
            message = cause.message ?: "Failed to parse document",
            canDelete = false,
        )
    }

    fun onIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.PageChanged -> onPageChanged(intent.pageIndex)
            ReaderIntent.ToggleControls -> toggleControls()
            ReaderIntent.DeleteBook -> onDeleteBook()
            ReaderIntent.NavigateBack -> onNavigateBack()
            is ReaderIntent.SetBionicIntensity -> onSetBionicIntensity(intent.intensity)
            is ReaderIntent.SetFontSize -> onSetFontSize(intent.fontSizeSp)
            is ReaderIntent.SetBionicEnabled -> onSetBionicEnabled(intent.enabled)
            is ReaderIntent.SetNightMode -> onSetNightMode(intent.nightMode)
        }
    }

    /**
     * Returns a bionic-annotated [AnnotatedString] for [pageIndex], computing and
     * caching it on first access. Returns null if bionic is disabled, the reader is not
     * in the Success state, or the page index is out of range.
     */
    fun getAnnotatedPage(pageIndex: Int): androidx.compose.ui.text.AnnotatedString? {
        if (!currentBionicEnabled) return null
        val state = _uiState.value as? ReaderUiState.Success ?: return null
        val page = state.pages.getOrNull(pageIndex) ?: return null
        val key = PageCacheKey(bookId, page.index, currentBionicIntensity, currentFontSizeSp)
        return bionicCache.getOrPut(key, page.text)
    }

    private fun warmCacheAround(pages: List<ReaderPage>, centerIndex: Int) {
        preWarmJob?.cancel()
        val intensity = currentBionicIntensity
        val fontSizeSp = currentFontSizeSp
        preWarmJob = viewModelScope.launch(Dispatchers.Default) {
            for (offset in -PRE_WARM_WINDOW..PRE_WARM_WINDOW) {
                val page = pages.getOrNull(centerIndex + offset) ?: continue
                val key = PageCacheKey(bookId, page.index, intensity, fontSizeSp)
                bionicCache.getOrPut(key, page.text)
            }
        }
    }

    private fun onSetBionicIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(MIN_BIONIC_INTENSITY, MAX_BIONIC_INTENSITY)
        currentBionicIntensity = clamped
        bionicCache.evictAll()
        val current = _uiState.value as? ReaderUiState.Success
        if (current != null) {
            _uiState.value = current.copy(bionicIntensity = clamped)
            warmCacheAround(current.pages, current.currentPageIndex)
        }
        pendingIntensityJob?.cancel()
        pendingIntensityJob = viewModelScope.launch {
            delay(SLIDER_DEBOUNCE_MS)
            saveUserPreferences(PreferenceUpdate.BionicIntensity(clamped))
        }
    }

    private fun onSetFontSize(fontSizeSp: Int) {
        val clamped = fontSizeSp.coerceIn(14, 28)
        currentFontSizeSp = clamped
        bionicCache.evictAll()
        val current = _uiState.value as? ReaderUiState.Success
        if (current != null) {
            _uiState.value = current.copy(fontSizeSp = clamped)
            warmCacheAround(current.pages, current.currentPageIndex)
        }
        pendingFontSizeJob?.cancel()
        pendingFontSizeJob = viewModelScope.launch {
            delay(SLIDER_DEBOUNCE_MS)
            saveUserPreferences(PreferenceUpdate.FontSize(clamped))
        }
    }

    private fun onSetBionicEnabled(enabled: Boolean) {
        currentBionicEnabled = enabled
        val current = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = current.copy(bionicEnabled = enabled)
        if (enabled) warmCacheAround(current.pages, current.currentPageIndex)
        viewModelScope.launch { saveUserPreferences(PreferenceUpdate.BionicEnabled(enabled)) }
    }

    private fun onSetNightMode(nightMode: NightMode) {
        currentNightMode = nightMode
        val current = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = current.copy(nightMode = nightMode)
        viewModelScope.launch { saveUserPreferences(PreferenceUpdate.Theme(nightMode)) }
    }

    private fun onPageChanged(pageIndex: Int) {
        val current = _uiState.value as? ReaderUiState.Success ?: return
        val clamped = pageIndex.coerceIn(0, current.totalPages - 1)
        if (current.currentPageIndex == clamped) return
        _uiState.value = current.copy(currentPageIndex = clamped)
        warmCacheAround(current.pages, clamped)
        val progress = Progress(
            bookId = bookId,
            currentPage = clamped,
            totalPages = current.totalPages,
            lastReadAt = System.currentTimeMillis(),
        )
        latestPendingProgress = progress
        pendingSaveJob?.cancel()
        pendingSaveJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            saveReadingProgress(progress)
            latestPendingProgress = null
        }
    }

    private fun toggleControls() {
        val current = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = current.copy(controlsVisible = !current.controlsVisible)
    }

    private fun onNavigateBack() {
        val progressToSave = latestPendingProgress
        latestPendingProgress = null
        pendingSaveJob?.cancel()
        pendingSaveJob = null
        viewModelScope.launch {
            progressToSave?.let { saveReadingProgress(it) }
            _navigationEvents.tryEmit(Unit)
        }
    }

    private fun onDeleteBook() {
        viewModelScope.launch {
            deleteBook(bookId)
            _navigationEvents.tryEmit(Unit)
        }
    }

    companion object {
        internal const val DEBOUNCE_MS = 500L
        internal const val FONT_DEBOUNCE_MS = 300L
        internal const val SLIDER_DEBOUNCE_MS = 200L
        internal const val PRE_WARM_WINDOW = 2
        private const val MIN_BIONIC_INTENSITY = 0.3f
        private const val MAX_BIONIC_INTENSITY = 0.8f
    }
}
