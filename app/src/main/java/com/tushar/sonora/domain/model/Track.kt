package com.tushar.sonora.domain.model

data class Track(
    val id: String,
    val fileName: String,
    val title: String,
    val artist: String,
    val album: String,
    val mimeType: String,
    val modifiedTime: String,
    val sizeBytes: Long,
    val thumbnailLink: String?,
    val durationMs: Long,
    val isFavorite: Boolean,
    val playCount: Int,
    val lastPlayedAt: Long?,
    val localPath: String?,
    val isRemoteAvailable: Boolean,
    val resourceKey: String? = null,
    val webContentLink: String? = null,
) {
    val fileFormat: String get() = fileName.substringAfterLast('.', "").uppercase()
    val isDownloaded: Boolean get() = !localPath.isNullOrBlank()
}
