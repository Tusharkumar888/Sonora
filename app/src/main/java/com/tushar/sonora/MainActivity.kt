package com.tushar.sonora

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.tushar.sonora.presentation.MusicViewModel
import com.tushar.sonora.presentation.SonoraRoot
import com.tushar.sonora.presentation.theme.SonoraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : androidx.activity.ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()
    private val controllerState = mutableStateOf<MediaController?>(null)
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val settings by musicViewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.theme) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            SideEffect {
                window.statusBarColor = ComposeColor.Transparent.toArgb()
                window.navigationBarColor = (if (darkTheme) ComposeColor(0xFF090B0E) else ComposeColor(0xFFF5F6F1)).toArgb()
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            SonoraTheme(settings.theme) {
                SonoraRoot(player = controllerState.value, viewModel = musicViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connectPlaybackController()
    }

    override fun onStop() {
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        controllerState.value = null
        super.onStop()
    }

    private fun connectPlaybackController() {
        val token = SessionToken(this, ComponentName(this, com.tushar.sonora.service.playback.PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }.onSuccess { controllerState.value = it }
        }, mainExecutor)
    }
}
