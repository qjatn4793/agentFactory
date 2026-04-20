package io.agentfactory.domain.agent.service

import io.agentfactory.domain.agent.command.AgentExecutionInsertCommand
import io.agentfactory.domain.agent.command.AgentExecutionUpdateStatusCommand
import io.agentfactory.domain.agent.model.AgentExecution
import io.agentfactory.domain.agent.persistence.IAgentExecutionPort
import io.agentfactory.domain.agent.query.AgentExecutionFindAllByJobIdQuery

class AgentExecutionService(
    private val agentExecutionPort: IAgentExecutionPort,
) {
    suspend fun start(command: AgentExecutionInsertCommand): AgentExecution =
        agentExecutionPort.insert(command)

    suspend fun updateStatus(command: AgentExecutionUpdateStatusCommand): AgentExecution =
        agentExecutionPort.updateStatus(command)

    suspend fun listByJob(jobId: String): List<AgentExecution> =
        agentExecutionPort.findAllByJobId(AgentExecutionFindAllByJobIdQuery(jobId))
}
