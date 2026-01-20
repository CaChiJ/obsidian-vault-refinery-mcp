package io.github.cachij.vaultrefinery.api.web

import io.github.cachij.vaultrefinery.domain.note.model.NoteMetadata
import io.github.cachij.vaultrefinery.domain.note.service.NoteService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RefineryWebController(
    private val noteService: NoteService
) {
    @GetMapping("/api/notes/scan")
    fun scanNotes(@RequestParam path: String): List<NoteMetadata> {
        return noteService.loadNotesFromVault(path)
    }
}