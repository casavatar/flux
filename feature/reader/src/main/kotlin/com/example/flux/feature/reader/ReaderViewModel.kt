package com.example.flux.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flux.data.parser.DocumentParserFactory
import com.example.flux.domain.model.Progress
import com.example.flux.domain.source.ParseResult
import com.example.flux.domain.usecase.GetBookByIdUseCase
import com.example.flux.domain.usecase.GetReadingProgressUseCase
import com.example.flux.domain.usecase.SaveReadingProgressUseCase
import com.example.flux.feature.reader.model.ReaderIntent
import com.example.flux.feature.reader.model.ReaderPage
import com.example.flux.feature.reader.model.ReaderUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getBookById: GetBookByIdUseCase,
    private val documentParserFactory: DocumentParserFactory,
    private val getReadingProgress: GetReadingProgressUseCase,
    private val saveReadingProgress: SaveReadingProgressUseCase,
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadBook()
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
                        is ParseResult.Error -> {
                            _uiState.value = ReaderUiState.Error(
                                result.cause.message ?: "Failed to parse document"
                            )
                        }
                    }
                }
            }
        }
    }

    fun onIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.PageChanged -> onPageChanged(intent.pageIndex)
            ReaderIntent.ToggleControls -> toggleControls()
        }
    }

    private fun onPageChanged(pageIndex: Int) {
        val current = _uiState.value as? ReaderUiState.Success ?: return
        if (current.currentPageIndex == pageIndex) return
        _uiState.value = current.copy(currentPageIndex = pageIndex)
        viewModelScope.launch {
            saveReadingProgress(
                Progress(
                    bookId = bookId,
                    currentPage = pageIndex,
                    totalPages = current.totalPages,
                    lastReadAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun toggleControls() {
        val current = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = current.copy(controlsVisible = !current.controlsVisible)
    }
}
