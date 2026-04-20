package io.agentfactory.application.api.config.handler

import io.agentfactory.domain.common.exception.IExceptionHandler
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Order(Ordered.LOWEST_PRECEDENCE)
@Component
class FallbackExceptionHandler : IExceptionHandler<Throwable> {
    override val exceptionType: KClass<out Throwable> = Throwable::class

    override fun supports(throwable: Throwable): Boolean = true

    override fun handle(exception: Throwable, attributes: MutableMap<String, Any?>) {
        if (attributes["messageCode"] != null) return
        attributes["status"] = 500
        attributes["messageCode"] = "INTERNAL_ERROR"
        attributes["message"] = "요청을 처리하지 못했습니다."
        attributes.remove("error")
        attributes.remove("trace")
    }
}
