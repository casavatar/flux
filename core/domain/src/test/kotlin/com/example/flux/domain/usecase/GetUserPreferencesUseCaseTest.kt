package com.example.flux.domain.usecase

import app.cash.turbine.test
import com.example.flux.domain.model.UserPreferences
import com.example.flux.domain.repository.PreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetUserPreferencesUseCaseTest {

    private val preferencesRepository: PreferencesRepository = mockk()
    private val useCase = GetUserPreferencesUseCase(preferencesRepository)

    @Test
    fun `emits default preferences when DataStore is empty`() = runTest {
        val defaults = UserPreferences()
        every { preferencesRepository.userPreferences } returns flowOf(defaults)

        useCase().test {
            assertEquals(defaults, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits updated preferences after DataStore change`() = runTest {
        val initial = UserPreferences()
        val updated = UserPreferences(bionicIntensity = 0.7f)
        every { preferencesRepository.userPreferences } returns flowOf(initial, updated)

        useCase().test {
            assertEquals(initial, awaitItem())
            assertEquals(updated, awaitItem())
            awaitComplete()
        }
    }
}
