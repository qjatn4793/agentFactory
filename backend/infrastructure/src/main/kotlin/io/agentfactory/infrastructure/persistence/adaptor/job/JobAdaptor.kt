package io.agentfactory.infrastructure.persistence.adaptor.job

import io.agentfactory.domain.job.command.JobInsertCommand
import io.agentfactory.domain.job.command.JobUpdateStatusCommand
import io.agentfactory.domain.job.enums.JobStatus
import io.agentfactory.domain.job.exception.JobErrorCode
import io.agentfactory.domain.job.exception.JobException
import io.agentfactory.domain.job.model.Job
import io.agentfactory.domain.job.persistence.IJobPort
import io.agentfactory.infrastructure.persistence.entity.JobEntity
import io.agentfactory.infrastructure.persistence.repository.r2dbc.JobRepository
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class JobAdaptor(
    private val repository: JobRepository,
) : IJobPort {

    override suspend fun insert(command: JobInsertCommand): Job {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val entity = JobEntity(
            id = UUID.randomUUID().toString(),
            idea = command.idea,
            status = JobStatus.PENDING,
            workspacePath = null,
            errorMessage = null,
            createdAt = now,
            updatedAt = now,
        )
        return repository.save(entity).toDomain()
    }

    override suspend fun updateStatus(command: JobUpdateStatusCommand): Job {
        val existing = repository.findById(command.id)
            ?: throw JobException(JobErrorCode.JOB_NOT_FOUND, mapOf("id" to command.id))
        val updated = existing.copy(
            status = command.status,
            workspacePath = command.workspacePath ?: existing.workspacePath,
            errorMessage = command.errorMessage ?: existing.errorMessage,
            updatedAt = OffsetDateTime.now(ZoneOffset.UTC),
        ).markNotNew()
        return repository.save(updated).toDomain()
    }

    override suspend fun findById(id: String): Job? =
        repository.findById(id)?.toDomain()
}

private fun JobEntity.toDomain(): Job = Job(
    id = id,
    idea = idea,
    status = status,
    workspacePath = workspacePath,
    errorMessage = errorMessage,
    createdAt = createdAt ?: OffsetDateTime.now(ZoneOffset.UTC),
    updatedAt = updatedAt ?: OffsetDateTime.now(ZoneOffset.UTC),
)
