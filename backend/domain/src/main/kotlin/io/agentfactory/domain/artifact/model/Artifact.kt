package io.agentfactory.domain.artifact.model

import io.agentfactory.domain.artifact.enums.ArtifactKind
import java.time.OffsetDateTime

data class Artifact(
    val id: String,
    val jobId: String,
    val kind: ArtifactKind,
    val path: String,
    val sizeBytes: Long?,
    val checksum: String?,
    val createdAt: OffsetDateTime,
)
