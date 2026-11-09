package com.tushar.sonora.service.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.tushar.sonora.data.remote.drive.DriveMediaUrl
import com.tushar.sonora.domain.model.Track
import java.io.File

fun Track.toMediaItem(apiKey: String): MediaItem {
    val audioUri = localPath?.let { Uri.fromFile(File(it)) }
        ?: webContentLink?.let(Uri::parse)
        ?: Uri.parse(DriveMediaUrl.streamUrl(id, apiKey, resourceKey))
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(thumbnailLink?.let(Uri::parse))
        .build()
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(audioUri)
        .setMediaMetadata(metadata)
        .build()
}
