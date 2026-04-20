package io.agentfactory.domain.agent.model

import io.agentfactory.domain.agent.enums.AgentExecutionStatus
import io.agentfactory.domain.agent.enums.AgentStage
import java.time.OffsetDateTime

data class AgentExecution(
    val id: String,
    val jobId: String,
    val stage: AgentStage,
    val status: AgentExecutionStatus,
    val attempt: Int,
    val startedAt: OffsetDateTime?,
    val finishedAt: OffsetDateTime?,
    val errorMessage: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
