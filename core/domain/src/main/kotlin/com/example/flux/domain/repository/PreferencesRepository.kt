package com.example.flux.domain.repository

import com.example.flux.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun update(transform: UserPreferences.() -> UserPreferences)
}
