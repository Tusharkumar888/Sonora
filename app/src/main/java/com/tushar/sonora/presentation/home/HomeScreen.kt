package com.tushar.sonora.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.data.local.PlaylistEntity
import com.tushar.sonora.presentation.components.SectionTitle
import com.tushar.sonora.presentation.components.TrackArtwork
import com.tushar.sonora.presentation.components.TrackRow
import com.tushar.sonora.presentation.components.formatDuration

@Composable
fun HomeScreen(
    tracks: List<Track>,
    recentlyPlayed: List<Track>,
    frequentlyPlayed: List<Track>,
    favorites: List<Track>,
    playlists: List<PlaylistEntity>,
    folderConfigured: Boolean,
    syncing: Boolean,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onSearch: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenPlaylists: () -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onFavorite: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onRemoveDownload: (Track) -> Unit,
) {
    val greeting = when (java.time.LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    LazyColumn(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Your music, in its own rhythm.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSearch, modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search music")
                }
            }
        }
        item {
            DriveStatusCard(folderConfigured, syncing, onConnect, onSync)
            Spacer(Modifier.height(18.dp))
        }
        item {
            val feature = recentlyPlayed.firstOrNull() ?: tracks.firstOrNull()
            if (feature != null) {
                FeaturedTrackCard(feature, onPlay = { onPlay(feature, tracks.ifEmpty { listOf(feature) }) })
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Text("Your library starts here", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Add a public Drive folder link and Sonora will find the songs inside it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = if (!folderConfigured) onConnect else onSync) {
                            Text(if (!folderConfigured) "Set up Drive folder" else "Sync your library")
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (recentlyPlayed.isNotEmpty()) {
            item { SectionTitle("Pick up where you left off", action = "See all", onAction = onOpenLibrary) }
            items(recentlyPlayed.take(5), key = { "recent-${it.id}" }) { track ->
                TrackRow(track, formatDuration(track.durationMs), { onPlay(track, tracks) }, { onFavorite(track) }, { onDownload(track) }, { onAddToPlaylist(track) }, { onRemoveDownload(track) })
            }
        }
        if (frequentlyPlayed.isNotEmpty()) {
            item { SectionTitle("In heavy rotation", "Your most played music") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(frequentlyPlayed.take(10), key = { "often-${it.id}" }) { track ->
                        Column(Modifier.width(144.dp).clickable { onPlay(track, tracks) }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TrackArtwork(track, 144.dp, Modifier.size(144.dp))
                            Text(track.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (favorites.isNotEmpty()) {
            item { SectionTitle("Your favorites", "Saved for a quick return") }
            items(favorites.take(4), key = { "favorite-${it.id}" }) { track ->
                TrackRow(track, formatDuration(track.durationMs), { onPlay(track, tracks) }, { onFavorite(track) }, { onDownload(track) }, { onAddToPlaylist(track) }, { onRemoveDownload(track) })
            }
        }
        if (playlists.isNotEmpty()) {
            item { SectionTitle("Made by you", "Your playlists", action = "See all", onAction = onOpenPlaylists) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(playlists.take(8), key = { "home-playlist-${it.id}" }) { playlist ->
                        Surface(
                            onClick = { onOpenPlaylist(playlist.id) },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.width(174.dp),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.LibraryMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                                Text(playlist.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Your collection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("Recently added", "${tracks.size} ${if (tracks.size == 1) "track" else "tracks"} in your cloud library", action = "Browse", onAction = onOpenLibrary)
        }
        if (tracks.isEmpty()) {
            item { Text("New music will appear here after your first sync.", Modifier.padding(bottom = 28.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(tracks.sortedByDescending { it.modifiedTime }.take(4), key = { "added-${it.id}" }) { track ->
                TrackRow(track, formatDuration(track.durationMs), { onPlay(track, tracks) }, { onFavorite(track) }, { onDownload(track) }, { onAddToPlaylist(track) }, { onRemoveDownload(track) })
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun DriveStatusCard(folderConfigured: Boolean, syncing: Boolean, onConnect: () -> Unit, onSync: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (folderConfigured) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff, null, tint = if (folderConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f).padding(horizontal = 11.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(if (folderConfigured) "Public Drive folder ready" else "No Drive folder configured", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(if (folderConfigured) "Songs stream directly from your folder" else "Add a folder link in Settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (syncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else if (folderConfigured) IconButton(onClick = onSync, modifier = Modifier.size(38.dp)) { Icon(Icons.Outlined.Sync, "Sync now", tint = MaterialTheme.colorScheme.primary) }
            else OutlinedButton(onClick = onConnect, shape = RoundedCornerShape(24.dp)) { Text("Set up") }
        }
    }
}

@Composable
private fun FeaturedTrackCard(track: Track, onPlay: () -> Unit) {
    Card(
        onClick = onPlay,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF243E36), Color(0xFF172127), Color(0xFF202133)))).padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("YOUR SOUNDTRACK", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB9F66A), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrackArtwork(track, 74.dp, Modifier.size(74.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(track.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(onClick = onPlay, color = Color(0xFFB9F66A), shape = RoundedCornerShape(50), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${track.title}", tint = Color(0xFF17200C), modifier = Modifier.size(28.dp)) }
                    }
                }
                Text("Continue listening", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.62f))
            }
        }
    }
}
