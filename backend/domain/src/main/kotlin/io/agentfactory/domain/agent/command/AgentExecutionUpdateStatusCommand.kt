package io.agentfactory.domain.agent.command

import io.agentfactory.domain.agent.enums.AgentExecutionStatus
import java.time.OffsetDateTime

data class AgentExecutionUpdateStatusCommand(
    val id: String,
    val status: AgentExecutionStatus,
    val startedAt: OffsetDateTime? = null,
    val finishedAt: OffsetDateTime? = null,
    val errorMessage: String? = null,
)
