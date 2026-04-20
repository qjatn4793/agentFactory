package io.agentfactory.domain.artifact.persistence

import io.agentfactory.domain.artifact.command.ArtifactInsertCommand
import io.agentfactory.domain.artifact.model.Artifact
import io.agentfactory.domain.artifact.query.ArtifactFindAllByJobIdQuery

interface IArtifactPort {
    suspend fun insert(command: ArtifactInsertCommand): Artifact
    suspend fun findAllByJobId(query: ArtifactFindAllByJobIdQuery): List<Artifact>
}
