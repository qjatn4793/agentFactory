package io.agentfactory.infrastructure.persistence.repository.r2dbc

import io.agentfactory.infrastructure.persistence.entity.ArtifactEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ArtifactRepository : CoroutineCrudRepository<ArtifactEntity, String> {
    fun findAllByJobId(jobId: String): Flow<ArtifactEntity>
}
