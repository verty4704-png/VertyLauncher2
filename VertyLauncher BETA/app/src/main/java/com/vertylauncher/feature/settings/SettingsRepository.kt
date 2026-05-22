package com.vertylauncher.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private companion object {
        val RENDERER = stringPreferencesKey("renderer")
        val MAX_MEMORY = intPreferencesKey("max_memory")
        val USERNAME = stringPreferencesKey("username")
        val SELECTED_VERSION = stringPreferencesKey("selected_version")
        val HAS_LAUNCHED = booleanPreferencesKey("has_launched_before")
        val AUTH_TYPE = stringPreferencesKey("auth_type") // offline, elyby, microsoft
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val UUID = stringPreferencesKey("user_uuid")
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data.map { prefs ->
        LauncherSettings(
            renderer = prefs[RENDERER] ?: "angle",
            maxMemory = prefs[MAX_MEMORY] ?: 2048,
            username = prefs[USERNAME] ?: "Player",
            selectedVersion = prefs[SELECTED_VERSION] ?: "",
            hasLaunchedBefore = prefs[HAS_LAUNCHED] ?: false,
            authType = prefs[AUTH_TYPE] ?: "offline",
            accessToken = prefs[ACCESS_TOKEN] ?: "",
            refreshToken = prefs[REFRESH_TOKEN] ?: "",
            uuid = prefs[UUID] ?: ""
        )
    }

    suspend fun updateRenderer(value: String) = context.dataStore.edit { it[RENDERER] = value }
    suspend fun updateMaxMemory(value: Int) = context.dataStore.edit { it[MAX_MEMORY] = value }
    suspend fun updateUsername(value: String) = context.dataStore.edit { it[USERNAME] = value }
    suspend fun updateSelectedVersion(value: String) = context.dataStore.edit { it[SELECTED_VERSION] = value }
    suspend fun setHasLaunched() = context.dataStore.edit { it[HAS_LAUNCHED] = true }
    suspend fun updateAuthType(value: String) = context.dataStore.edit { it[AUTH_TYPE] = value }
    suspend fun updateAccessToken(value: String) = context.dataStore.edit { it[ACCESS_TOKEN] = value }
    suspend fun updateRefreshToken(value: String) = context.dataStore.edit { it[REFRESH_TOKEN] = value }
    suspend fun updateUuid(value: String) = context.dataStore.edit { it[UUID] = value }
}

data class LauncherSettings(
    val renderer: String,
    val maxMemory: Int,
    val username: String,
    val selectedVersion: String,
    val hasLaunchedBefore: Boolean = false,
    val authType: String = "offline",
    val accessToken: String = "",
    val refreshToken: String = "",
    val uuid: String = ""
)
