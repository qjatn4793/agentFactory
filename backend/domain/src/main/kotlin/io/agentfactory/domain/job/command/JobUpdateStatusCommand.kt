package io.agentfactory.domain.job.command

import io.agentfactory.domain.job.enums.JobStatus

data class JobUpdateStatusCommand(
    val id: String,
    val status: JobStatus,
    val workspacePath: String? = null,
    val errorMessage: String? = null,
)
