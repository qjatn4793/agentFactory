package io.agentfactory.domain.common.exception

abstract class AgentFactoryException(
    val errorCode: ErrorCode,
    val params: Map<String, Any?> = emptyMap(),
) : RuntimeException(errorCode.defaultMessage)
