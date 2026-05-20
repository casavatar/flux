package com.example.flux.feature.reader.model

import com.example.flux.domain.model.Book
import com.example.flux.domain.model.NightMode

sealed class ReaderUiState {

    data object Loading : ReaderUiState()

    data class Success(
        val book: Book,
        val pages: List<ReaderPage>,
        val currentPageIndex: Int,
        val totalPages: Int,
        val fontSizeSp: Int = DEFAULT_FONT_SIZE_SP,
        val controlsVisible: Boolean = false,
        val bionicIntensity: Float = DEFAULT_BIONIC_INTENSITY,
        val bionicEnabled: Boolean = true,
        val nightMode: NightMode = NightMode.SYSTEM,
    ) : ReaderUiState()

    companion object {
        const val DEFAULT_FONT_SIZE_SP = 18
        const val DEFAULT_BIONIC_INTENSITY = 0.5f
    }

    data class Error(
        val message: String,
        val canDelete: Boolean = false,
    ) : ReaderUiState()
}
