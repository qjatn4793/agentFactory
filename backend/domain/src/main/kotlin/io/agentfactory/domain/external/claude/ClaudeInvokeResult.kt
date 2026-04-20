package io.agentfactory.domain.external.claude

data class ClaudeInvokeResult(
    val rawOutput: String,
    val artifactPaths: List<String>,
    val tokensUsed: Int?,
    val durationMs: Long,
)
