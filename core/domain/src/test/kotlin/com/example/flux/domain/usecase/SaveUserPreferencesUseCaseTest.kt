package com.example.flux.domain.usecase

import com.example.flux.domain.model.NightMode
import com.example.flux.domain.model.UserPreferences
import com.example.flux.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SaveUserPreferencesUseCaseTest {

    private val preferencesRepository: PreferencesRepository = mockk()
    private val useCase = SaveUserPreferencesUseCase(preferencesRepository)

    private val transformSlot = slot<UserPreferences.() -> UserPreferences>()

    private fun stubUpdate() {
        coEvery { preferencesRepository.update(capture(transformSlot)) } returns Unit
    }

    private fun capturedResult(): UserPreferences = transformSlot.captured.invoke(UserPreferences())

    @Test
    fun `BionicIntensity within range stored exactly`() = runTest {
        stubUpdate()
        useCase(PreferenceUpdate.BionicIntensity(0.5f))
        assertEquals(0.5f, capturedResult().bionicIntensity)
    }

    @Test
    fun `BionicIntensity below minimum clamped to 0_3`() = runTest {
        stubUpdate()
        useCase(PreferenceUpdate.BionicIntensity(0.1f))
        assertEquals(0.3f, capturedResult().bionicIntensity)
    }

    @Test
    fun `BionicIntensity above maximum clamped to 0_8`() = runTest {
        stubUpdate()
        useCase(PreferenceUpdate.BionicIntensity(1.0f))
        assertEquals(0.8f, capturedResult().bionicIntensity)
    }

    @Test
    fun `FontSize within range stored exactly`() = runTest {
        stubUpdate()
        useCase(PreferenceUpdate.FontSize(20))
        assertEquals(20, capturedResult().defaultFontSizeSp)
    }

    @Test
    fun `FontSize above maximum clamped to 28`() = runTest {
        stubUpdate()
        useCase(PreferenceUpdate.FontSize(100))
        assertEquals(28, capturedResult().defaultFontSizeSp)
    }

    @Test
    fun `FontSize below minimum clamped to 12`() = runTest {
        stubUpdate()
        useCase(PreferenceUpdate.FontSize(4))
        assertEquals(12, capturedResult().defaultFontSizeSp)
    }

    @Test
    fun `BionicEnabled false stored`() = runTest {
        stubUpdate()
        useCase(PreferenceUpdate.BionicEnabled(false))
        assertEquals(false, capturedResult().bionicEnabled)
    }

    @Test
    fun `Theme update stored`() = runTest {
        stubUpdate()
        useCase(PreferenceUpdate.Theme(NightMode.SEPIA))
        assertEquals(NightMode.SEPIA, capturedResult().nightMode)
    }

    @Test
    fun `update is delegated to repository exactly once`() = runTest {
        stubUpdate()
        useCase(PreferenceUpdate.BionicEnabled(true))
        coVerify(exactly = 1) { preferencesRepository.update(any()) }
    }
}
