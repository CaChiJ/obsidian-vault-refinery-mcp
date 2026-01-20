package io.github.cachij.vaultrefinery.domain.note.model

import java.time.LocalDateTime

data class NoteMetadata(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val lastModified: LocalDateTime,
    val extension: String,
    val content: String,
    val contentHash: String
) {
    override fun toString(): String {
        return "NoteMetadata(fileName='$fileName', filePath='$filePath', sizeBytes=$sizeBytes, lastModified=$lastModified, extension='$extension', contentHash='$contentHash')"
    }
}