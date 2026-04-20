package io.agentfactory.domain.common.exception

interface ErrorCode {
    val status: Int
    val code: String
    val defaultMessage: String
}
