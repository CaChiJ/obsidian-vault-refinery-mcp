package io.github.cachij.vaultrefinery.mcp.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.cachij.vaultrefinery.domain.note.model.*
import io.github.cachij.vaultrefinery.mcp.handler.McpDomainHandler
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.*

@RestController
@RequestMapping("/mcp")
class McpController(
    private val domainHandlers: List<McpDomainHandler>,
    private val objectMapper: ObjectMapper
) {
    private val sessions = HashMap<String, SessionData>()

    @GetMapping
    fun handleGet(
        request: HttpServletRequest,
        @RequestHeader("Accept", required = false) accept: String?,
        @RequestHeader("Last-Event-ID", required = false) lastEventId: String?,
        @RequestHeader("MCP-Session-Id", required = false) sessionId: String?
    ): ResponseEntity<Any> {
        if (accept?.contains("text/event-stream") != true) {
            return ResponseEntity.status(405).body("Method Not Allowed")
        }

        val emitter = SseEmitter(300000L)

        val session = sessionId?.let { sessions[it] }
        if (sessionId != null && session == null) {
            return ResponseEntity.status(404).body("Session not found")
        }

        try {
            if (lastEventId != null && session != null) {
                resumeStream(emitter, session, lastEventId)
            } else {
                initiateStream(emitter, sessionId)
            }
        } catch (e: Exception) {
            emitter.completeWithError(e)
        }

        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(emitter)
    }

    @PostMapping
    fun handlePost(
        @RequestBody body: String,
        @RequestHeader("Accept", required = false) acceptHeader: String?,
        @RequestHeader("Origin", required = false) origin: String?,
        @RequestHeader("MCP-Session-Id", required = false) sessionId: String?,
        @RequestHeader("MCP-Protocol-Version", required = false) protocolVersion: String?
    ): ResponseEntity<Any> {
        if (!isValidOrigin(origin)) {
            return ResponseEntity.status(403).build()
        }

        val jsonRpcRequest = try {
            objectMapper.readValue(body, JsonRpcRequest::class.java)
        } catch (e: Exception) {
            return ResponseEntity.status(400).body("Invalid JSON-RPC request")
        }

        val session = getSession(sessionId)

        return when (jsonRpcRequest.method) {
            "initialize" -> handleInitialize(jsonRpcRequest, session)
            "tools/list" -> handleListTools(jsonRpcRequest, session)
            "tools/call" -> handleCallTool(jsonRpcRequest, session)
            else -> handleUnknownMethod(jsonRpcRequest)
        }
    }

    @DeleteMapping
    fun handleDelete(
        @RequestHeader("MCP-Session-Id") sessionId: String
    ): ResponseEntity<Any> {
        sessions.remove(sessionId)
        return ResponseEntity.ok().build()
    }

    private fun handleInitialize(request: JsonRpcRequest, session: SessionData): ResponseEntity<Any> {
        val initRequest = objectMapper.convertValue(request.params, InitializeRequest::class.java)
        val primaryHandler = domainHandlers.firstOrNull { it.getServerInfo() != null }

        val serverInfo = primaryHandler?.getServerInfo() ?: ServerInfo(
            name = "vault-refinery-mcp",
            version = "1.0.0"
        )

        val result = InitializeResult(
            protocolVersion = "2025-11-25",
            capabilities = ServerCapabilities(
                tools = ToolCapabilities(listChanged = false)
            ),
            serverInfo = serverInfo
        )

        session.isInitialized = true

        val response = JsonRpcResponse(
            id = request.id,
            result = result
        )

        val headers = org.springframework.http.HttpHeaders()
        headers["MCP-Session-Id"] = session.id

        return ResponseEntity.ok().headers(headers).body(response)
    }

    private fun handleListTools(request: JsonRpcRequest, session: SessionData): ResponseEntity<Any> {
        if (!session.isInitialized) {
            return ResponseEntity.status(400).body(
                JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcError(
                        code = -32600,
                        message = "Not initialized"
                    )
                )
            )
        }

        val allTools = domainHandlers.flatMap { it.getTools() }
        val result = ListToolsResult(tools = allTools)

        val response = JsonRpcResponse(
            id = request.id,
            result = result
        )

        return ResponseEntity.ok().body(response)
    }

    private fun handleCallTool(request: JsonRpcRequest, session: SessionData): ResponseEntity<Any> {
        if (!session.isInitialized) {
            return ResponseEntity.status(400).body(
                JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcError(
                        code = -32600,
                        message = "Not initialized"
                    )
                )
            )
        }

        val callRequest = objectMapper.convertValue(request.params, CallToolRequest::class.java)

        val result = domainHandlers
            .mapNotNull { it.handleCallTool(callRequest) }
            .firstOrNull()
            ?: CallToolResult(
                content = listOf(
                    Content(
                        type = "text",
                        text = "Unknown tool: ${callRequest.name}"
                    )
                ),
                isError = true
            )

        val response = JsonRpcResponse(
            id = request.id,
            result = result
        )

        return ResponseEntity.ok().body(response)
    }

    private fun handleUnknownMethod(request: JsonRpcRequest): ResponseEntity<Any> {
        return ResponseEntity.ok().body(
            JsonRpcResponse(
                id = request.id,
                error = JsonRpcError(
                    code = -32601,
                    message = "Method not found"
                )
            )
        )
    }

    private fun isValidOrigin(origin: String?): Boolean {
        return true
    }

    private fun getSession(sessionId: String?): SessionData {
        if (sessionId != null) {
            sessions[sessionId]?.let { return it }
        }
        return createSession()
    }

    private fun createSession(): SessionData {
        val session = SessionData(id = UUID.randomUUID().toString())
        sessions[session.id] = session
        return session
    }

    private fun initiateStream(emitter: SseEmitter, sessionId: String?) {
        val session = sessionId?.let { sessions[it] } ?: createSession()

        try {
            emitter.send(SseEmitter.event().id(session.currentEventId.toString()).data(""))
            emitter.send(SseEmitter.event().name("retry").data("1000"))
        } catch (e: Exception) {
            emitter.completeWithError(e)
        }
    }

    private fun resumeStream(emitter: SseEmitter, session: SessionData, lastEventId: String) {
        try {
            emitter.send(SseEmitter.event().id(session.currentEventId.toString()).data(""))
        } catch (e: Exception) {
            emitter.completeWithError(e)
        }
    }

    data class SessionData(
        val id: String,
        var isInitialized: Boolean = false,
        var currentEventId: Int = 0
    )
}
