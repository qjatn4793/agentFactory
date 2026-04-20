package io.agentfactory.domain.job.exception

import io.agentfactory.domain.common.exception.ErrorCode

enum class JobErrorCode(
    override val status: Int,
    override val code: String,
    override val defaultMessage: String,
) : ErrorCode {
    JOB_NOT_FOUND(404, "JOB_NOT_FOUND", "해당 작업을 찾을 수 없습니다."),
    JOB_INVALID_STATUS_TRANSITION(400, "JOB_INVALID_STATUS_TRANSITION", "허용되지 않는 상태 전이입니다."),
    ;
}
