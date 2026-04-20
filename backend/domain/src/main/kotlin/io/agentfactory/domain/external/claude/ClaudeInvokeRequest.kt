package io.agentfactory.domain.external.claude

import io.agentfactory.domain.agent.enums.AgentStage

data class ClaudeInvokeRequest(
    val jobId: String,
    val stage: AgentStage,
    val systemPrompt: String,
    val userPrompt: String,
    val workspacePath: String,
)
