package com.example.flux.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.data.parser.DocumentParserFactory
import com.example.flux.domain.model.Progress
import com.example.flux.domain.source.ParseResult
import com.example.flux.domain.usecase.DeleteBookUseCase
import com.example.flux.domain.usecase.GetBookByIdUseCase
import com.example.flux.domain.usecase.GetReadingProgressUseCase
import com.example.flux.domain.usecase.SaveReadingProgressUseCase
import com.example.flux.feature.reader.model.ReaderIntent
import com.example.flux.feature.reader.model.ReaderPage
import com.example.flux.feature.reader.model.ReaderUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
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
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // One-shot events that signal the screen to navigate back.
    private val _navigationEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<Unit> = _navigationEvents.asSharedFlow()

    // Debounced persistence: rapid page flips collapse to a single DB write.
    // extraBufferCapacity=64 ensures no flip event is dropped before debounce sees it.
    private val pendingSave = MutableSharedFlow<Progress>(extraBufferCapacity = 64)

    init {
        loadBook()
        collectPendingSaves()
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
                                        controlsVisible = previous?.controlsVisible ?: false,
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

    private fun collectPendingSaves() {
        viewModelScope.launch {
            pendingSave
                .debounce(DEBOUNCE_MS)
                .collect { progress -> saveReadingProgress(progress) }
        }
    }

    fun onIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.PageChanged -> onPageChanged(intent.pageIndex)
            ReaderIntent.ToggleControls -> toggleControls()
            ReaderIntent.DeleteBook -> onDeleteBook()
        }
    }

    private fun onPageChanged(pageIndex: Int) {
        val current = _uiState.value as? ReaderUiState.Success ?: return
        if (current.currentPageIndex == pageIndex) return
        _uiState.value = current.copy(currentPageIndex = pageIndex)
        pendingSave.tryEmit(
            Progress(
                bookId = bookId,
                currentPage = pageIndex,
                totalPages = current.totalPages,
                lastReadAt = System.currentTimeMillis(),
            )
        )
    }

    private fun toggleControls() {
        val current = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = current.copy(controlsVisible = !current.controlsVisible)
    }

    private fun onDeleteBook() {
        viewModelScope.launch {
            deleteBook(bookId)
            _navigationEvents.tryEmit(Unit)
        }
    }

    companion object {
        internal const val DEBOUNCE_MS = 500L
    }
}
