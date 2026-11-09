package com.tushar.sonora.data.repository

import com.google.gson.Gson
import com.tushar.sonora.data.local.SongDao
import com.tushar.sonora.data.local.SongEntity
import com.tushar.sonora.data.remote.drive.DriveApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DriveRepositoryImplTest {
    @Test
    fun syncReadsEveryPageAndStoresOnlySupportedAudio() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse().setBody(
                    """{"nextPageToken":"page-two","files":[{"id":"song-one","name":"Artist - Song.mp3","mimeType":"audio/mpeg","size":"1024","modifiedTime":"2026-11-01T00:00:00Z","parents":["public-folder-id"],"resourceKey":"song-key","webContentLink":"https://drive.google.com/uc?id=song-one&export=download&resourcekey=song-key"},{"id":"notes","name":"notes.txt","mimeType":"text/plain"}]}""",
                ),
            )
            server.enqueue(
                MockResponse().setBody(
                    """{"files":[{"id":"song-two","name":"Room Tone.wav","mimeType":"audio/wav","size":"2048","modifiedTime":"2026-11-02T00:00:00Z"}]}""",
                ),
            )
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .build()
                .create(DriveApi::class.java)
            val dao = RecordingSongDao()
            val repository = DriveRepositoryImpl(api, dao)

            assertEquals(2, repository.sync("public-folder-id", "test-api-key", "folder-key"))
            assertEquals(listOf("song-one", "song-two"), dao.remoteSnapshot.map { it.id })
            assertEquals("Artist - Song", dao.remoteSnapshot.first().title)
            assertEquals("Unknown artist", dao.remoteSnapshot.first().artist)
            assertEquals("song-key", dao.remoteSnapshot.first().resourceKey)
            assertEquals("https://drive.google.com/uc?id=song-one&export=download&resourcekey=song-key", dao.remoteSnapshot.first().webContentLink)

            val firstRequest = server.takeRequest()
            assertEquals("GET", firstRequest.method)
            assertEquals("/drive/v3/files", firstRequest.requestUrl?.encodedPath)
            assertEquals("'public-folder-id' in parents and trashed = false", firstRequest.requestUrl?.queryParameter("q"))
            assertNull(firstRequest.requestUrl?.queryParameter("pageToken"))
            assertEquals("drive", firstRequest.requestUrl?.queryParameter("spaces"))
            assertEquals("test-api-key", firstRequest.getHeader("x-goog-api-key"))
            assertEquals("public-folder-id/folder-key", firstRequest.getHeader("X-Goog-Drive-Resource-Keys"))

            val secondRequest = server.takeRequest()
            assertEquals("page-two", secondRequest.requestUrl?.queryParameter("pageToken"))
        } finally {
            server.shutdown()
        }
    }
}

private class RecordingSongDao : SongDao {
    var remoteSnapshot: List<SongEntity> = emptyList()
        private set

    override fun observeAll(): Flow<List<SongEntity>> = emptyFlow()
    override fun observeFavorites(): Flow<List<SongEntity>> = emptyFlow()
    override fun observeRecentlyPlayed(limit: Int): Flow<List<SongEntity>> = emptyFlow()
    override fun observeFrequentlyPlayed(limit: Int): Flow<List<SongEntity>> = emptyFlow()
    override suspend fun getById(id: String): SongEntity? = null
    override suspend fun getByIds(ids: List<String>): List<SongEntity> = emptyList()
    override fun search(query: String): Flow<List<SongEntity>> = emptyFlow()
    override suspend fun upsertAll(songs: List<SongEntity>) { remoteSnapshot = songs }
    override suspend fun upsert(song: SongEntity) = Unit
    override suspend fun markAllRemoteMissing() = Unit
    override suspend fun deleteUnavailableUncached() = Unit
    override suspend fun setFavorite(id: String, favorite: Boolean) = Unit
    override suspend fun markPlayed(id: String, playedAt: Long) = Unit
    override suspend fun markDownloaded(id: String, path: String, durationMs: Long, title: String, artist: String, album: String) = Unit
    override suspend fun updateAudioMetadata(id: String, durationMs: Long, title: String, artist: String, album: String) = Unit
    override suspend fun clearDownload(id: String) = Unit
    override fun observeDownloadedBytes(): Flow<Long> = emptyFlow()
}
