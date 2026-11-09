package com.tushar.sonora.service.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tushar.sonora.R
import com.tushar.sonora.data.local.AppPreferences
import com.tushar.sonora.data.remote.drive.DriveMediaUrl
import com.tushar.sonora.domain.repository.MusicRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers

@HiltWorker
class DownloadSongWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val music: MusicRepository,
    private val preferences: AppPreferences,
    private val http: OkHttpClient,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(KEY_TRACK_ID) ?: return@withContext Result.failure()
        val track = music.track(id) ?: return@withContext Result.failure()
        if (track.isDownloaded && File(track.localPath!!).isFile) return@withContext Result.success()
        try {
            val apiKey = preferences.settings.first().driveApiKey.orEmpty()
            val downloadUrl = track.webContentLink ?: if (apiKey.isNotBlank()) {
                DriveMediaUrl.streamUrl(id, apiKey, track.resourceKey)
            } else {
                throw IOException("Add a Google Drive API key in Settings first.")
            }
            setForeground(createForeground("Preparing ${track.title}", 0))
            val cacheDir = File(applicationContext.filesDir, "music").apply { mkdirs() }
            val safeName = id.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val destination = File(cacheDir, "$safeName.audio")
            val partial = File(cacheDir, "$safeName.audio.part")
            val request = Request.Builder()
                .url(downloadUrl)
                .get()
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val message = when (response.code) {
                        401, 403 -> "Drive denied access. Check that the folder and song are shared as Anyone with the link and that your API key is valid."
                        404 -> "This song is no longer available in Google Drive."
                        429 -> "Google Drive is busy. Try the download again in a moment."
                        else -> "Download failed (HTTP ${response.code})."
                    }
                    throw IOException(message)
                }
                val body = response.body ?: throw IOException("Google Drive returned an empty audio file.")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    partial.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var copied = 0L
                        var reported = -5
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            copied += count
                            if (total > 0) {
                                val percent = ((copied * 100) / total).toInt().coerceIn(0, 99)
                                if (percent >= reported + 5) {
                                    reported = percent
                                    setProgress(workDataOf(KEY_PROGRESS to percent))
                                    setForeground(createForeground("Downloading ${track.title}", percent))
                                }
                            }
                        }
                    }
                }
            }
            if (!partial.renameTo(destination)) {
                partial.copyTo(destination, overwrite = true)
                partial.delete()
            }
            val metadata = readMetadata(destination, track.title)
            music.setDownloaded(id, destination.absolutePath, metadata.duration, metadata.title, metadata.artist, metadata.album)
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            if (runAttemptCount < 3) Result.retry()
            else Result.failure(workDataOf(KEY_ERROR to (error.message ?: "Download failed. Please retry.")))
        } catch (error: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (error.message ?: "Download failed. Please retry.")))
        }
    }

    private fun createForeground(title: String, progress: Int): ForegroundInfo {
        val channelId = "sonora_downloads"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Music downloads", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_sonora)
            .setContentTitle("Sonora")
            .setContentText(title)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id.hashCode(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else ForegroundInfo(id.hashCode(), notification)
    }

    private fun readMetadata(file: File, fallbackTitle: String): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            AudioMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf(String::isNotBlank) ?: fallbackTitle,
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf(String::isNotBlank) ?: "Unknown artist",
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf(String::isNotBlank) ?: "Unknown album",
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
            )
        } finally {
            retriever.release()
        }
    }

    private data class AudioMetadata(val title: String, val artist: String, val album: String, val duration: Long)

    companion object {
        const val KEY_TRACK_ID = "track_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
    }
}

