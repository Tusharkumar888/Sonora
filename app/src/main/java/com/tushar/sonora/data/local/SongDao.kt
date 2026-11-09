package com.tushar.sonora.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE")
    fun observeFavorites(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int = 12): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
    fun observeFrequentlyPlayed(limit: Int = 12): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<SongEntity>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE")
    fun search(query: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity)

    @Query("UPDATE songs SET isRemoteAvailable = 0")
    suspend fun markAllRemoteMissing()

    @Query("DELETE FROM songs WHERE isRemoteAvailable = 0 AND localPath IS NULL")
    suspend fun deleteUnavailableUncached()

    @Query("UPDATE songs SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE id = :id")
    suspend fun markPlayed(id: String, playedAt: Long)

    @Query("UPDATE songs SET localPath = :path, durationMs = :durationMs, title = :title, artist = :artist, album = :album WHERE id = :id")
    suspend fun markDownloaded(id: String, path: String, durationMs: Long, title: String, artist: String, album: String)

    @Query("UPDATE songs SET durationMs = CASE WHEN :durationMs > 0 THEN :durationMs ELSE durationMs END, title = CASE WHEN :title = '' THEN title ELSE :title END, artist = CASE WHEN :artist = '' THEN artist ELSE :artist END, album = CASE WHEN :album = '' THEN album ELSE :album END WHERE id = :id")
    suspend fun updateAudioMetadata(id: String, durationMs: Long, title: String, artist: String, album: String)

    @Query("UPDATE songs SET localPath = NULL WHERE id = :id")
    suspend fun clearDownload(id: String)

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM songs WHERE localPath IS NOT NULL")
    fun observeDownloadedBytes(): Flow<Long>

    @Transaction
    suspend fun applyRemoteSnapshot(items: List<SongEntity>) {
        markAllRemoteMissing()
        upsertAll(items)
        deleteUnavailableUncached()
    }
}
