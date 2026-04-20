package io.agentfactory.domain.job.exception

import io.agentfactory.domain.common.exception.AgentFactoryException

class JobException(
    errorCode: JobErrorCode,
    params: Map<String, Any?> = emptyMap(),
) : AgentFactoryException(errorCode, params)
