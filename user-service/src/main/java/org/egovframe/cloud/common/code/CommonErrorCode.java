package org.egovframe.cloud.common.code;

/**
 * 공통 에러 코드
 * 모든 서비스에서 공통으로 사용할 수 있는 에러 코드들을 정의
 * common-messages.properties의 메시지를 사용
 * 
 * 코드 체계:
 * E001~E099: 입력/검증 관련
 * E100~E199: 인증/권한 관련  
 * E200~E299: 리소스 관련
 * E300~E399: 비즈니스 로직 관련
 * E400~E499: 시스템 관련
 * E500~E599: 서버 에러 관련
 * V001~V099: 공통 검증
 * B001~B099: 공통 비즈니스
 */
public enum CommonErrorCode implements ErrorCode {

    // ===================== 입력/검증 관련 (E001~E099) =====================
    INVALID_INPUT_VALUE(400, "E001", "err.invalid.input.value"),
    INVALID_TYPE_VALUE(400, "E002", "err.invalid.type.value"),
    VALIDATION_REQUIRED(400, "E003", "err.validation.required"),
    VALIDATION_SIZE(400, "E004", "err.validation.size"),
    UNPROCESSABLE_ENTITY(422, "E005", "err.unprocessable.entity"),
    
    // ===================== 인증/권한 관련 (E100~E199) =====================
    UNAUTHORIZED(401, "E100", "err.unauthorized"),
    ACCESS_DENIED(403, "E101", "err.access.denied"),
    
    // ===================== 리소스 관련 (E200~E299) =====================
    ENTITY_NOT_FOUND(404, "E200", "err.entity.not.found"),
    ENTITY_NOT_FOUND_WITH_ID(404, "E201", "err.entity.not.found.with.id"),
    NOT_FOUND(404, "E202", "err.page.not.found"),
    METHOD_NOT_ALLOWED(405, "E203", "err.method.not.allowed"),
    
    // ===================== 시스템 관련 (E400~E499) =====================
    SYSTEM_MAINTENANCE(503, "E400", "err.system.maintenance"),
    SERVICE_UNAVAILABLE(503, "E401", "err.service.unavailable"),
    
    // ===================== 서버 에러 관련 (E500~E599) =====================
    INTERNAL_SERVER_ERROR(500, "E500", "err.internal.server"),
    OPERATION_FAILED(500, "E501", "err.operation.failed"),

    // ===================== 공통 비즈니스 에러 (B001~B099) =====================
    BUSINESS_CUSTOM_MESSAGE(400, "B001", "err.system.maintenance"),
    DUPLICATE_INPUT_INVALID(400, "B002", "err.duplicate.input.value"),
    DB_CONSTRAINT_DELETE(400, "B003", "err.constraint.delete"),

    // ===================== 공통 검증 에러 (V001~V099) =====================
    PAGE_MIN(400, "V001", "validation.page.min"),
    SIZE_MIN(400, "V002", "validation.size.min"),
    SORT_PATTERN(400, "V003", "validation.sort.pattern"),
    ORDER_PATTERN(400, "V004", "validation.order.pattern"),
    KEYWORD_SIZE(400, "V005", "validation.keyword.size");

    private final int status;
    private final String code;
    private final String messageKey;

    CommonErrorCode(final int status, final String code, final String messageKey) {
        this.status = status;
        this.code = code;
        this.messageKey = messageKey;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
