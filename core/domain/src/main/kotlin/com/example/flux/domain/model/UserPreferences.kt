package com.example.flux.domain.model

data class UserPreferences(
    val bionicIntensity: Float = 0.5f,
    val defaultFontSizeSp: Int = 18,
    val bionicEnabled: Boolean = true,
    val nightMode: NightMode = NightMode.SYSTEM,
)
