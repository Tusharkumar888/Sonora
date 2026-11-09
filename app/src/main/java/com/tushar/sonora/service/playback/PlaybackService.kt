package com.tushar.sonora.service.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tushar.sonora.data.local.AppPreferences
import com.tushar.sonora.data.local.SongDao
import com.tushar.sonora.data.local.toTrack
import com.tushar.sonora.domain.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import javax.inject.Inject
import java.io.File
import com.tushar.sonora.service.sync.DownloadSongWorker

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var httpClient: OkHttpClient
    @Inject lateinit var songDao: SongDao
    @Inject lateinit var music: MusicRepository
    @Inject lateinit var preferences: AppPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val fallbackAttemptedFor = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        val httpFactory = OkHttpDataSource.Factory(httpClient)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val trackId = mediaItem?.mediaId ?: return
                fallbackAttemptedFor.remove(trackId)
                serviceScope.launch { music.markPlayed(trackId) }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val item = player.currentMediaItem ?: return
                if (item.localConfiguration?.uri?.scheme == "file") return
                val trackId = item.mediaId
                if (!fallbackAttemptedFor.add(trackId)) return
                serviceScope.launch { downloadAndResume(trackId) }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                saveState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                saveState()
                persistPlayerMetadata()
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                persistPlayerMetadata(mediaMetadata)
            }
        })
        serviceScope.launch { restorePlayback() }
        serviceScope.launch {
            while (isActive) {
                saveState()
                delay(5_000)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    private suspend fun restorePlayback() {
        val saved = preferences.settings.first()
        val id = saved.lastTrackId ?: return
        val entity = songDao.getById(id) ?: return
        val apiKey = saved.driveApiKey.orEmpty()
        if (apiKey.isBlank() && entity.localPath.isNullOrBlank() && entity.webContentLink.isNullOrBlank()) return
        player.setMediaItem(entity.toTrack().toMediaItem(apiKey))
        player.prepare()
        if (saved.lastPosition > 0L) player.seekTo(saved.lastPosition)
        if (saved.resumePlayback) player.play()
    }

    private suspend fun downloadAndResume(trackId: String) {
        val track = music.track(trackId) ?: return
        if (track.localPath?.let(::File)?.isFile == true) return
        val apiKey = preferences.settings.first().driveApiKey.orEmpty()

        val uniqueName = "sonora_download_$trackId"
        val request = OneTimeWorkRequestBuilder<DownloadSongWorker>()
            .setInputData(workDataOf(DownloadSongWorker.KEY_TRACK_ID to trackId))
            .build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, request)

        val result = withTimeoutOrNull(DOWNLOAD_FALLBACK_TIMEOUT_MS) {
            workManager.getWorkInfosForUniqueWorkFlow(uniqueName)
                .first { workInfos -> workInfos.any { it.state.isFinished } }
                .firstOrNull()
        }
        if (result?.state != WorkInfo.State.SUCCEEDED) return

        val updatedTrack = music.track(trackId) ?: return
        if (updatedTrack.localPath?.let(::File)?.isFile != true) return
        if (player.currentMediaItem?.mediaId != trackId) return

        val queueIndex = player.currentMediaItemIndex
        if (queueIndex < 0 || queueIndex >= player.mediaItemCount) return
        val position = player.currentPosition
        val queue = (0 until player.mediaItemCount).map { index ->
            val queuedItem = player.getMediaItemAt(index)
            if (queuedItem.mediaId == trackId) updatedTrack.toMediaItem(apiKey) else queuedItem
        }
        player.setMediaItems(queue, queueIndex, position)
        player.prepare()
        player.play()
    }

    private fun saveState() {
        val id = player.currentMediaItem?.mediaId
        serviceScope.launch { preferences.savePlayback(id, player.currentPosition, player.isPlaying) }
    }

    private fun persistPlayerMetadata(metadata: androidx.media3.common.MediaMetadata = player.mediaMetadata) {
        val id = player.currentMediaItem?.mediaId ?: return
        serviceScope.launch {
            music.updateAudioMetadata(
                id = id,
                durationMs = player.duration.takeIf { it > 0L } ?: 0L,
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                album = metadata.albumTitle?.toString().orEmpty(),
            )
        }
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!player.isPlaying) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private companion object {
        const val DOWNLOAD_FALLBACK_TIMEOUT_MS = 15 * 60 * 1000L
    }
}
