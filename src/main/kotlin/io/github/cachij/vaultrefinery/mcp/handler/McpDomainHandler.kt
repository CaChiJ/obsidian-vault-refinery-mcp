package io.github.cachij.vaultrefinery.mcp.handler

import io.github.cachij.vaultrefinery.domain.note.model.*

interface McpDomainHandler {
    fun getTools(): List<Tool>
    fun getServerInfo(): ServerInfo?
    fun handleCallTool(request: CallToolRequest): CallToolResult?
}
