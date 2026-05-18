package com.example.flux.domain.usecase

import com.example.flux.domain.model.UserPreferences
import com.example.flux.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserPreferencesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    operator fun invoke(): Flow<UserPreferences> = preferencesRepository.userPreferences
}
