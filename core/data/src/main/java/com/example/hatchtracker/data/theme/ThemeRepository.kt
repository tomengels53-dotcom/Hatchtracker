package com.example.hatchtracker.data.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hatchtracker.domain.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class ThemeRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val themeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val storedValue = preferences[themeKey]
        
        // If no value in DataStore, attempt migration from SharedPreferences
        if (storedValue == null) {
            migrateFromSharedPreferences()
            // After migration, read again
            context.dataStore.data.first()[themeKey]?.let { parseThemeMode(it) } ?: ThemeMode.LIGHT
        } else {
            parseThemeMode(storedValue)
        }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = mode.name.lowercase(java.util.Locale.ROOT)
        }
    }

    private suspend fun migrateFromSharedPreferences() {
        try {
            val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val oldTheme = sharedPrefs.getString("theme_mode", null)
            
            if (oldTheme != null) {
                // Migrate to DataStore
                val migratedMode = when (oldTheme) {
                    "dark" -> ThemeMode.DARK
                    "light" -> ThemeMode.LIGHT
                    "system" -> ThemeMode.LIGHT // Fallback: system -> light
                    else -> ThemeMode.LIGHT
                }
                
                context.dataStore.edit { preferences ->
                    preferences[themeKey] = migratedMode.name.lowercase(java.util.Locale.ROOT)
                }
                
                // Remove only the theme_mode key from SharedPreferences
                sharedPrefs.edit().remove("theme_mode").apply()
            } else {
                // No old preference, set default
                context.dataStore.edit { preferences ->
                    preferences[themeKey] = ThemeMode.LIGHT.name.lowercase(java.util.Locale.ROOT)
                }
            }
        } catch (e: Exception) {
            // Safe fallback: if migration fails, default to LIGHT
            context.dataStore.edit { preferences ->
                preferences[themeKey] = ThemeMode.LIGHT.name.lowercase(java.util.Locale.ROOT)
            }
        }
    }

    private fun parseThemeMode(value: String): ThemeMode {
        return when (value.lowercase(java.util.Locale.ROOT)) {
            "dark" -> ThemeMode.DARK
            "light" -> ThemeMode.LIGHT
            else -> ThemeMode.LIGHT // Safe fallback for invalid values
        }
    }
}
