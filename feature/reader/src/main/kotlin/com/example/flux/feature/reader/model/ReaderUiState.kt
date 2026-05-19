package com.example.flux.feature.reader.model

import com.example.flux.domain.model.Book

sealed class ReaderUiState {

    data object Loading : ReaderUiState()

    data class Success(
        val book: Book,
        val pages: List<ReaderPage>,
        val currentPageIndex: Int,
        val totalPages: Int,
        val fontSizeSp: Int = DEFAULT_FONT_SIZE_SP,
        val controlsVisible: Boolean = false,
    ) : ReaderUiState()

    companion object {
        const val DEFAULT_FONT_SIZE_SP = 18
    }

    data class Error(
        val message: String,
        val canDelete: Boolean = false,
    ) : ReaderUiState()
}
