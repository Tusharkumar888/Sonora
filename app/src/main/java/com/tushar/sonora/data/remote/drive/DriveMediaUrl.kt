package com.tushar.sonora.data.remote.drive

import android.net.Uri

object DriveMediaUrl {
    private const val API_KEY_PARAMETER = "sonora_api_key"
    private const val RESOURCE_KEY_PARAMETER = "sonora_resource_key"

    fun streamUrl(fileId: String, apiKey: String, resourceKey: String?): String = Uri.Builder()
        .scheme("https")
        .authority("www.googleapis.com")
        .appendPath("drive")
        .appendPath("v3")
        .appendPath("files")
        .appendPath(fileId)
        .appendQueryParameter("alt", "media")
        .appendQueryParameter(API_KEY_PARAMETER, apiKey)
        .apply { if (!resourceKey.isNullOrBlank()) appendQueryParameter(RESOURCE_KEY_PARAMETER, resourceKey) }
        .build()
        .toString()
}
