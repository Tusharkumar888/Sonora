package com.tushar.sonora.data.remote.drive

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class DriveFolderReference(val id: String, val resourceKey: String?)

object DriveFolderLinkParser {
    fun parse(value: String): DriveFolderReference {
        val input = value.trim()
        require(input.isNotEmpty()) { "Paste a Google Drive folder link." }

        val uri = runCatching { URI(input) }.getOrElse {
            throw IllegalArgumentException("That does not look like a Google Drive folder link.")
        }
        val host = uri.host?.lowercase()
        require(host == "drive.google.com" || host == "www.drive.google.com") {
            "Use a link copied from a Google Drive folder."
        }

        val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
        val folderSegment = segments.indexOfLast { it == "folders" }
        val pathId = if (folderSegment >= 0) segments.getOrNull(folderSegment + 1) else null
        val id = pathId ?: queryParameter(uri.rawQuery, "id")
        require(!id.isNullOrBlank() && DRIVE_ID.matches(id)) {
            "The link must point to a Google Drive folder."
        }
        return DriveFolderReference(id, queryParameter(uri.rawQuery, "resourcekey"))
    }

    private fun queryParameter(query: String?, name: String): String? = query
        ?.split('&')
        ?.asSequence()
        ?.mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator < 0) null else {
                val key = decode(part.substring(0, separator))
                if (key.equals(name, ignoreCase = true)) decode(part.substring(separator + 1)) else null
            }
        }
        ?.firstOrNull()

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private val DRIVE_ID = Regex("[A-Za-z0-9_-]{10,}")
}
