package com.example.flux.domain.usecase

import com.example.flux.domain.repository.PreferencesRepository
import javax.inject.Inject

class SaveUserPreferencesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    suspend operator fun invoke(update: PreferenceUpdate) {
        preferencesRepository.update {
            when (update) {
                is PreferenceUpdate.BionicIntensity -> copy(bionicIntensity = update.value.coerceIn(0.3f, 0.8f))
                is PreferenceUpdate.FontSize -> copy(defaultFontSizeSp = update.sp.coerceIn(12, 28))
                is PreferenceUpdate.BionicEnabled -> copy(bionicEnabled = update.enabled)
                is PreferenceUpdate.Theme -> copy(nightMode = update.nightMode)
            }
        }
    }
}
