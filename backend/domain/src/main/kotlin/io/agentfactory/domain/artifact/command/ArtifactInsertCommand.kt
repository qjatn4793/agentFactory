package io.agentfactory.domain.artifact.command

import io.agentfactory.domain.artifact.enums.ArtifactKind

data class ArtifactInsertCommand(
    val jobId: String,
    val kind: ArtifactKind,
    val path: String,
    val sizeBytes: Long? = null,
    val checksum: String? = null,
)
