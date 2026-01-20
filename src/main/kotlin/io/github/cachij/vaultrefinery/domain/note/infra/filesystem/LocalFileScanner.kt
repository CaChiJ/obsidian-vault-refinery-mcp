package io.github.cachij.vaultrefinery.infra.filesystem

import io.github.cachij.vaultrefinery.domain.note.model.NoteMetadata
import org.springframework.stereotype.Component
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId

@Component
class LocalFileScanner {
    fun scan(folderPath: String): List<NoteMetadata> {
        val folder = File(folderPath)
        require(folder.exists() && folder.isDirectory) { "유효하지 않은 폴더입니다: $folderPath" }

        return folder.walk()
            .filter { it.isFile && !it.name.startsWith(".") }
            .filter { it.extension == "md" }
            .map { file -> convertToMetadata(file) }
            .toList()
    }

    private fun convertToMetadata(file: File): NoteMetadata {
        val content = file.readText()

        return NoteMetadata(
            fileName = file.name,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            lastModified = Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime(),
            extension = file.extension,
            content = content,
            contentHash = computeHash(content)
        )
    }

    private fun computeHash(content: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}