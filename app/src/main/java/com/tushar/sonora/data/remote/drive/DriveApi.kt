package com.tushar.sonora.data.remote.drive

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface DriveApi {
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("x-goog-api-key") apiKey: String,
        @Header("X-Goog-Drive-Resource-Keys") resourceKeys: String? = null,
        @Query("q") query: String,
        @Query("pageToken") pageToken: String? = null,
        @Query("pageSize") pageSize: Int = 1000,
        @Query("orderBy") orderBy: String = "name_natural",
        @Query("fields") fields: String = "nextPageToken,files(id,name,mimeType,size,modifiedTime,parents,thumbnailLink,resourceKey,webContentLink)",
        @Query("spaces") spaces: String = "drive",
        @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
        @Query("includeItemsFromAllDrives") includeItemsFromAllDrives: Boolean = true,
    ): DriveFileListDto
}
