package io.agentfactory.domain.job.model

import io.agentfactory.domain.job.enums.JobStatus
import java.time.OffsetDateTime

data class Job(
    val id: String,
    val idea: String,
    val status: JobStatus,
    val workspacePath: String?,
    val errorMessage: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
