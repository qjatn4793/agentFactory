package io.agentfactory.infrastructure.persistence.entity

import io.agentfactory.domain.job.enums.JobStatus
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("jobs")
data class JobEntity(
    @Id
    @Column("id")
    @get:JvmName("idValue")
    val id: String,
    @Column("idea")
    val idea: String,
    @Column("status")
    val status: JobStatus,
    @Column("workspace_path")
    val workspacePath: String?,
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

    fun markNotNew(): JobEntity = apply { _isNew = false }
}
