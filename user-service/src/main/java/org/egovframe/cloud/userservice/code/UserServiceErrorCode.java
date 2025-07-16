package org.egovframe.cloud.userservice.code;

import org.egovframe.cloud.common.code.ErrorCode;

/**
 * User Service 전용 에러 코드
 * user-service에서만 사용하는 특화된 에러 코드들을 정의
 * messages.properties의 메시지를 사용
 * 
 * 코드 체계:
 * U100~U199: User 인증/권한 에러
 * UV100~UV199: User 검증 에러
 */
public enum UserServiceErrorCode implements ErrorCode {

    // ===================== User 인증/권한 에러 (U100~U199) =====================
    INVALID_CREDENTIALS(401, "U100", "err.invalid.credentials"),
    MISSING_CREDENTIALS(400, "U101", "err.missing.credentials"),
    SESSION_EXPIRED(401, "U102", "err.session.expired"),
    SESSION_INVALID(401, "U103", "err.session.invalid"),
    LOGIN_FAILED(401, "U104", "err.login.failed"),
    USER_NOT_FOUND(404, "U105", "err.user.not.found"),

    // ===================== User 검증 에러 (UV100~UV199) =====================
    USERNAME_REQUIRED(400, "UV100", "validation.username.required"),
    PASSWORD_REQUIRED(400, "UV101", "validation.password.required"),
    USERNAME_SIZE(400, "UV102", "validation.username.size"),
    PASSWORD_SIZE(400, "UV103", "validation.password.size");

    private final int status;
    private final String code;
    private final String messageKey;

    UserServiceErrorCode(final int status, final String code, final String messageKey) {
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
