package io.agentfactory.infrastructure.persistence.entity

import io.agentfactory.domain.artifact.enums.ArtifactKind
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("artifacts")
data class ArtifactEntity(
    @Id
    @Column("id")
    @get:JvmName("idValue")
    val id: String,
    @Column("job_id")
    val jobId: String,
    @Column("kind")
    val kind: ArtifactKind,
    @Column("path")
    val path: String,
    @Column("size_bytes")
    val sizeBytes: Long?,
    @Column("checksum")
    val checksum: String?,
    @CreatedDate
    @Column("created_at")
    val createdAt: OffsetDateTime? = null,
) : Persistable<String> {

    @Transient
    private var _isNew: Boolean = true

    override fun getId(): String = id

    override fun isNew(): Boolean = _isNew

    fun markNotNew(): ArtifactEntity = apply { _isNew = false }
}
