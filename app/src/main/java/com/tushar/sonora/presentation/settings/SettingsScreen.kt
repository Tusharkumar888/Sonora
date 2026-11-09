package com.tushar.sonora.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tushar.sonora.BuildConfig
import com.tushar.sonora.data.local.AppSettings
import com.tushar.sonora.domain.model.Track
import com.tushar.sonora.presentation.MusicViewModel
import com.tushar.sonora.presentation.components.SectionTitle
import com.tushar.sonora.presentation.components.TrackArtwork
import com.tushar.sonora.presentation.components.formatBytes

@Composable
fun SettingsScreen(
    settings: AppSettings,
    tracks: List<Track>,
    downloadedBytes: Long,
    syncing: Boolean,
    viewModel: MusicViewModel,
    onSaveDriveConnection: (String, String) -> Unit,
    onClearDriveConnection: () -> Unit,
    onSync: () -> Unit,
) {
    var folderLink by rememberSaveable { mutableStateOf(settings.musicFolderLink.orEmpty()) }
    var apiKey by rememberSaveable { mutableStateOf(settings.driveApiKey.orEmpty()) }
    var showApiKey by rememberSaveable { mutableStateOf(false) }
    var confirmClearDrive by rememberSaveable { mutableStateOf(false) }
    var confirmClearDownloads by rememberSaveable { mutableStateOf(false) }
    val downloaded = tracks.filter { it.isDownloaded }

    LaunchedEffect(settings.musicFolderLink, settings.driveApiKey) {
        folderLink = settings.musicFolderLink.orEmpty()
        apiKey = settings.driveApiKey.orEmpty()
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Settings", Modifier.padding(top = 22.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Your Sonora experience, your way.", Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SectionTitle("Google Drive", "Use a public folder link and an API key")
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (settings.driveConfigured) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                            null,
                            tint = if (settings.driveConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(if (settings.driveConfigured) "Public folder ready" else "No folder configured", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (settings.driveConfigured) "Paste a new link or sync the saved folder"
                                else "No Google account sign-in is needed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = folderLink,
                        onValueChange = { folderLink = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Google Drive folder link") },
                        placeholder = { Text("https://drive.google.com/drive/folders/…") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Google Drive API key") },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(if (showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (showApiKey) "Hide API key" else "Show API key")
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                    )
                    Text(
                        "Share the folder and its songs as “Anyone with the link”. Restrict your API key to the Google Drive API and this Android app in Google Cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onSaveDriveConnection(folderLink, apiKey) },
                        enabled = !syncing,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (syncing) "Syncing…" else "Save folder and sync") }
                    if (settings.driveConfigured) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onSync, enabled = !syncing, modifier = Modifier.weight(1f)) { Text("Sync now") }
                            OutlinedButton(onClick = { confirmClearDrive = true }, enabled = !syncing, modifier = Modifier.weight(1f)) { Text("Remove folder") }
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("Automatic sync", "Refresh the public folder periodically")
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Automatic sync", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Runs periodically when you’re online", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.autoSync, onCheckedChange = viewModel::setAutoSync)
                }
            }
        }
        item {
            SectionTitle("Appearance", "Choose your preferred theme")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "System", "dark" to "Dark", "light" to "Light").forEach { (value, title) ->
                    FilterChip(selected = settings.theme == value, onClick = { viewModel.setTheme(value) }, label = { Text(title) })
                }
            }
        }
        item {
            SectionTitle("Offline music", "Stored in Sonora’s private app storage")
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("${downloaded.size} downloaded ${if (downloaded.size == 1) "song" else "songs"}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${formatBytes(downloadedBytes)} used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    downloaded.forEach { track ->
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            TrackArtwork(track, 42.dp, Modifier.size(42.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(track.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatBytes(track.sizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteDownload(track) }) {
                                Icon(Icons.Outlined.DeleteOutline, "Delete ${track.title} download", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (downloaded.isNotEmpty()) {
                        TextButton(onClick = { confirmClearDownloads = true }, modifier = Modifier.align(Alignment.End)) { Text("Clear all downloads", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        item {
            SectionTitle("About Sonora")
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Sonora Music Player", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("Sonora reads songs only from the public Drive folder you provide. It does not upload or change your files.", Modifier.padding(top = 8.dp, bottom = 28.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (confirmClearDrive) AlertDialog(
        onDismissRequest = { confirmClearDrive = false },
        title = { Text("Remove Drive folder?") },
        text = { Text("Sonora will forget the folder link and API key. Downloaded songs on this device will remain available.") },
        confirmButton = { TextButton(onClick = { onClearDriveConnection(); confirmClearDrive = false }) { Text("Remove folder", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmClearDrive = false }) { Text("Cancel") } },
    )
    if (confirmClearDownloads) AlertDialog(
        onDismissRequest = { confirmClearDownloads = false },
        title = { Text("Remove downloaded music?") },
        text = { Text("This deletes ${downloaded.size} local ${if (downloaded.size == 1) "copy" else "copies"}. Files in Drive remain unchanged.") },
        confirmButton = { TextButton(onClick = { downloaded.forEach { viewModel.deleteDownload(it) }; confirmClearDownloads = false }) { Text("Remove downloads", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmClearDownloads = false }) { Text("Cancel") } },
    )
}
