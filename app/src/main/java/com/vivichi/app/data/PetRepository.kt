package com.vivichi.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "vivichi_state")

/**
 * Mirrors the original app's single localStorage-JSON-blob persistence strategy:
 * the whole [AppState] is (de)serialized as one JSON string under one key.
 */
class PetRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val stateKey = stringPreferencesKey("vivichi_state_json")

    val stateFlow: Flow<AppState> = context.dataStore.data.map { prefs ->
        val raw = prefs[stateKey]
        if (raw.isNullOrBlank()) {
            AppState()
        } else {
            try {
                json.decodeFromString(AppState.serializer(), raw)
            } catch (e: Exception) {
                AppState()
            }
        }
    }

    suspend fun current(): AppState = stateFlow.first()

    suspend fun save(state: AppState) {
        context.dataStore.edit { prefs ->
            prefs[stateKey] = json.encodeToString(AppState.serializer(), state)
        }
    }

    suspend fun reset() {
        save(AppState())
    }
}
