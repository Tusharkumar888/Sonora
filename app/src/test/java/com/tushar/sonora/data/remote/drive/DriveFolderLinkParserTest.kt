package com.tushar.sonora.data.remote.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DriveFolderLinkParserTest {
    @Test
    fun parsesDriveFolderLinkAndResourceKey() {
        val folder = DriveFolderLinkParser.parse(
            "https://drive.google.com/drive/u/0/folders/1AbcDef_GhiJklMnOpQrStuv?resourcekey=share-key",
        )

        assertEquals("1AbcDef_GhiJklMnOpQrStuv", folder.id)
        assertEquals("share-key", folder.resourceKey)
    }

    @Test
    fun parsesLegacyIdLinkWithoutResourceKey() {
        val folder = DriveFolderLinkParser.parse("https://drive.google.com/open?id=1AbcDef_GhiJklMnOpQrStuv")

        assertEquals("1AbcDef_GhiJklMnOpQrStuv", folder.id)
        assertNull(folder.resourceKey)
    }

    @Test
    fun rejectsNonDriveAndFileLinks() {
        assertThrows(IllegalArgumentException::class.java) {
            DriveFolderLinkParser.parse("https://example.com/drive/folders/1AbcDef_GhiJklMnOpQrStuv")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DriveFolderLinkParser.parse("https://drive.google.com/file/d/1AbcDef_GhiJklMnOpQrStuv/view")
        }
    }
}
