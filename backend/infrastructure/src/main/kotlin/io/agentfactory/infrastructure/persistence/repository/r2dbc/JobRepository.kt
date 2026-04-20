package io.agentfactory.infrastructure.persistence.repository.r2dbc

import io.agentfactory.infrastructure.persistence.entity.JobEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface JobRepository : CoroutineCrudRepository<JobEntity, String>
