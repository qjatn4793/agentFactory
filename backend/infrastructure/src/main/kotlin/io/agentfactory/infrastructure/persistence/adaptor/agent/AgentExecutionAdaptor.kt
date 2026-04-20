package io.agentfactory.infrastructure.persistence.adaptor.agent

import io.agentfactory.domain.agent.command.AgentExecutionInsertCommand
import io.agentfactory.domain.agent.command.AgentExecutionUpdateStatusCommand
import io.agentfactory.domain.agent.enums.AgentExecutionStatus
import io.agentfactory.domain.agent.model.AgentExecution
import io.agentfactory.domain.agent.persistence.IAgentExecutionPort
import io.agentfactory.domain.agent.query.AgentExecutionFindAllByJobIdQuery
import io.agentfactory.infrastructure.persistence.entity.AgentExecutionEntity
import io.agentfactory.infrastructure.persistence.repository.r2dbc.AgentExecutionRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class AgentExecutionAdaptor(
    private val repository: AgentExecutionRepository,
) : IAgentExecutionPort {

    override suspend fun insert(command: AgentExecutionInsertCommand): AgentExecution {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val entity = AgentExecutionEntity(
            id = UUID.randomUUID().toString(),
            jobId = command.jobId,
            stage = command.stage,
            status = AgentExecutionStatus.PENDING,
            attempt = command.attempt,
            startedAt = null,
            finishedAt = null,
            errorMessage = null,
            createdAt = now,
            updatedAt = now,
        )
        return repository.save(entity).toDomain()
    }

    override suspend fun updateStatus(command: AgentExecutionUpdateStatusCommand): AgentExecution {
        val existing = repository.findById(command.id)
            ?: error("agent execution not found: ${command.id}")
        val updated = existing.copy(
            status = command.status,
            startedAt = command.startedAt ?: existing.startedAt,
            finishedAt = command.finishedAt ?: existing.finishedAt,
            errorMessage = command.errorMessage ?: existing.errorMessage,
            updatedAt = OffsetDateTime.now(ZoneOffset.UTC),
        ).markNotNew()
        return repository.save(updated).toDomain()
    }

    override suspend fun findAllByJobId(query: AgentExecutionFindAllByJobIdQuery): List<AgentExecution> =
        repository.findAllByJobId(query.jobId).toList().map { it.toDomain() }
}

private fun AgentExecutionEntity.toDomain(): AgentExecution = AgentExecution(
    id = id,
    jobId = jobId,
    stage = stage,
    status = status,
    attempt = attempt,
    startedAt = startedAt,
    finishedAt = finishedAt,
    errorMessage = errorMessage,
    createdAt = createdAt ?: OffsetDateTime.now(ZoneOffset.UTC),
    updatedAt = updatedAt ?: OffsetDateTime.now(ZoneOffset.UTC),
)
