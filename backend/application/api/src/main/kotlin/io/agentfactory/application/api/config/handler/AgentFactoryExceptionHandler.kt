package io.agentfactory.application.api.config.handler

import io.agentfactory.domain.common.exception.AgentFactoryException
import io.agentfactory.domain.common.exception.IExceptionHandler
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class AgentFactoryExceptionHandler : IExceptionHandler<AgentFactoryException> {
    override val exceptionType: KClass<out AgentFactoryException> = AgentFactoryException::class

    override fun handle(exception: Throwable, attributes: MutableMap<String, Any?>) {
        val ex = exception as AgentFactoryException
        attributes["status"] = ex.errorCode.status
        attributes["messageCode"] = ex.errorCode.code
        attributes["message"] = ex.errorCode.defaultMessage
        attributes.remove("error")
        attributes.remove("trace")
        attributes.remove("path")
        attributes.remove("requestId")
    }
}
