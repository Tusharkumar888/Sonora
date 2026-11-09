package com.tushar.sonora.presentation.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tushar.sonora.data.local.PlaylistEntity
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.presentation.MusicViewModel
import com.tushar.sonora.presentation.components.TrackArtwork
import com.tushar.sonora.presentation.components.TrackRow
import com.tushar.sonora.presentation.components.formatDuration
import androidx.media3.common.Player

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistEntity>,
    allTracks: List<Track>,
    viewModel: MusicViewModel,
    player: Player?,
    onTrackPlay: (Track, List<Track>) -> Unit,
    openPlaylistId: Long? = null,
    onPlaylistOpened: () -> Unit = {},
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var createOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var addTracksOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PlaylistEntity?>(null) }
    var menuPlaylist by remember { mutableStateOf<Long?>(null) }
    val selectedPlaylist = selectedId?.let { id -> playlists.firstOrNull { it.id == id } }
    val selectedTracksFlow = remember(selectedPlaylist?.id) { selectedPlaylist?.let { viewModel.playlistTracks(it.id) } }
    val selectedTracksState = selectedTracksFlow?.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedTracks = selectedTracksState?.value ?: emptyList()

    androidx.compose.runtime.LaunchedEffect(openPlaylistId, playlists) {
        val match = playlists.firstOrNull { it.id == openPlaylistId }
        if (match != null) {
            selectedId = match.id
            onPlaylistOpened()
        }
    }

    if (selectedPlaylist == null) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Playlists", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Made for the moments you want to keep.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { name = ""; createOpen = true }, modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Outlined.Add, "Create playlist", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            if (playlists.isEmpty()) {
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(26.dp), modifier = Modifier.size(82.dp)) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.QueueMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp)) }
                    }
                    Text("Start a collection", Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Create playlists for every mood and moment.", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { name = ""; createOpen = true }, Modifier.padding(top = 18.dp)) { Text("Create your first playlist") }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playlists, key = { it.id }) { playlist ->
                        Card(onClick = { selectedId = playlist.id }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                TrackArtwork(allTracks.firstOrNull { it.id == playlist.coverTrackId }, 62.dp, Modifier.size(62.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Tap to open your collection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                androidx.compose.foundation.layout.Box {
                                    IconButton(onClick = { menuPlaylist = playlist.id }) { Icon(Icons.Outlined.MoreHoriz, "Playlist options") }
                                    DropdownMenu(expanded = menuPlaylist == playlist.id, onDismissRequest = { menuPlaylist = null }) {
                                        DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { selectedId = playlist.id; name = playlist.name; renameOpen = true; menuPlaylist = null })
                                        DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }, onClick = { pendingDelete = playlist; menuPlaylist = null })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedId = null }) { Icon(Icons.Outlined.ArrowBack, "Back to playlists") }
                Column(Modifier.weight(1f).padding(start = 6.dp)) {
                    Text(selectedPlaylist.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${selectedTracks.size} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { name = selectedPlaylist.name; renameOpen = true }) { Icon(Icons.Outlined.Edit, "Rename playlist") }
                IconButton(onClick = { addTracksOpen = true }) { Icon(Icons.Outlined.Add, "Add songs", tint = MaterialTheme.colorScheme.primary) }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable { viewModel.playPlaylist(player, selectedTracks) }) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Play playlist", Modifier.padding(start = 9.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            if (selectedTracks.isEmpty()) {
                Text("Add songs from your Drive library to build this playlist.", Modifier.padding(top = 26.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(selectedTracks, key = { it.id }) { track ->
                        val index = selectedTracks.indexOfFirst { it.id == track.id }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TrackRow(track, formatDuration(track.durationMs), { onTrackPlay(track, selectedTracks) }, { viewModel.toggleFavorite(track) }, { viewModel.download(track) }, { addTracksOpen = true }, { viewModel.deleteDownload(track) }, Modifier.weight(1f))
                            Column {
                                IconButton(onClick = { viewModel.moveInPlaylist(selectedPlaylist.id, index, index - 1) }, enabled = index > 0, modifier = Modifier.size(34.dp)) { Icon(Icons.Outlined.ArrowUpward, "Move up", modifier = Modifier.size(17.dp)) }
                                IconButton(onClick = { viewModel.moveInPlaylist(selectedPlaylist.id, index, index + 1) }, enabled = index < selectedTracks.lastIndex, modifier = Modifier.size(34.dp)) { Icon(Icons.Outlined.ArrowDownward, "Move down", modifier = Modifier.size(17.dp)) }
                                IconButton(onClick = { viewModel.removeFromPlaylist(selectedPlaylist.id, track.id) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Outlined.RemoveCircleOutline, "Remove from playlist", modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (createOpen) NameDialog("New playlist", "Give your collection a name", name, { name = it }, "Create", {
        viewModel.createPlaylist(name) { newId -> selectedId = newId }
        createOpen = false
    }, { createOpen = false })

    if (renameOpen && selectedPlaylist != null) NameDialog("Rename playlist", "Choose a new name", name, { name = it }, "Save", {
        viewModel.renamePlaylist(selectedPlaylist.id, name)
        renameOpen = false
    }, { renameOpen = false })

    if (addTracksOpen && selectedPlaylist != null) AddTracksDialog(
        allTracks = allTracks,
        existing = selectedTracks.map { it.id }.toSet(),
        onAdd = { track -> viewModel.addToPlaylist(selectedPlaylist.id, track.id) },
        onDismiss = { addTracksOpen = false },
    )

    pendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete playlist?") },
            text = { Text("${playlist.name} and its saved order will be removed. Your music files will stay in Drive.") },
            confirmButton = { TextButton(onClick = { viewModel.deletePlaylist(playlist.id); pendingDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NameDialog(title: String, prompt: String, value: String, onChange: (String) -> Unit, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = onChange, label = { Text(prompt) }, singleLine = true) },
        confirmButton = { TextButton(onClick = onConfirm, enabled = value.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddTracksDialog(allTracks: List<Track>, existing: Set<String>, onAdd: (Track) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add songs") },
        text = {
            if (allTracks.isEmpty()) Text("Sync your Google Drive library first to add music.")
            else LazyColumn(Modifier.size(width = 330.dp, height = 420.dp)) {
                items(allTracks.filterNot { it.id in existing }, key = { it.id }) { track ->
                    Row(Modifier.fillMaxWidth().clickable { onAdd(track) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TrackArtwork(track, 42.dp, Modifier.size(42.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
