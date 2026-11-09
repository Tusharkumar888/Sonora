package com.tushar.sonora.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tushar.sonora.data.local.AppPreferences
import com.tushar.sonora.data.local.PlaylistEntity
import com.tushar.sonora.data.remote.drive.DriveFolderLinkParser
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.domain.repository.DriveRepository
import com.tushar.sonora.domain.repository.MusicRepository
import com.tushar.sonora.domain.repository.PlaylistRepository
import com.tushar.sonora.service.playback.toMediaItem
import com.tushar.sonora.service.sync.DownloadSongWorker
import com.tushar.sonora.service.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.media3.common.Player
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val music: MusicRepository,
    private val drive: DriveRepository,
    private val playlistsRepository: PlaylistRepository,
    private val preferences: AppPreferences,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {
    private val workManager = WorkManager.getInstance(context)
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.tushar.sonora.data.local.AppSettings())
    val tracks = music.tracks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites = music.favorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentlyPlayed = music.recentlyPlayed().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val frequentlyPlayed = music.frequentlyPlayed().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists = playlistsRepository.playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<PlaylistEntity>())
    val downloadedBytes = music.downloadedBytes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults = query.debounce(250).flatMapLatest { term ->
        if (term.isBlank()) music.tracks() else music.search(term.trim())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _syncing = MutableStateFlow(false)
    val syncing = _syncing.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        viewModelScope.launch { preferences.settings.collect { syncScheduler.setEnabled(it.autoSync && it.driveConfigured) } }
    }

    fun setQuery(value: String) { _query.value = value }
    fun clearMessage() { _message.value = null }

    fun syncNow() = viewModelScope.launch {
        val configuration = settings.value
        syncConfiguredLibrary(configuration.musicFolderId, configuration.driveApiKey, configuration.musicFolderResourceKey)
    }

    private suspend fun syncConfiguredLibrary(folderId: String?, apiKey: String?, resourceKey: String?) {
        if (_syncing.value) return
        if (folderId.isNullOrBlank() || apiKey.isNullOrBlank()) {
            _message.value = "Add a public Drive folder link and API key in Settings first."
            return
        }
        _syncing.value = true
        runCatching {
            drive.sync(folderId, apiKey, resourceKey)
        }.onSuccess { count -> _message.value = "Library updated · $count ${if (count == 1) "song" else "songs"}" }
            .onFailure { _message.value = it.message ?: "Sync failed. Check your connection and try again." }
        _syncing.value = false
    }

    fun toggleFavorite(track: Track) = viewModelScope.launch { music.setFavorite(track.id, !track.isFavorite) }
    fun markPlayed(track: Track) = viewModelScope.launch { music.markPlayed(track.id) }

    fun playTrack(player: Player?, track: Track, queue: List<Track>) {
        val target = queue.ifEmpty { listOf(track) }
        val index = target.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        player ?: run { _message.value = "The player is still starting. Try again in a moment."; return }
        val apiKey = settings.value.driveApiKey.orEmpty()
        if (apiKey.isBlank() && target.any { !it.isDownloaded && it.webContentLink.isNullOrBlank() }) {
            _message.value = "Add a public Drive folder link and API key in Settings first."
            return
        }
        player.setMediaItems(target.map { it.toMediaItem(apiKey) }, index, 0L)
        player.prepare()
        player.play()
    }

    fun playPlaylist(player: Player?, tracks: List<Track>) {
        val first = tracks.firstOrNull() ?: return
        playTrack(player, first, tracks)
    }

    fun setTheme(value: String) = viewModelScope.launch { preferences.setTheme(value) }
    fun setAutoSync(value: Boolean) = viewModelScope.launch { preferences.setAutoSync(value) }
    fun saveDriveConnection(folderLink: String, apiKey: String) = viewModelScope.launch {
        if (apiKey.isBlank()) {
            _message.value = "Paste your Google Drive API key."
            return@launch
        }
        val folder = try {
            DriveFolderLinkParser.parse(folderLink)
        } catch (error: IllegalArgumentException) {
            _message.value = error.message ?: "Enter a valid Google Drive folder link."
            return@launch
        }
        preferences.setDriveConnection(folderLink, folder.id, folder.resourceKey, apiKey)
        _message.value = "Drive folder saved. Syncing your songs…"
        syncConfiguredLibrary(folder.id, apiKey.trim(), folder.resourceKey)
    }

    fun clearDriveConnection() = viewModelScope.launch {
        preferences.clearDriveConnection()
        syncScheduler.setEnabled(false)
        _message.value = "Drive folder removed. Downloaded songs are still available."
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        val clean = name.trim()
        if (clean.isBlank()) return@launch
        onCreated(playlistsRepository.create(clean))
    }

    fun renamePlaylist(id: Long, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) playlistsRepository.rename(id, name)
    }

    fun deletePlaylist(id: Long) = viewModelScope.launch { playlistsRepository.delete(id) }
    fun addToPlaylist(playlistId: Long, trackId: String) = viewModelScope.launch { playlistsRepository.addTrack(playlistId, trackId) }
    fun removeFromPlaylist(playlistId: Long, trackId: String) = viewModelScope.launch { playlistsRepository.removeTrack(playlistId, trackId) }
    fun moveInPlaylist(playlistId: Long, from: Int, to: Int) = viewModelScope.launch { playlistsRepository.move(playlistId, from, to) }
    fun playlistTracks(id: Long) = playlistsRepository.tracks(id)
    suspend fun playlistTrackIds(id: Long): List<String> = playlistsRepository.trackIds(id)

    fun download(track: Track) {
        val uniqueName = "sonora_download_${track.id}"
        val request = OneTimeWorkRequestBuilder<DownloadSongWorker>()
            .setInputData(workDataOf(DownloadSongWorker.KEY_TRACK_ID to track.id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(uniqueName)
            .build()
        workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, request)
        _message.value = "Download queued for ${track.title}"
    }

    fun downloadProgress(trackId: String) = workManager.getWorkInfosForUniqueWorkFlow("sonora_download_$trackId").map { infos ->
        val info = infos.firstOrNull() ?: return@map null
        when (info.state) {
            androidx.work.WorkInfo.State.SUCCEEDED -> 100
            androidx.work.WorkInfo.State.RUNNING -> info.progress.getInt(DownloadSongWorker.KEY_PROGRESS, 0)
            androidx.work.WorkInfo.State.ENQUEUED, androidx.work.WorkInfo.State.BLOCKED -> 0
            else -> null
        }
    }

    fun deleteDownload(track: Track) = viewModelScope.launch {
        val path = track.localPath ?: return@launch
        val root = File(context.filesDir, "music").canonicalFile
        val file = File(path).canonicalFile
        if (!file.toPath().startsWith(root.toPath())) {
            _message.value = "This cached file is outside Sonora storage and was left untouched."
            return@launch
        }
        if (file.exists() && !file.delete()) {
            _message.value = "Could not remove this download. Please try again."
            return@launch
        }
        music.clearDownload(track.id)
        _message.value = "Download removed"
    }
}
