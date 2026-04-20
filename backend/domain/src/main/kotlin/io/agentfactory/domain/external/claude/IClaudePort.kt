package io.agentfactory.domain.external.claude

interface IClaudePort {
    suspend fun invoke(request: ClaudeInvokeRequest): ClaudeInvokeResult
}
