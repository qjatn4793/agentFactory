package io.agentfactory.infrastructure.persistence.repository.r2dbc

import io.agentfactory.infrastructure.persistence.entity.AgentExecutionEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AgentExecutionRepository : CoroutineCrudRepository<AgentExecutionEntity, String> {
    fun findAllByJobId(jobId: String): Flow<AgentExecutionEntity>
}
