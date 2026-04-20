package io.agentfactory.domain.job.persistence

import io.agentfactory.domain.job.command.JobInsertCommand
import io.agentfactory.domain.job.command.JobUpdateStatusCommand
import io.agentfactory.domain.job.model.Job

interface IJobPort {
    suspend fun insert(command: JobInsertCommand): Job
    suspend fun updateStatus(command: JobUpdateStatusCommand): Job
    suspend fun findById(id: String): Job?
}
