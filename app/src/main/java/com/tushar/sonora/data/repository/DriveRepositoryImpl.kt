package com.tushar.sonora.data.repository

import com.tushar.sonora.data.local.SongDao
import com.tushar.sonora.data.local.SongEntity
import com.tushar.sonora.data.remote.drive.DriveApi
import com.tushar.sonora.data.remote.drive.DriveFileDto
import com.tushar.sonora.domain.repository.DriveRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class DriveRepositoryImpl @Inject constructor(
    private val api: DriveApi,
    private val songDao: SongDao,
) : DriveRepository {
    override suspend fun sync(folderId: String, apiKey: String, folderResourceKey: String?): Int {
        if (folderId.isBlank()) throw IOException("Paste a Google Drive folder link in Settings.")
        if (apiKey.isBlank()) throw IOException("Add a Google Drive API key in Settings.")

        val remote = fetchFolderAudio(folderId, folderResourceKey, apiKey)
        val existing = songDao.getByIds(remote.map { it.id }).associateBy(SongEntity::id)
        val snapshot = remote.map { file ->
            val previous = existing[file.id]
            val unchanged = previous?.modifiedTime == file.modifiedTime.orEmpty()
            SongEntity(
                id = file.id,
                fileName = file.name,
                title = if (unchanged) previous.title else DriveFileDto.displayTitle(file.name),
                artist = if (unchanged) previous.artist else "Unknown artist",
                album = if (unchanged) previous.album else "Unknown album",
                mimeType = file.mimeType,
                modifiedTime = file.modifiedTime.orEmpty(),
                sizeBytes = file.size ?: previous?.sizeBytes ?: 0L,
                thumbnailLink = file.thumbnailLink,
                resourceKey = file.resourceKey ?: previous?.resourceKey,
                webContentLink = file.webContentLink ?: previous?.webContentLink,
                durationMs = if (unchanged) previous.durationMs else 0L,
                isFavorite = previous?.isFavorite ?: false,
                playCount = previous?.playCount ?: 0,
                lastPlayedAt = previous?.lastPlayedAt,
                localPath = if (unchanged) previous.localPath else null,
                isRemoteAvailable = true,
            )
        }
        songDao.applyRemoteSnapshot(snapshot)
        return snapshot.size
    }

    private suspend fun fetchFolderAudio(
        rootId: String,
        rootResourceKey: String?,
        apiKey: String,
    ): List<DriveFileDto> {
        val queue = ArrayDeque<Pair<String, String?>>().apply { add(rootId to rootResourceKey) }
        val visited = mutableSetOf<String>()
        val audio = mutableListOf<DriveFileDto>()
        while (queue.isNotEmpty()) {
            val (parentId, parentResourceKey) = queue.removeFirst()
            if (!visited.add(parentId)) continue
            val children = fetchPages(
                query = folderQuery(parentId),
                apiKey = apiKey,
                resourceKeyHeader = parentResourceKey?.let { "$parentId/$it" },
            )
            children.filter(DriveFileDto::isFolder).forEach { folder ->
                queue.addLast(folder.id to folder.resourceKey)
            }
            audio += children.filter { !it.isFolder && DriveFileDto.supportsAudio(it.name, it.mimeType) }
        }
        return audio
    }

    private suspend fun fetchPages(
        query: String,
        apiKey: String,
        resourceKeyHeader: String?,
    ): List<DriveFileDto> {
        val all = mutableListOf<DriveFileDto>()
        val seenTokens = mutableSetOf<String>()
        var pageToken: String? = null
        do {
            val page = try {
                api.listFiles(
                    apiKey = apiKey,
                    resourceKeys = resourceKeyHeader,
                    query = query,
                    pageToken = pageToken,
                )
            } catch (error: HttpException) {
                val message = when (error.code()) {
                    400 -> "That Drive folder link is not valid. Check the link and try again."
                    401, 403 -> "Drive could not read this folder. Share it as Anyone with the link and check that the API key allows the Google Drive API."
                    404 -> "This Drive folder or one of its songs is no longer available."
                    429 -> "Google Drive is receiving too many requests. Wait a moment, then retry."
                    in 500..599 -> "Google Drive is temporarily unavailable. Please retry shortly."
                    else -> "Google Drive could not complete the request (${error.code()})."
                }
                throw IOException(message, error)
            }
            all += page.files
            pageToken = page.nextPageToken?.takeIf { it.isNotBlank() }
            if (pageToken != null && !seenTokens.add(pageToken)) {
                throw IOException("Google Drive returned a repeated page token. Try syncing again.")
            }
        } while (pageToken != null)
        return all
    }

    private fun folderQuery(parentId: String): String = "'$parentId' in parents and trashed = false"
}
