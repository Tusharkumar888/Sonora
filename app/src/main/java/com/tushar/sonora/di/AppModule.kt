package com.tushar.sonora.di

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.tushar.sonora.data.local.MIGRATION_1_2
import com.tushar.sonora.data.local.MusicDatabase
import com.tushar.sonora.data.local.PlaylistDao
import com.tushar.sonora.data.local.SongDao
import com.tushar.sonora.data.remote.drive.DriveApi
import com.tushar.sonora.data.repository.DriveRepositoryImpl
import com.tushar.sonora.data.repository.MusicRepositoryImpl
import com.tushar.sonora.data.repository.PlaylistRepositoryImpl
import com.tushar.sonora.domain.repository.DriveRepository
import com.tushar.sonora.domain.repository.MusicRepository
import com.tushar.sonora.domain.repository.PlaylistRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds @Singleton abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository
    @Binds @Singleton abstract fun bindDriveRepository(impl: DriveRepositoryImpl): DriveRepository
    @Binds @Singleton abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): MusicDatabase =
        Room.databaseBuilder(context, MusicDatabase::class.java, "sonora.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun songDao(database: MusicDatabase): SongDao = database.songDao()
    @Provides fun playlistDao(database: MusicDatabase): PlaylistDao = database.playlistDao()

    @Provides @Singleton
    fun okHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val androidCert = context.packageSha1()
        return OkHttpClient.Builder()
        .addNetworkInterceptor { chain ->
            val original = chain.request()
            val isDriveApi = original.url.host == DRIVE_API_HOST && original.url.encodedPath.startsWith("/drive/v3/")
            val apiKey = original.url.queryParameter(APP_API_KEY_PARAMETER)
            val resourceKey = original.url.queryParameter(APP_RESOURCE_KEY_PARAMETER)
            val fileId = original.url.pathSegments.lastOrNull()
            val cleanUrl = original.url.newBuilder()
                .removeAllQueryParameters(APP_API_KEY_PARAMETER)
                .removeAllQueryParameters(APP_RESOURCE_KEY_PARAMETER)
                .build()
            val request = original.newBuilder().url(cleanUrl).apply {
                if (isDriveApi) {
                    header("X-Android-Package", context.packageName)
                    header("X-Android-Cert", androidCert)
                }
                if (isDriveApi && !apiKey.isNullOrBlank()) header("x-goog-api-key", apiKey)
                if (isDriveApi && !resourceKey.isNullOrBlank() && !fileId.isNullOrBlank()) {
                    header("X-Goog-Drive-Resource-Keys", "$fileId/$resourceKey")
                }
                if (!isDriveApi) {
                    removeHeader("x-goog-api-key")
                    removeHeader("X-Goog-Drive-Resource-Keys")
                    removeHeader("X-Android-Package")
                    removeHeader("X-Android-Cert")
                }
            }.build()
            chain.proceed(request)
        }
        .retryOnConnectionFailure(true)
        .build()
    }

    @Provides @Singleton fun gson(): Gson = GsonBuilder().create()

    @Provides @Singleton
    fun driveApi(client: OkHttpClient, gson: Gson): DriveApi = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(DriveApi::class.java)

    private const val DRIVE_API_HOST = "www.googleapis.com"
    private const val APP_API_KEY_PARAMETER = "sonora_api_key"
    private const val APP_RESOURCE_KEY_PARAMETER = "sonora_resource_key"
}

@Suppress("DEPRECATION")
private fun Context.packageSha1(): String {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            .signingInfo?.apkContentsSigners
    } else {
        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
    }
    val signature = signatures?.firstOrNull()?.toByteArray()
        ?: error("Could not read the installed app signing certificate.")
    return java.security.MessageDigest.getInstance("SHA-1")
        .digest(signature)
        .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
}
