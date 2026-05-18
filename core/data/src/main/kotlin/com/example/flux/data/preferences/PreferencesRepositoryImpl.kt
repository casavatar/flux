package com.example.flux.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.flux.domain.model.NightMode
import com.example.flux.domain.model.UserPreferences
import com.example.flux.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toUserPreferences() }

    override suspend fun update(transform: UserPreferences.() -> UserPreferences) {
        dataStore.edit { prefs ->
            val updated = prefs.toUserPreferences().transform()
            prefs[Keys.BIONIC_INTENSITY] = updated.bionicIntensity
            prefs[Keys.DEFAULT_FONT_SIZE_SP] = updated.defaultFontSizeSp
            prefs[Keys.BIONIC_ENABLED] = updated.bionicEnabled
            prefs[Keys.NIGHT_MODE] = updated.nightMode.name
        }
    }

    private fun Preferences.toUserPreferences() = UserPreferences(
        bionicIntensity = this[Keys.BIONIC_INTENSITY] ?: 0.5f,
        defaultFontSizeSp = this[Keys.DEFAULT_FONT_SIZE_SP] ?: 18,
        bionicEnabled = this[Keys.BIONIC_ENABLED] ?: true,
        nightMode = NightMode.valueOf(this[Keys.NIGHT_MODE] ?: NightMode.SYSTEM.name),
    )

    private object Keys {
        val BIONIC_INTENSITY = floatPreferencesKey("bionic_intensity")
        val DEFAULT_FONT_SIZE_SP = intPreferencesKey("default_font_size_sp")
        val BIONIC_ENABLED = booleanPreferencesKey("bionic_enabled")
        val NIGHT_MODE = stringPreferencesKey("night_mode")
    }
}
