package io.agentfactory.application.api.config

import io.agentfactory.domain.common.exception.IExceptionHandler
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class GlobalErrorAttributes(
    private val handlers: List<IExceptionHandler<Throwable>>,
) : DefaultErrorAttributes() {

    override fun getErrorAttributes(
        request: ServerRequest,
        options: ErrorAttributeOptions,
    ): MutableMap<String, Any?> {
        val attributes: MutableMap<String, Any?> =
            super.getErrorAttributes(request, options).mapValues { it.value as Any? }.toMutableMap()
        val throwable = getError(request) ?: return attributes
        val handler = handlers.firstOrNull { it.supports(throwable) }
        handler?.handle(throwable, attributes)
        return attributes
    }
}
