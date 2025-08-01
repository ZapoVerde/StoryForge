package com.example.storyforge.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsDataStore(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    suspend fun saveApiToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("api_token_encrypted")] = token
        }
    }

    suspend fun getApiToken(): String? {
        return context.dataStore.data.map {
            it[stringPreferencesKey("api_token_encrypted")]
        }.first()
    }

    suspend fun saveGlobalSettings(json: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("global_settings")] = json
        }
    }

    suspend fun getGlobalSettings(): String? {
        return context.dataStore.data.map {
            it[stringPreferencesKey("global_settings")]
        }.first()
    }
}