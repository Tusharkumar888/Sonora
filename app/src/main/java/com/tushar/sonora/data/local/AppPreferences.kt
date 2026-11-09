package com.tushar.sonora.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sonoraDataStore by preferencesDataStore(name = "sonora_settings")

data class AppSettings(
    val theme: String = "dark",
    val autoSync: Boolean = true,
    val driveApiKey: String? = null,
    val musicFolderLink: String? = null,
    val musicFolderId: String? = null,
    val musicFolderResourceKey: String? = null,
    val lastTrackId: String? = null,
    val lastPosition: Long = 0L,
    val resumePlayback: Boolean = false,
) {
    val driveConfigured: Boolean
        get() = !driveApiKey.isNullOrBlank() && !musicFolderId.isNullOrBlank()
}

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val autoSync = booleanPreferencesKey("auto_sync")
        val driveApiKey = stringPreferencesKey("drive_api_key")
        val musicFolderLink = stringPreferencesKey("music_folder_link")
        val musicFolderId = stringPreferencesKey("music_folder_id")
        val musicFolderResourceKey = stringPreferencesKey("music_folder_resource_key")
        val lastTrackId = stringPreferencesKey("last_track_id")
        val lastPosition = longPreferencesKey("last_position")
        val resumePlayback = booleanPreferencesKey("resume_playback")
    }

    val settings: Flow<AppSettings> = context.sonoraDataStore.data.map { p ->
        AppSettings(
            theme = p[Keys.theme] ?: "dark",
            autoSync = p[Keys.autoSync] ?: true,
            driveApiKey = p[Keys.driveApiKey],
            musicFolderLink = p[Keys.musicFolderLink],
            musicFolderId = p[Keys.musicFolderId],
            musicFolderResourceKey = p[Keys.musicFolderResourceKey],
            lastTrackId = p[Keys.lastTrackId],
            lastPosition = p[Keys.lastPosition] ?: 0L,
            resumePlayback = p[Keys.resumePlayback] ?: false,
        )
    }

    suspend fun setTheme(value: String) = context.sonoraDataStore.edit { it[Keys.theme] = value }
    suspend fun setAutoSync(value: Boolean) = context.sonoraDataStore.edit { it[Keys.autoSync] = value }
    suspend fun setDriveConnection(link: String, id: String, resourceKey: String?, apiKey: String) = context.sonoraDataStore.edit {
        it[Keys.musicFolderLink] = link.trim()
        it[Keys.musicFolderId] = id
        if (resourceKey.isNullOrBlank()) it.remove(Keys.musicFolderResourceKey) else it[Keys.musicFolderResourceKey] = resourceKey
        it[Keys.driveApiKey] = apiKey.trim()
    }
    suspend fun clearDriveConnection() = context.sonoraDataStore.edit {
        it.remove(Keys.musicFolderLink)
        it.remove(Keys.musicFolderId)
        it.remove(Keys.musicFolderResourceKey)
        it.remove(Keys.driveApiKey)
    }
    suspend fun savePlayback(id: String?, position: Long, playing: Boolean) = context.sonoraDataStore.edit {
        if (id == null) it.remove(Keys.lastTrackId) else it[Keys.lastTrackId] = id
        it[Keys.lastPosition] = position.coerceAtLeast(0L)
        it[Keys.resumePlayback] = playing
    }

}
