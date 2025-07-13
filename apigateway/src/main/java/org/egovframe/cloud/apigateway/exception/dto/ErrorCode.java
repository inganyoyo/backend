package org.egovframe.cloud.apigateway.exception.dto;

/**
 * org.egovframe.cloud.apigateway.exception.dto.ErrorCode
 * <p>
 * REST API 요청에 대한 오류 반환값을 정의
 * ErrorResponse 클래스에서 status, code, message 세 가지 속성을 의존한다
 * message 는 MessageSource 의 키 값을 정의하여 다국어 처리를 지원한다
 *
 * @version 1.0
 * @since 2025/07/14
 */
public enum ErrorCode {

    INVALID_INPUT_VALUE(400, "E001", "err.invalid.input.value"), // Bad Request
    INVALID_TYPE_VALUE(400, "E002", "err.invalid.type.value"), // Bad Request
    ENTITY_NOT_FOUND(400, "E003", "err.entity.not.found"), // Bad Request
    UNAUTHORIZED(401, "E004", "err.unauthorized"), // The request requires an user authentication
    ACCESS_DENIED(403, "E005", "err.access.denied"), // Forbidden, Access is Denied
    NOT_FOUND(404, "E007", "err.not.found"), // Not found
    METHOD_NOT_ALLOWED(405, "E008", "err.method.not.allowed"), // 요청 방법이 서버에 의해 알려졌으나, 사용 불가능한 상태
    UNPROCESSABLE_ENTITY(422, "E009", "err.unprocessable.entity"), // Unprocessable Entity
    INTERNAL_SERVER_ERROR(500, "E999", "err.internal.server"), // Server Error
    SERVICE_UNAVAILABLE(503, "E010", "err.service.unavailable"), // Service Unavailable
    BUSINESS_CUSTOM_MESSAGE(400, "B001", ""), // 사용자 정의 메시지를 넘기는 business exception
    ;


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
