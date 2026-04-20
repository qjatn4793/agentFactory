package io.agentfactory.domain.job.service

import io.agentfactory.domain.job.command.JobInsertCommand
import io.agentfactory.domain.job.command.JobUpdateStatusCommand
import io.agentfactory.domain.job.exception.JobErrorCode
import io.agentfactory.domain.job.exception.JobException
import io.agentfactory.domain.job.model.Job
import io.agentfactory.domain.job.persistence.IJobPort

class JobService(
    private val jobPort: IJobPort,
) {
    suspend fun create(command: JobInsertCommand): Job =
        jobPort.insert(command)

    suspend fun updateStatus(command: JobUpdateStatusCommand): Job =
        jobPort.updateStatus(command)

    suspend fun findById(id: String): Job =
        jobPort.findById(id)
            ?: throw JobException(JobErrorCode.JOB_NOT_FOUND, mapOf("id" to id))
}
