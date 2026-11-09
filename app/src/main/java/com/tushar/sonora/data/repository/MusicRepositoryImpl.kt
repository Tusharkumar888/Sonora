package com.tushar.sonora.data.repository

import com.tushar.sonora.data.local.SongDao
import com.tushar.sonora.data.local.SongEntity
import com.tushar.sonora.data.local.toTrack
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(private val songDao: SongDao) : MusicRepository {
    override fun tracks(): Flow<List<Track>> = songDao.observeAll().map { songs -> songs.map(SongEntity::toTrack) }
    override fun favorites(): Flow<List<Track>> = songDao.observeFavorites().map { songs -> songs.map(SongEntity::toTrack) }
    override fun recentlyPlayed(): Flow<List<Track>> = songDao.observeRecentlyPlayed().map { songs -> songs.map(SongEntity::toTrack) }
    override fun frequentlyPlayed(): Flow<List<Track>> = songDao.observeFrequentlyPlayed().map { songs -> songs.map(SongEntity::toTrack) }
    override fun search(query: String): Flow<List<Track>> = songDao.search(query).map { songs -> songs.map(SongEntity::toTrack) }
    override fun downloadedBytes(): Flow<Long> = songDao.observeDownloadedBytes()
    override suspend fun track(id: String): Track? = songDao.getById(id)?.toTrack()
    override suspend fun tracks(ids: List<String>): List<Track> = if (ids.isEmpty()) emptyList() else songDao.getByIds(ids).map(SongEntity::toTrack)
    override suspend fun setFavorite(id: String, favorite: Boolean) = songDao.setFavorite(id, favorite)
    override suspend fun markPlayed(id: String) = songDao.markPlayed(id, System.currentTimeMillis())
    override suspend fun setDownloaded(id: String, path: String, durationMs: Long, title: String, artist: String, album: String) =
        songDao.markDownloaded(id, path, durationMs, title, artist, album)
    override suspend fun updateAudioMetadata(id: String, durationMs: Long, title: String, artist: String, album: String) =
        songDao.updateAudioMetadata(id, durationMs, title, artist, album)
    override suspend fun clearDownload(id: String) = songDao.clearDownload(id)
}
