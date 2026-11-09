package com.tushar.sonora.presentation.library

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.presentation.components.TrackArtwork
import com.tushar.sonora.presentation.components.TrackRow
import com.tushar.sonora.presentation.components.formatDuration

private enum class SortMode(val title: String) { NAME("Name"), ARTIST("Artist"), RECENT("Recently added"), DURATION("Duration") }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryScreen(
    tracks: List<Track>,
    query: String,
    onQueryChange: (String) -> Unit,
    syncing: Boolean,
    onSync: () -> Unit,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onRemoveDownload: (Track) -> Unit,
) {
    var format by rememberSaveable { mutableStateOf("All formats") }
    var sort by rememberSaveable { mutableStateOf(SortMode.NAME) }
    var sortMenu by remember { mutableStateOf(false) }
    var grid by rememberSaveable { mutableStateOf(false) }
    val filtered = remember(tracks, format, sort) {
        val source = if (format == "All formats") tracks else tracks.filter { it.fileFormat.equals(format, ignoreCase = true) }
        when (sort) {
            SortMode.NAME -> source.sortedBy { it.title.lowercase() }
            SortMode.ARTIST -> source.sortedBy { it.artist.lowercase() }
            SortMode.RECENT -> source.sortedByDescending { it.modifiedTime }
            SortMode.DURATION -> source.sortedBy { it.durationMs }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${tracks.size} songs · from Google Drive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSync, enabled = !syncing) {
                if (syncing) androidx.compose.material3.CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, "Refresh library", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { grid = !grid }) {
                Icon(if (grid) Icons.Outlined.ViewList else Icons.Outlined.GridView, if (grid) "List layout" else "Grid layout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search songs, artists, albums") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
        )
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("All formats", "MP3", "M4A", "AAC", "WAV", "FLAC", "OGG", "OPUS", "AIFF", "WMA").forEach { value ->
                    FilterChip(selected = format == value, onClick = { format = value }, label = { Text(value) })
                }
            }
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Outlined.Sort, "Sort", tint = MaterialTheme.colorScheme.primary) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(text = { Text(mode.title) }, onClick = { sort = mode; sortMenu = false })
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        PullToRefreshBox(isRefreshing = syncing, onRefresh = onSync, modifier = Modifier.weight(1f)) {
            when {
                filtered.isEmpty() -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    item {
                        if (syncing && query.isBlank()) LibraryLoadingSkeleton(Modifier.fillMaxWidth())
                        else EmptyLibrary(hasQuery = query.isNotBlank())
                    }
                }
                grid -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(154.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.id }) { track ->
                        Card(onClick = { onPlay(track) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.foundation.layout.Box {
                                    TrackArtwork(track, 1.dp, Modifier.fillMaxWidth().height(144.dp))
                                    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50), modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(34.dp).clickable { onPlay(track) }) {
                                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, "Play ${track.title}", tint = MaterialTheme.colorScheme.onPrimary) }
                                    }
                                }
                                Text(track.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatDuration(track.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    items(filtered, key = { it.id }) { track ->
                        TrackRow(track, formatDuration(track.durationMs), { onPlay(track) }, { onFavorite(track) }, { onDownload(track) }, { onAddToPlaylist(track) }, { onRemoveDownload(track) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryLoadingSkeleton(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "library loading")
        .animateFloat(
            initialValue = 0.28f,
            targetValue = 0.62f,
            animationSpec = infiniteRepeatable(tween(850), repeatMode = RepeatMode.Reverse),
            label = "skeleton pulse",
        )
    Column(modifier.padding(horizontal = 4.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(5) {
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulse), shape = RoundedCornerShape(14.dp)) {
                    androidx.compose.foundation.layout.Box(Modifier.size(52.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulse), shape = RoundedCornerShape(8.dp)) {
                        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(0.65f).height(14.dp))
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulse * 0.8f), shape = RoundedCornerShape(8.dp)) {
                        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(0.38f).height(11.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(hasQuery: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp), modifier = Modifier.size(70.dp)) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        Text(if (hasQuery) "No matches" else "No music found yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            if (hasQuery) "Try a different title, artist or album." else "Connect Drive and refresh to scan for audio in your library.",
            Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
