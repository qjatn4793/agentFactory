package io.agentfactory.domain.agent.persistence

import io.agentfactory.domain.agent.command.AgentExecutionInsertCommand
import io.agentfactory.domain.agent.command.AgentExecutionUpdateStatusCommand
import io.agentfactory.domain.agent.model.AgentExecution
import io.agentfactory.domain.agent.query.AgentExecutionFindAllByJobIdQuery

interface IAgentExecutionPort {
    suspend fun insert(command: AgentExecutionInsertCommand): AgentExecution
    suspend fun updateStatus(command: AgentExecutionUpdateStatusCommand): AgentExecution
    suspend fun findAllByJobId(query: AgentExecutionFindAllByJobIdQuery): List<AgentExecution>
}
