package com.example.flux.feature.reader.model

import com.example.flux.domain.model.NightMode

sealed class ReaderIntent {
    data class PageChanged(val pageIndex: Int) : ReaderIntent()
    data object ToggleControls : ReaderIntent()
    data object DeleteBook : ReaderIntent()
    data object NavigateBack : ReaderIntent()
    data class SetBionicIntensity(val intensity: Float) : ReaderIntent()
    data class SetFontSize(val fontSizeSp: Int) : ReaderIntent()
    data class SetBionicEnabled(val enabled: Boolean) : ReaderIntent()
    data class SetNightMode(val nightMode: NightMode) : ReaderIntent()
}
