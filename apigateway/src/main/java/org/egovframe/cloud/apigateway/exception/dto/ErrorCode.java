package org.egovframe.cloud.apigateway.exception.dto;

/**
 * org.egovframe.cloud.apigateway.exception.dto.ErrorCode
 * <p>
 * API Gateway 전용 에러 코드
 * 실제 사용되는 에러 코드만 정의
 *
 * @version 1.0
 * @since 2025/07/14
 */
public enum ErrorCode {

    // Security 에러 (WebFluxSecurityConfig)
    UNAUTHORIZED(401, "E004", "err.unauthorized"),
    ACCESS_DENIED(403, "E005", "err.access.denied"),
    
    // Gateway 에러 (GatewayErrorWebExceptionHandler)
    NOT_FOUND(404, "E007", "err.not.found"),
    INTERNAL_SERVER_ERROR(500, "E999", "err.internal.server"),
    SERVICE_UNAVAILABLE(503, "E010", "err.service.unavailable");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(final int status, final String code, final String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
