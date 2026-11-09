package com.tushar.sonora.data.repository

import com.tushar.sonora.data.local.PlaylistDao
import com.tushar.sonora.data.local.PlaylistEntity
import com.tushar.sonora.data.local.PlaylistTrackEntity
import com.tushar.sonora.data.local.SongDao
import com.tushar.sonora.data.local.toTrack
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
) : PlaylistRepository {
    override fun playlists(): Flow<List<PlaylistEntity>> = playlistDao.observeAll()

    override fun tracks(playlistId: Long): Flow<List<Track>> = combine(
        playlistDao.observeTrackIds(playlistId),
        songDao.observeAll(),
    ) { ids, songs ->
        val byId = songs.associateBy { it.id }
        ids.mapNotNull(byId::get).map { it.toTrack() }
    }

    override suspend fun create(name: String): Long = playlistDao.create(PlaylistEntity(name = name.trim()))
    override suspend fun rename(id: Long, name: String) = playlistDao.rename(id, name.trim())
    override suspend fun delete(id: Long) = playlistDao.delete(id)

    override suspend fun addTrack(playlistId: Long, trackId: String) {
        val ids = playlistDao.getTrackIds(playlistId)
        if (trackId !in ids) playlistDao.insertTrack(PlaylistTrackEntity(playlistId, trackId, ids.size))
    }

    override suspend fun removeTrack(playlistId: Long, trackId: String) {
        val ids = playlistDao.getTrackIds(playlistId).filterNot { it == trackId }
        playlistDao.replaceTrackOrder(playlistId, ids)
    }

    override suspend fun move(playlistId: Long, from: Int, to: Int) {
        val ids = playlistDao.getTrackIds(playlistId).toMutableList()
        if (from !in ids.indices || to !in ids.indices || from == to) return
        ids.add(to, ids.removeAt(from))
        playlistDao.replaceTrackOrder(playlistId, ids)
    }

    override suspend fun trackIds(playlistId: Long): List<String> = playlistDao.getTrackIds(playlistId)
}
