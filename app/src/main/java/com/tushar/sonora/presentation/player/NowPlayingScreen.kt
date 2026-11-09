package com.tushar.sonora.presentation.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.presentation.components.TrackArtwork
import com.tushar.sonora.presentation.components.formatDuration
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NowPlayingScreen(
    player: Player?,
    snapshot: PlaybackSnapshot,
    track: Track?,
    queueTracks: List<Track>,
    onClose: () -> Unit,
    onFavorite: (Track) -> Unit,
) {
    var queueOpen by remember { mutableStateOf(false) }
    var speedOpen by remember { mutableStateOf(false) }
    var scrubPosition by remember(snapshot.positionMs) { mutableFloatStateOf(snapshot.positionMs.toFloat()) }
    var volume by remember(snapshot.volume) { mutableFloatStateOf(snapshot.volume) }
    val title = snapshot.mediaItem?.mediaMetadata?.title?.toString() ?: track?.title ?: "Choose a song"
    val artist = snapshot.mediaItem?.mediaMetadata?.artist?.toString() ?: track?.artist ?: "Your Drive library is ready when you are"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF18201A), MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close player") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("From your library", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { queueOpen = true }) { Icon(Icons.Outlined.QueueMusic, "Open queue") }
            }
            Spacer(Modifier.weight(0.42f))
            Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f), shape = RoundedCornerShape(30.dp), modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(13.dp)) { }
                TrackArtwork(track, 1.dp, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)))
                if (snapshot.isBuffering) CircularProgressIndicator(Modifier.align(Alignment.BottomEnd).padding(18.dp).size(28.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.weight(0.4f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    AnimatedContent(title, label = "song title") { value -> Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Text(artist, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val album = snapshot.mediaItem?.mediaMetadata?.albumTitle?.toString() ?: track?.album
                    if (!album.isNullOrBlank() && album != "Unknown album") Text(album, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f))
                }
                IconButton(onClick = { track?.let(onFavorite) }, enabled = track != null, modifier = Modifier.size(50.dp)) {
                    Icon(if (track?.isFavorite == true) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, if (track?.isFavorite == true) "Remove favorite" else "Add favorite", tint = if (track?.isFavorite == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(25.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth()) {
                Slider(
                    value = scrubPosition.coerceIn(0f, snapshot.durationMs.coerceAtLeast(1L).toFloat()),
                    onValueChange = { scrubPosition = it },
                    onValueChangeFinished = { player?.seekTo(scrubPosition.toLong()) },
                    valueRange = 0f..snapshot.durationMs.coerceAtLeast(1L).toFloat(),
                    enabled = snapshot.durationMs > 0,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(scrubPosition.toLong()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDuration(snapshot.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { player?.shuffleModeEnabled = !snapshot.shuffleEnabled }) {
                    Icon(Icons.Outlined.Shuffle, "Shuffle", tint = if (snapshot.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { player?.seekToPreviousMediaItem() }, enabled = player?.hasPreviousMediaItem() == true) { Icon(Icons.Outlined.SkipPrevious, "Previous track", modifier = Modifier.size(30.dp)) }
                Surface(onClick = {
                    if (snapshot.isPlaying) player?.pause()
                    else player?.let { if (it.playbackState == Player.STATE_IDLE || it.playbackState == Player.STATE_ENDED) it.prepare(); it.play() }
                }, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50), modifier = Modifier.size(68.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (snapshot.isPlaying) "Pause" else "Play", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
                    }
                }
                IconButton(onClick = { player?.seekToNextMediaItem() }, enabled = player?.hasNextMediaItem() == true) { Icon(Icons.Outlined.SkipNext, "Next track", modifier = Modifier.size(30.dp)) }
                Box {
                    IconButton(onClick = {
                        player?.repeatMode = when (snapshot.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                    }) {
                        Icon(if (snapshot.repeatMode == Player.REPEAT_MODE_ONE) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat, "Repeat mode", tint = if (snapshot.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.GraphicEq, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Slider(value = volume, onValueChange = { volume = it; player?.volume = it }, modifier = Modifier.weight(1f).padding(horizontal = 10.dp), valueRange = 0f..1f)
                Box {
                    TextButton(onClick = { speedOpen = true }) { Text("${"%.2f".format(snapshot.speed).trimEnd('0').trimEnd('.')}×", color = MaterialTheme.colorScheme.primary) }
                    DropdownMenu(expanded = speedOpen, onDismissRequest = { speedOpen = false }) {
                        listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(text = { Text("${speed}×") }, onClick = { player?.setPlaybackParameters(PlaybackParameters(speed)); speedOpen = false })
                        }
                    }
                }
            }
            Spacer(Modifier.weight(0.35f))
        }
    }

    if (queueOpen) ModalBottomSheet(onDismissRequest = { queueOpen = false }, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Up next", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${snapshot.itemCount} tracks in queue", Modifier.padding(top = 3.dp, bottom = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.lazy.LazyColumn(Modifier.height(430.dp)) {
                items(snapshot.itemCount, key = { index -> player?.getMediaItemAt(index)?.mediaId ?: index }) { index ->
                    val item = player?.getMediaItemAt(index)
                    val queuedTrack = queueTracks.firstOrNull { it.id == item?.mediaId }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (index == snapshot.currentIndex) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent).clickable { player?.seekTo(index, 0L); player?.play(); scope.launch { sheetState.hide(); queueOpen = false } }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TrackArtwork(queuedTrack, 42.dp, Modifier.size(42.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(item?.mediaMetadata?.title?.toString() ?: queuedTrack?.title ?: "Track ${index + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = if (index == snapshot.currentIndex) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item?.mediaMetadata?.artist?.toString() ?: queuedTrack?.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (index == snapshot.currentIndex) Icon(Icons.Outlined.GraphicEq, "Now playing", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
