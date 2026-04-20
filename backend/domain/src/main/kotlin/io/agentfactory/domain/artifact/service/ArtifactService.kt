package io.agentfactory.domain.artifact.service

import io.agentfactory.domain.artifact.command.ArtifactInsertCommand
import io.agentfactory.domain.artifact.model.Artifact
import io.agentfactory.domain.artifact.persistence.IArtifactPort
import io.agentfactory.domain.artifact.query.ArtifactFindAllByJobIdQuery

class ArtifactService(
    private val artifactPort: IArtifactPort,
) {
    suspend fun register(command: ArtifactInsertCommand): Artifact =
        artifactPort.insert(command)

    suspend fun listByJob(jobId: String): List<Artifact> =
        artifactPort.findAllByJobId(ArtifactFindAllByJobIdQuery(jobId))
}
