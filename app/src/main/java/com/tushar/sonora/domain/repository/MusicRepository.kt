package com.tushar.sonora.domain.repository

import com.tushar.sonora.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun tracks(): Flow<List<Track>>
    fun favorites(): Flow<List<Track>>
    fun recentlyPlayed(): Flow<List<Track>>
    fun frequentlyPlayed(): Flow<List<Track>>
    fun search(query: String): Flow<List<Track>>
    fun downloadedBytes(): Flow<Long>
    suspend fun track(id: String): Track?
    suspend fun tracks(ids: List<String>): List<Track>
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun markPlayed(id: String)
    suspend fun setDownloaded(id: String, path: String, durationMs: Long, title: String, artist: String, album: String)
    suspend fun updateAudioMetadata(id: String, durationMs: Long, title: String, artist: String, album: String)
    suspend fun clearDownload(id: String)
}

interface DriveRepository {
    suspend fun sync(folderId: String, apiKey: String, folderResourceKey: String?): Int
}

interface PlaylistRepository {
    fun playlists(): Flow<List<com.tushar.sonora.data.local.PlaylistEntity>>
    fun tracks(playlistId: Long): Flow<List<Track>>
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
    suspend fun addTrack(playlistId: Long, trackId: String)
    suspend fun removeTrack(playlistId: Long, trackId: String)
    suspend fun move(playlistId: Long, from: Int, to: Int)
    suspend fun trackIds(playlistId: Long): List<String>
}
