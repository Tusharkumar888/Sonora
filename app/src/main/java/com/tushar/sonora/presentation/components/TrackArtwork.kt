package com.tushar.sonora.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tushar.sonora.domain.model.Track

private val covers = listOf(
    Color(0xFF234D49) to Color(0xFF162E37),
    Color(0xFF6E3D37) to Color(0xFF35213F),
    Color(0xFF65522A) to Color(0xFF30283B),
    Color(0xFF364C70) to Color(0xFF24243E),
)

@Composable
fun TrackArtwork(track: Track?, size: Dp, modifier: Modifier = Modifier) {
    val pair = covers[(track?.id?.hashCode()?.and(Int.MAX_VALUE) ?: 0) % covers.size]
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(pair.first, pair.second)))
            .size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (!track?.thumbnailLink.isNullOrBlank()) {
            AsyncImage(
                model = track?.thumbnailLink,
                contentDescription = track?.let { "Artwork for ${it.album}" },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Color.White.copy(alpha = 0.76f), modifier = Modifier.fillMaxSize(0.32f))
        }
    }
}
