package com.tushar.sonora.data.remote.drive

import com.google.gson.annotations.SerializedName

data class DriveFileListDto(
    @SerializedName("files") val files: List<DriveFileDto> = emptyList(),
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
)

data class DriveFileDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("modifiedTime") val modifiedTime: String? = null,
    @SerializedName("parents") val parents: List<String> = emptyList(),
    @SerializedName("thumbnailLink") val thumbnailLink: String? = null,
    @SerializedName("resourceKey") val resourceKey: String? = null,
    @SerializedName("webContentLink") val webContentLink: String? = null,
) {
    val isFolder: Boolean get() = mimeType == FOLDER_MIME

    companion object {
        const val FOLDER_MIME = "application/vnd.google-apps.folder"
        val supportedExtensions = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "aiff", "aif", "wma", "amr")
        fun supportsAudio(name: String, mimeType: String): Boolean =
            mimeType.startsWith("audio/", ignoreCase = true) || name.substringAfterLast('.', "").lowercase() in supportedExtensions

        fun displayTitle(name: String): String {
            val extension = name.substringAfterLast('.', "").lowercase()
            return if (extension in supportedExtensions) {
                name.substringBeforeLast('.').ifBlank { name }
            } else {
                name
            }
        }
    }
}
