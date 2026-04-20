package io.agentfactory.domain.agent.command

import io.agentfactory.domain.agent.enums.AgentStage

data class AgentExecutionInsertCommand(
    val jobId: String,
    val stage: AgentStage,
    val attempt: Int = 1,
)
