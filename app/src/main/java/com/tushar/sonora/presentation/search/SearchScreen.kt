package com.tushar.sonora.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tushar.sonora.data.local.PlaylistEntity
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.presentation.components.SectionTitle
import com.tushar.sonora.presentation.components.TrackArtwork
import com.tushar.sonora.presentation.components.TrackRow
import com.tushar.sonora.presentation.components.formatDuration

@Composable
fun SearchScreen(
    query: String,
    tracks: List<Track>,
    playlists: List<PlaylistEntity>,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onTrackPlay: (Track) -> Unit,
    onPlaylist: (Long) -> Unit,
    onFavorite: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onRemoveDownload: (Track) -> Unit,
) {
    val playlistMatches = if (query.isBlank()) emptyList() else playlists.filter { it.name.contains(query, ignoreCase = true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Close search") }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Songs, artists, albums, playlists") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
            )
        }
        if (query.isBlank()) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                Text("Find something to play", Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Search songs, artists, albums and playlists.", Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                if (tracks.isNotEmpty()) {
                    item { SectionTitle("Music", "Songs, artists and albums") }
                    items(tracks, key = { "search-${it.id}" }) { track ->
                        TrackRow(track, formatDuration(track.durationMs), { onTrackPlay(track) }, { onFavorite(track) }, { onDownload(track) }, { onAddToPlaylist(track) }, { onRemoveDownload(track) })
                    }
                }
                if (playlistMatches.isNotEmpty()) {
                    item { SectionTitle("Playlists") }
                    items(playlistMatches, key = { "playlist-${it.id}" }) { playlist ->
                        Row(Modifier.fillMaxWidth().clickable { onPlaylist(playlist.id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(13.dp), modifier = Modifier.size(48.dp)) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.QueueMusic, null, tint = MaterialTheme.colorScheme.primary) }
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(playlist.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Playlist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (tracks.isEmpty() && playlistMatches.isEmpty()) {
                    item {
                        Spacer(Modifier.padding(top = 24.dp))
                        Text("No results for “$query”", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Try a different search or sync your Drive folder.", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { Spacer(Modifier.padding(bottom = 24.dp)) }
            }
        }
    }
}
