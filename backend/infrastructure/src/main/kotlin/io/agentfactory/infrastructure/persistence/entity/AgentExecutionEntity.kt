package io.agentfactory.infrastructure.persistence.entity

import io.agentfactory.domain.agent.enums.AgentExecutionStatus
import io.agentfactory.domain.agent.enums.AgentStage
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("agent_executions")
data class AgentExecutionEntity(
    @Id
    @Column("id")
    @get:JvmName("idValue")
    val id: String,
    @Column("job_id")
    val jobId: String,
    @Column("stage")
    val stage: AgentStage,
    @Column("status")
    val status: AgentExecutionStatus,
    @Column("attempt")
    val attempt: Int,
    @Column("started_at")
    val startedAt: OffsetDateTime?,
    @Column("finished_at")
    val finishedAt: OffsetDateTime?,
    @Column("error_message")
    val errorMessage: String?,
    @CreatedDate
    @Column("created_at")
    val createdAt: OffsetDateTime? = null,
    @LastModifiedDate
    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,
) : Persistable<String> {

    @Transient
    private var _isNew: Boolean = true

    override fun getId(): String = id

    override fun isNew(): Boolean = _isNew

    fun markNotNew(): AgentExecutionEntity = apply { _isNew = false }
}
