package io.agentfactory.domain.common.exception

import kotlin.reflect.KClass

interface IExceptionHandler<out T : Throwable> {
    val exceptionType: KClass<out T>

    fun supports(throwable: Throwable): Boolean =
        exceptionType.isInstance(throwable)

    fun handle(exception: Throwable, attributes: MutableMap<String, Any?>)
}
