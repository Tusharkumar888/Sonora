package com.tushar.sonora.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.presentation.components.TrackArtwork
import kotlinx.coroutines.delay

data class PlaybackSnapshot(
    val mediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val currentIndex: Int = -1,
    val itemCount: Int = 0,
    val volume: Float = 1f,
    val speed: Float = 1f,
    val error: String? = null,
)

@Composable
fun rememberPlaybackSnapshot(player: Player?): PlaybackSnapshot {
    var state by remember(player) { mutableStateOf(readSnapshot(player)) }
    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) { state = readSnapshot(player) }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player) {
        while (player != null) {
            state = readSnapshot(player)
            delay(400)
        }
    }
    return state
}

private fun readSnapshot(player: Player?): PlaybackSnapshot {
    if (player == null) return PlaybackSnapshot()
    return PlaybackSnapshot(
        mediaItem = player.currentMediaItem,
        isPlaying = player.isPlaying,
        isBuffering = player.playbackState == Player.STATE_BUFFERING,
        positionMs = player.currentPosition,
        durationMs = player.duration.takeIf { it > 0L } ?: 0L,
        shuffleEnabled = player.shuffleModeEnabled,
        repeatMode = player.repeatMode,
        currentIndex = player.currentMediaItemIndex,
        itemCount = player.mediaItemCount,
        volume = player.volume,
        speed = player.playbackParameters.speed,
        error = player.playerError?.let { playbackErrorMessage(it) },
    )
}

private fun playbackErrorMessage(error: PlaybackException): String = when {
    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS && error.message?.contains("401") == true ->
        "Google Drive authorization expired. Reconnect in Settings and retry playback."
    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED || error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Network interrupted. Check your connection and try playback again."
    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
        "This audio format is not supported on this device."
    else -> "Playback could not start. Check the file and your connection, then retry."
}

@Composable
fun MiniPlayer(player: Player?, track: Track?, snapshot: PlaybackSnapshot, onExpand: () -> Unit) {
    if (snapshot.mediaItem == null) return
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = onExpand).padding(start = 9.dp, top = 8.dp, bottom = 8.dp, end = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                TrackArtwork(track, 42.dp, Modifier.size(42.dp))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(snapshot.mediaItem?.mediaMetadata?.title?.toString() ?: track?.title ?: "Unknown track", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(snapshot.mediaItem?.mediaMetadata?.artist?.toString() ?: track?.artist ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { if (player?.hasNextMediaItem() == true) player.seekToNextMediaItem() }, enabled = player?.hasNextMediaItem() == true, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.SkipNext, "Next track", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { if (snapshot.isPlaying) player?.pause() else player?.play() }, modifier = Modifier.size(40.dp)) {
                    Icon(if (snapshot.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (snapshot.isPlaying) "Pause" else "Play", tint = MaterialTheme.colorScheme.primary)
                }
            }
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.surface)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(if (snapshot.durationMs > 0) (snapshot.positionMs.toFloat() / snapshot.durationMs).coerceIn(0f, 1f) else 0f).height(2.dp).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}
