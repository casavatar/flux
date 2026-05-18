package com.example.flux.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.example.flux.domain.model.NightMode
import com.example.flux.domain.model.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var tmpFile: File
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setUp() {
        tmpFile = File.createTempFile("prefs_test_${System.nanoTime()}", ".preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { tmpFile },
        )
        repository = PreferencesRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        tmpFile.delete()
    }

    @Test
    fun `default values applied before any write`() = testScope.runTest {
        repository.userPreferences.test {
            val prefs = awaitItem()
            assertEquals(0.5f, prefs.bionicIntensity)
            assertEquals(18, prefs.defaultFontSizeSp)
            assertTrue(prefs.bionicEnabled)
            assertEquals(NightMode.SYSTEM, prefs.nightMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updating bionicIntensity emits new value`() = testScope.runTest {
        repository.userPreferences.test {
            awaitItem() // defaults
            repository.update { copy(bionicIntensity = 0.7f) }
            assertEquals(0.7f, awaitItem().bionicIntensity)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updating defaultFontSizeSp emits new value`() = testScope.runTest {
        repository.userPreferences.test {
            awaitItem() // defaults
            repository.update { copy(defaultFontSizeSp = 22) }
            assertEquals(22, awaitItem().defaultFontSizeSp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disabling bionicEnabled emits false`() = testScope.runTest {
        repository.userPreferences.test {
            awaitItem() // defaults
            repository.update { copy(bionicEnabled = false) }
            assertFalse(awaitItem().bionicEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updating nightMode enum persists and emits correct variant`() = testScope.runTest {
        repository.userPreferences.test {
            awaitItem() // defaults
            repository.update { copy(nightMode = NightMode.DARK) }
            assertEquals(NightMode.DARK, awaitItem().nightMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flow emits on every sequential update`() = testScope.runTest {
        repository.userPreferences.test {
            awaitItem() // defaults
            repository.update { copy(defaultFontSizeSp = 20) }
            assertEquals(20, awaitItem().defaultFontSizeSp)
            repository.update { copy(defaultFontSizeSp = 24) }
            assertEquals(24, awaitItem().defaultFontSizeSp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `update preserves unchanged fields`() = testScope.runTest {
        repository.update { copy(bionicIntensity = 0.6f) }

        repository.userPreferences.test {
            val prefs = awaitItem()
            assertEquals(0.6f, prefs.bionicIntensity)
            assertEquals(18, prefs.defaultFontSizeSp)
            assertTrue(prefs.bionicEnabled)
            assertEquals(NightMode.SYSTEM, prefs.nightMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `all NightMode variants round-trip correctly`() = testScope.runTest {
        for (mode in NightMode.entries) {
            repository.update { copy(nightMode = mode) }
        }
        repository.userPreferences.test {
            assertEquals(NightMode.entries.last(), awaitItem().nightMode)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
