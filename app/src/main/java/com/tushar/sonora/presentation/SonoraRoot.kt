package com.tushar.sonora.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.tushar.sonora.data.local.PlaylistEntity
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.presentation.home.HomeScreen
import com.tushar.sonora.presentation.library.LibraryScreen
import com.tushar.sonora.presentation.player.MiniPlayer
import com.tushar.sonora.presentation.player.NowPlayingScreen
import com.tushar.sonora.presentation.player.rememberPlaybackSnapshot
import com.tushar.sonora.presentation.playlist.PlaylistsScreen
import com.tushar.sonora.presentation.search.SearchScreen
import com.tushar.sonora.presentation.settings.SettingsScreen

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    LIBRARY("Library", Icons.Outlined.LibraryMusic),
    PLAYLISTS("Playlists", Icons.Outlined.QueueMusic),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

@Composable
fun SonoraRoot(
    player: Player?,
    viewModel: MusicViewModel,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val frequentlyPlayed by viewModel.frequentlyPlayed.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val downloadedBytes by viewModel.downloadedBytes.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snapshot = rememberPlaybackSnapshot(player)
    val snackState = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var trackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var playlistToOpen by remember { mutableStateOf<Long?>(null) }
    val saveableStateHolder = rememberSaveableStateHolder()
    val currentTrack = tracks.firstOrNull { it.id == snapshot.mediaItem?.mediaId }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackState.showSnackbar(message!!)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(snapshot.error) {
        if (!snapshot.error.isNullOrBlank()) snackState.showSnackbar(snapshot.error)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackState) },
        bottomBar = {
            if (!playerOpen) Column {
                MiniPlayer(player, currentTrack, snapshot) { playerOpen = true }
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    AppTab.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = tab == destination && !searchOpen,
                            onClick = { searchOpen = false; tab = destination },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            alwaysShowLabel = false,
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        Box(Modifier.fillMaxSize().padding(contentPadding)) {
            when {
                playerOpen -> NowPlayingScreen(
                    player = player,
                    snapshot = snapshot,
                    track = currentTrack,
                    queueTracks = tracks,
                    onClose = { playerOpen = false },
                    onFavorite = viewModel::toggleFavorite,
                )
                searchOpen -> SearchScreen(
                    query = query,
                    tracks = searchResults,
                    playlists = playlists,
                    onQueryChange = viewModel::setQuery,
                    onBack = { searchOpen = false; viewModel.setQuery("") },
                    onTrackPlay = { track -> viewModel.playTrack(player, track, searchResults) },
                    onPlaylist = { id -> playlistToOpen = id; searchOpen = false; tab = AppTab.PLAYLISTS },
                    onFavorite = viewModel::toggleFavorite,
                    onDownload = viewModel::download,
                    onAddToPlaylist = { trackForPlaylist = it },
                    onRemoveDownload = viewModel::deleteDownload,
                )
                else -> AnimatedContent(
                    targetState = tab,
                    label = "main navigation",
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { it / 18 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 18 })
                    },
                ) { target ->
                    saveableStateHolder.SaveableStateProvider(target.name) {
                        when (target) {
                            AppTab.HOME -> HomeScreen(
                                tracks = tracks,
                                recentlyPlayed = recentlyPlayed,
                                frequentlyPlayed = frequentlyPlayed,
                                favorites = favorites,
                                playlists = playlists,
                                folderConfigured = settings.driveConfigured,
                                syncing = syncing,
                                onConnect = { tab = AppTab.SETTINGS },
                                onSync = viewModel::syncNow,
                                onSearch = { viewModel.setQuery(""); searchOpen = true },
                                onOpenLibrary = { tab = AppTab.LIBRARY },
                                onOpenPlaylist = { id -> playlistToOpen = id; tab = AppTab.PLAYLISTS },
                                onOpenPlaylists = { tab = AppTab.PLAYLISTS },
                                onPlay = { track, queue -> viewModel.playTrack(player, track, queue); playerOpen = true },
                                onFavorite = viewModel::toggleFavorite,
                                onDownload = viewModel::download,
                                onAddToPlaylist = { trackForPlaylist = it },
                                onRemoveDownload = viewModel::deleteDownload,
                            )
                            AppTab.LIBRARY -> LibraryScreen(
                                tracks = if (query.isBlank()) tracks else searchResults,
                                query = query,
                                onQueryChange = viewModel::setQuery,
                                syncing = syncing,
                                onSync = viewModel::syncNow,
                                onPlay = { track -> viewModel.playTrack(player, track, if (query.isBlank()) tracks else searchResults); playerOpen = true },
                                onFavorite = viewModel::toggleFavorite,
                                onDownload = viewModel::download,
                                onAddToPlaylist = { trackForPlaylist = it },
                                onRemoveDownload = viewModel::deleteDownload,
                            )
                            AppTab.PLAYLISTS -> PlaylistsScreen(
                                playlists = playlists,
                                allTracks = tracks,
                                viewModel = viewModel,
                                player = player,
                                onTrackPlay = { track, queue -> viewModel.playTrack(player, track, queue); playerOpen = true },
                                openPlaylistId = playlistToOpen,
                                onPlaylistOpened = { playlistToOpen = null },
                            )
                            AppTab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                tracks = tracks,
                                downloadedBytes = downloadedBytes,
                                syncing = syncing,
                                viewModel = viewModel,
                                onSaveDriveConnection = viewModel::saveDriveConnection,
                                onClearDriveConnection = { viewModel.clearDriveConnection() },
                                onSync = viewModel::syncNow,
                            )
                        }
                    }
                }
            }
        }
    }

    trackForPlaylist?.let { track ->
        if (playlists.isEmpty()) {
            AlertDialog(
                onDismissRequest = { trackForPlaylist = null },
                title = { Text("Create a playlist first") },
                text = { Text("Playlists keep your music organized. Create one, then add ${track.title}.") },
                confirmButton = { TextButton(onClick = { trackForPlaylist = null; tab = AppTab.PLAYLISTS }) { Text("Go to playlists") } },
                dismissButton = { TextButton(onClick = { trackForPlaylist = null }) { Text("Cancel") } },
            )
        } else {
            AlertDialog(
                onDismissRequest = { trackForPlaylist = null },
                title = { Text("Add to playlist") },
                text = {
                    Column {
                        Text(track.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                        playlists.forEach { playlist ->
                            TextButton(onClick = { viewModel.addToPlaylist(playlist.id, track.id); trackForPlaylist = null }, modifier = Modifier.fillMaxWidth()) {
                                Text(playlist.name, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { trackForPlaylist = null }) { Text("Done") } },
            )
        }
    }
}
