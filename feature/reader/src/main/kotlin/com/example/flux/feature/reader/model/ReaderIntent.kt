package com.example.flux.feature.reader.model

sealed class ReaderIntent {
    data class PageChanged(val pageIndex: Int) : ReaderIntent()
    data object ToggleControls : ReaderIntent()
    data object DeleteBook : ReaderIntent()
    data object NavigateBack : ReaderIntent()
}
