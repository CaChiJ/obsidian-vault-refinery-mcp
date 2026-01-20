package io.github.cachij.vaultrefinery.domain.note.service

import io.github.cachij.vaultrefinery.domain.note.model.NoteMetadata
import io.github.cachij.vaultrefinery.infra.filesystem.LocalFileScanner
import org.springframework.stereotype.Service

@Service
class NoteService(
    private val fileScanner: LocalFileScanner
) {
    fun loadNotesFromVault(path: String): List<NoteMetadata> {
        val notes = fileScanner.scan(path)
        println("Refinery: Found ${notes.size} notes in $path")
        return notes
    }
}