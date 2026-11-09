package com.tushar.sonora.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tushar.sonora.domain.model.Track

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val title: String,
    val artist: String = "Unknown artist",
    val album: String = "Unknown album",
    val mimeType: String,
    val modifiedTime: String,
    val sizeBytes: Long,
    val thumbnailLink: String? = null,
    val resourceKey: String? = null,
    val webContentLink: String? = null,
    val durationMs: Long = 0,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val localPath: String? = null,
    val isRemoteAvailable: Boolean = true,
    val lastSeenAt: Long = System.currentTimeMillis(),
)

fun SongEntity.toTrack() = Track(
    id = id,
    fileName = fileName,
    title = title,
    artist = artist,
    album = album,
    mimeType = mimeType,
    modifiedTime = modifiedTime,
    sizeBytes = sizeBytes,
    thumbnailLink = thumbnailLink,
    resourceKey = resourceKey,
    webContentLink = webContentLink,
    durationMs = durationMs,
    isFavorite = isFavorite,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
    localPath = localPath,
    isRemoteAvailable = isRemoteAvailable,
)
