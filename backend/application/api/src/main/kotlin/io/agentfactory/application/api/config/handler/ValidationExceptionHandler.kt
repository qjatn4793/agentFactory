package io.agentfactory.application.api.config.handler

import io.agentfactory.domain.common.exception.IExceptionHandler
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebExchangeBindException
import kotlin.reflect.KClass

@Component
class ValidationExceptionHandler : IExceptionHandler<WebExchangeBindException> {
    override val exceptionType: KClass<out WebExchangeBindException> = WebExchangeBindException::class

    override fun handle(exception: Throwable, attributes: MutableMap<String, Any?>) {
        val ex = exception as WebExchangeBindException
        attributes["status"] = 400
        attributes["messageCode"] = "VALIDATION_FAILED"
        attributes["message"] = "입력값이 올바르지 않습니다."
        attributes["fields"] = ex.bindingResult.fieldErrors.map {
            mapOf("field" to it.field, "message" to (it.defaultMessage ?: "invalid"))
        }
        attributes.remove("error")
        attributes.remove("trace")
    }
}
