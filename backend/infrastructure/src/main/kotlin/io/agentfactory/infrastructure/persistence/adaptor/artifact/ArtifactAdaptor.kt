package io.agentfactory.infrastructure.persistence.adaptor.artifact

import io.agentfactory.domain.artifact.command.ArtifactInsertCommand
import io.agentfactory.domain.artifact.model.Artifact
import io.agentfactory.domain.artifact.persistence.IArtifactPort
import io.agentfactory.domain.artifact.query.ArtifactFindAllByJobIdQuery
import io.agentfactory.infrastructure.persistence.entity.ArtifactEntity
import io.agentfactory.infrastructure.persistence.repository.r2dbc.ArtifactRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class ArtifactAdaptor(
    private val repository: ArtifactRepository,
) : IArtifactPort {

    override suspend fun insert(command: ArtifactInsertCommand): Artifact {
        val entity = ArtifactEntity(
            id = UUID.randomUUID().toString(),
            jobId = command.jobId,
            kind = command.kind,
            path = command.path,
            sizeBytes = command.sizeBytes,
            checksum = command.checksum,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC),
        )
        return repository.save(entity).toDomain()
    }

    override suspend fun findAllByJobId(query: ArtifactFindAllByJobIdQuery): List<Artifact> =
        repository.findAllByJobId(query.jobId).toList().map { it.toDomain() }
}

private fun ArtifactEntity.toDomain(): Artifact = Artifact(
    id = id,
    jobId = jobId,
    kind = kind,
    path = path,
    sizeBytes = sizeBytes,
    checksum = checksum,
    createdAt = createdAt ?: OffsetDateTime.now(ZoneOffset.UTC),
)
