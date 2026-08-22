package com.streamlab.tv.data.repository

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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DEFAULT_M3U_URL = "https://lucky-narwhal-1452d9.netlify.app/default-playlist.m3u8"
    }

    private val m3uUrlKey = stringPreferencesKey("m3u_url")
    private val tmdbKeyKey = stringPreferencesKey("tmdb_key")

    val m3uUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val saved = preferences[m3uUrlKey]
        if (saved.isNullOrBlank()) DEFAULT_M3U_URL else saved
    }

    val tmdbKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[tmdbKeyKey] ?: ""
    }

    suspend fun saveM3uUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[m3uUrlKey] = url
        }
    }

    suspend fun saveTmdbKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[tmdbKeyKey] = key
        }
    }
}
