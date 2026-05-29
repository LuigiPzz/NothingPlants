package com.example.nothingplants.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("app_preferences")

class PreferencesManager(private val context: Context) {
    companion object {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_API_KEY_2 = stringPreferencesKey("gemini_api_key_2")
        val FERTILIZER_SUMMARY_CACHE = stringPreferencesKey("fertilizer_summary_cache")
        val SOIL_SUMMARY_CACHE = stringPreferencesKey("soil_summary_cache")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        val LOCATION_LATITUDE = stringPreferencesKey("location_latitude")
        val LOCATION_LONGITUDE = stringPreferencesKey("location_longitude")
        val LOCATION_CITY = stringPreferencesKey("location_city")
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }

    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY]
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = apiKey
        }
    }

    val apiKey2Flow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY_2]
    }

    suspend fun saveApiKey2(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY_2] = apiKey
        }
    }

    val fertilizerSummaryFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FERTILIZER_SUMMARY_CACHE]
    }

    suspend fun saveFertilizerSummary(summaryJson: String) {
        context.dataStore.edit { preferences ->
            preferences[FERTILIZER_SUMMARY_CACHE] = summaryJson
        }
    }

    suspend fun clearFertilizerSummary() {
        context.dataStore.edit { preferences ->
            preferences.remove(FERTILIZER_SUMMARY_CACHE)
        }
    }

    val soilSummaryFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SOIL_SUMMARY_CACHE]
    }

    suspend fun saveSoilSummary(summaryJson: String) {
        context.dataStore.edit { preferences ->
            preferences[SOIL_SUMMARY_CACHE] = summaryJson
        }
    }

    suspend fun clearSoilSummary() {
        context.dataStore.edit { preferences ->
            preferences.remove(SOIL_SUMMARY_CACHE)
        }
    }

    val autoBackupFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_ENABLED] ?: false
    }

    suspend fun saveAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_ENABLED] = enabled
        }
    }

    val locationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LOCATION_ENABLED] ?: false
    }

    suspend fun saveLocationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOCATION_ENABLED] = enabled
        }
    }

    val locationLatitudeFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LOCATION_LATITUDE]
    }

    val locationLongitudeFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LOCATION_LONGITUDE]
    }

    val locationCityFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LOCATION_CITY]
    }

    suspend fun saveLocationData(latitude: String, longitude: String, city: String) {
        context.dataStore.edit { preferences ->
            preferences[LOCATION_LATITUDE] = latitude
            preferences[LOCATION_LONGITUDE] = longitude
            preferences[LOCATION_CITY] = city
        }
    }

    suspend fun clearLocationData() {
        context.dataStore.edit { preferences ->
            preferences.remove(LOCATION_LATITUDE)
            preferences.remove(LOCATION_LONGITUDE)
            preferences.remove(LOCATION_CITY)
        }
    }

    val sortOrderFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SORT_ORDER] ?: "ROOM"
    }

    suspend fun saveSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ORDER] = order
        }
    }
}
