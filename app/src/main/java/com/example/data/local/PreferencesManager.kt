package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nabraawy_prefs")

data class UserPreferences(
    val language: AppLanguage = AppLanguage.AR,
    val isMusicEnabled: Boolean = true,
    val isDarkTheme: Boolean = true
)

class PreferencesManager(private val context: Context) {
    private val keyLanguage = stringPreferencesKey("language")
    private val keyMusicEnabled = booleanPreferencesKey("music_enabled")
    private val keyDarkTheme = booleanPreferencesKey("dark_theme")

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val langCode = prefs[keyLanguage] ?: AppLanguage.AR.code
        UserPreferences(
            language = if (langCode == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.AR,
            isMusicEnabled = prefs[keyMusicEnabled] ?: true,
            isDarkTheme = prefs[keyDarkTheme] ?: true
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[keyLanguage] = language.code }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { it[keyMusicEnabled] = enabled }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[keyDarkTheme] = dark }
    }
}
