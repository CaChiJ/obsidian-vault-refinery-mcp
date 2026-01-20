package io.github.cachij.vaultrefinery.domain.note.model

data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String? = null,
    val method: String,
    val params: Map<String, Any?>? = null
)

data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: String?,
    val result: Any? = null,
    val error: JsonRpcError? = null
)

data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: Any? = null
)

data class InitializeRequest(
    val protocolVersion: String,
    val capabilities: ClientCapabilities,
    val clientInfo: ClientInfo
)

data class InitializeResult(
    val protocolVersion: String,
    val capabilities: ServerCapabilities,
    val serverInfo: ServerInfo
)

data class ClientCapabilities(
    val roots: Map<String, Any?>? = null,
    val sampling: Map<String, Any?>? = null
)

data class ServerCapabilities(
    val tools: ToolCapabilities
)

data class ToolCapabilities(
    val listChanged: Boolean? = null
)

data class ClientInfo(
    val name: String,
    val version: String
)

data class ServerInfo(
    val name: String,
    val version: String
)

data class ListToolsRequest(
    val cursor: String? = null
)

data class ListToolsResult(
    val tools: List<Tool>
)

data class Tool(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any>
)

data class CallToolRequest(
    val name: String,
    val arguments: Map<String, Any?>
)

data class CallToolResult(
    val content: List<Content>,
    val isError: Boolean = false
)

data class Content(
    val type: String,
    val text: String? = null,
    val data: Any? = null
)
