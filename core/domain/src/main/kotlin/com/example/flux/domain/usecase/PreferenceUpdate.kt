package com.example.flux.domain.usecase

import com.example.flux.domain.model.NightMode

sealed class PreferenceUpdate {
    data class BionicIntensity(val value: Float) : PreferenceUpdate()
    data class FontSize(val sp: Int) : PreferenceUpdate()
    data class BionicEnabled(val enabled: Boolean) : PreferenceUpdate()
    data class Theme(val nightMode: NightMode) : PreferenceUpdate()
}
