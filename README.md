# Sonora — Android Music Player

Sonora is a Kotlin and Jetpack Compose music player that reads audio from one publicly shared Google Drive folder. Paste the folder link and a Google Drive API key in Settings; Sonora lists the folder contents and streams audio directly from Drive without Google account sign-in.

## Features

- Public Drive folder setup by pasted link, with folder ID and resource-key parsing.
- Paginated Drive API v3 sync, recursive subfolder traversal, and supported-audio filtering.
- Direct Media3 streaming from Drive's public download links, background media session and notification controls, queue, repeat, shuffle, playback speed, and audio focus.
- Room-backed favorites, listening history, playlists, and downloaded audio.
- WorkManager downloads to app-private storage, progress notifications, and periodic sync.
- Dark, light, and system themes; Home, Library, Playlists, and Settings tabs; local song and playlist search.

## Requirements

- Android Studio with JDK 17.
- Android SDK Platform 36.1 and Build Tools 35.0.0.
- A Google Cloud project with the Google Drive API enabled.

The project uses Android Gradle Plugin 8.13.2 with Gradle 8.13, Kotlin 2.3.21, Compose BOM 2026.04.01, Media3 1.11.1, Room 2.8.5, WorkManager 2.12.0, and Hilt 2.58.

## Google Drive setup

1. Create or select a project in [Google Cloud Console](https://console.cloud.google.com/) and enable the **Google Drive API**.
2. Create an API key in **APIs & Services → Credentials**. Restrict it to the Google Drive API. For an Android app, add an Android application restriction with package name `com.tushar.sonora` and the SHA-1 certificate fingerprint for the signing key used to install the app.
3. In Google Drive, share the music folder as **Anyone with the link → Viewer**. The audio files inside it must also be accessible to anyone with the link. Sonora intentionally reads only public link-shared content.
4. Run Sonora, open **Settings**, paste the folder link and API key, then tap **Save folder and sync**.

To view the debug SHA-1, run `gradlew.bat :app:signingReport` from the project root or open **Gradle → app → Tasks → android → signingReport** in Android Studio. The signing key and SHA-1 must match the APK installed on the device. Register a separate SHA-1 if you use a release signing key.

## Open and run

1. Open this folder in Android Studio and allow Gradle sync to finish, or open it in VS Code and use the Gradle wrapper in the integrated terminal.
2. Start an Android 8.0 (API 26) or newer device or emulator with internet access.
3. Run the `app` configuration. Paste the public Drive folder link and API key in Settings to load music.

Build a debug APK from the project root:

```powershell
./gradlew.bat :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Data and security

- No Google account sign-in or OAuth token is used. The standard API key identifies the Google Cloud project for requests; it does not make private Drive files public or grant access to them.
- Any person who has the shared folder link can access its contents. Do not use this setup for private music. To keep public API key use within quota, restrict the key to the Drive API and the Android package/signing certificate.
- The API key is stored in the app's private Preferences DataStore and is not committed to the repository or written to logs. Since this is a client app, a determined user can still extract a key from a device; API restrictions are important.
- Audio streams from each file's public Drive download link when available. Sonora sends the API key only on Drive API metadata requests and supports Drive resource keys for link-shared folders and files.
- Offline files are saved in app-private `filesDir/music`; they are not written to shared storage. Removing a download deletes the local copy and leaves the Drive file unchanged.
- Songs, playlists, favorites, play history, and preferences are stored locally using Room and Preferences DataStore.

## Project layout

- `data/remote/drive`: Drive API v3, public-folder link parsing, and media URL handling.
- `data/local`: Room entities, DAOs, database, migrations, and Preferences DataStore.
- `data/repository`: Drive, library, and playlist implementations.
- `domain`: track models and repository interfaces.
- `presentation`: Compose screens, ViewModel, components, and theme.
- `service/playback`: MediaSessionService and Media3 item mapping.
- `service/sync`: download and periodic sync workers.
- `di`: Hilt bindings and network/database providers.

## Verification

Run the local unit tests and debug APK build with:

```powershell
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The tests cover audio matching, filename display titles, public Drive folder link parsing, and paginated folder sync. Live Drive streaming and background notification playback need a shared public folder and an Android device or emulator with internet access.
# Sonora
