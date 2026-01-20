package io.github.cachij.vaultrefinery.domain.note.orchestrator

import io.github.cachij.vaultrefinery.domain.note.model.*
import io.github.cachij.vaultrefinery.domain.note.service.NoteService
import io.github.cachij.vaultrefinery.mcp.handler.McpDomainHandler
import org.springframework.stereotype.Service

@Service
class NoteMcpOrchestrator(
    private val noteService: NoteService
) : McpDomainHandler {
    private val serverInfo = ServerInfo(
        name = "obsidian-vault-refinery-mcp",
        version = "0.0.1"
    )

    private val tools = listOf(
        Tool(
            name = "note_list_notes",
            description = "List all markdown notes from the Obsidian vault at the specified path",
            inputSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf(
                        "type" to "string",
                        "description" to "Absolute path to the Obsidian vault directory"
                    )
                ),
                "required" to listOf("path")
            )
        ),
        Tool(
            name = "note_read_note",
            description = "Read the content of a specific note from the Obsidian vault",
            inputSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf(
                        "type" to "string",
                        "description" to "Absolute path to the Obsidian vault directory"
                    ),
                    "fileName" to mapOf(
                        "type" to "string",
                        "description" to "Name of the note file (e.g., 'my-note.md')"
                    )
                ),
                "required" to listOf("path", "fileName")
            )
        )
    )

    override fun getTools(): List<Tool> = tools

    override fun getServerInfo(): ServerInfo? = serverInfo

    override fun handleCallTool(request: CallToolRequest): CallToolResult? {
        return when (request.name) {
            "note_list_notes" -> listNotes(request.arguments)
            "note_read_note" -> readNote(request.arguments)
            else -> null
        }
    }

    private fun listNotes(arguments: Map<String, Any?>): CallToolResult {
        val path = arguments["path"] as? String
            ?: return CallToolResult(
                content = listOf(
                    Content(
                        type = "text",
                        text = "Missing required parameter: path"
                    )
                ),
                isError = true
            )

        return try {
            val notes = noteService.loadNotesFromVault(path)
            val summary = notes.joinToString("\n") { note ->
                "- ${note.fileName} (${note.filePath})\n  Size: ${note.sizeBytes} bytes\n  Modified: ${note.lastModified}"
            }

            CallToolResult(
                content = listOf(
                    Content(
                        type = "text",
                        text = "Found ${notes.size} notes:\n\n$summary"
                    )
                )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    Content(
                        type = "text",
                        text = "Error listing notes: ${e.message}"
                    )
                ),
                isError = true
            )
        }
    }

    private fun readNote(arguments: Map<String, Any?>): CallToolResult {
        val path = arguments["path"] as? String
            ?: return CallToolResult(
                content = listOf(
                    Content(
                        type = "text",
                        text = "Missing required parameter: path"
                    )
                ),
                isError = true
            )

        val fileName = arguments["fileName"] as? String
            ?: return CallToolResult(
                content = listOf(
                    Content(
                        type = "text",
                        text = "Missing required parameter: fileName"
                    )
                ),
                isError = true
            )

        return try {
            val notes = noteService.loadNotesFromVault(path)
            val note = notes.find { it.fileName == fileName }

            if (note != null) {
                CallToolResult(
                    content = listOf(
                        Content(
                            type = "text",
                            text = "# ${note.fileName}\n\n${note.content}"
                        )
                    )
                )
            } else {
                CallToolResult(
                    content = listOf(
                        Content(
                            type = "text",
                            text = "Note not found: $fileName"
                        )
                    ),
                    isError = true
                )
            }
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    Content(
                        type = "text",
                        text = "Error reading note: ${e.message}"
                    )
                ),
                isError = true
            )
        }
    }
}
