package com.tushar.sonora.service.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tushar.sonora.data.local.AppPreferences
import com.tushar.sonora.domain.repository.DriveRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DriveSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val drive: DriveRepository,
    private val preferences: AppPreferences,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        val settings = preferences.settings.first()
        val folderId = settings.musicFolderId ?: return Result.failure()
        val apiKey = settings.driveApiKey ?: return Result.failure()
        drive.sync(folderId, apiKey, settings.musicFolderResourceKey)
        Result.success()
    } catch (error: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
