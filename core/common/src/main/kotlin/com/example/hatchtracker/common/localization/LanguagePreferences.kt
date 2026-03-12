package com.example.hatchtracker.common.localization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "localization_prefs")

@Singleton
class LanguagePreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val LANGUAGE_TAG_KEY = stringPreferencesKey("app_language_tag")
    private val WEIGHT_UNIT_KEY = stringPreferencesKey("weight_unit")

    val appLanguageTag: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_TAG_KEY] ?: ""
        }

    val weightUnit: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[WEIGHT_UNIT_KEY] ?: "SYSTEM"
        }

    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_TAG_KEY] = tag
        }
    }

    suspend fun setWeightUnit(unit: String) {
        context.dataStore.edit { preferences ->
            preferences[WEIGHT_UNIT_KEY] = unit
        }
    }
}
