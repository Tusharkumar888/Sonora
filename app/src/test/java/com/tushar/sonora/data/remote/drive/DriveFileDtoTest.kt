package com.tushar.sonora.data.remote.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveFileDtoTest {
    @Test
    fun recognizesSupportedAudioMimeTypesAndExtensions() {
        assertTrue(DriveFileDto.supportsAudio("unknown.bin", "audio/mpeg"))
        assertTrue(DriveFileDto.supportsAudio("live-set.FLAC", "application/octet-stream"))
        assertTrue(DriveFileDto.supportsAudio("voice.m4a", "audio/mp4"))
        assertFalse(DriveFileDto.supportsAudio("notes.txt", "text/plain"))
        assertFalse(DriveFileDto.supportsAudio("cover.jpg", "image/jpeg"))
    }

    @Test
    fun stripsOnlyTheFileExtensionFromTheDisplayTitle() {
        assertEquals("Song Name", DriveFileDto.displayTitle("Song Name.mp3"))
        assertEquals("Artist - Song Name", DriveFileDto.displayTitle("Artist - Song Name.flac"))
        assertEquals("Live.Set", DriveFileDto.displayTitle("Live.Set"))
    }
}
