package com.example.demo.common.code;

/**
 * Board Service 전용 에러 코드
 * board-service에서만 사용하는 특화된 에러 코드들을 정의
 * messages.properties의 메시지를 사용
 * 
 * 코드 체계:
 * B100~B199: Board 비즈니스 에러
 * BV100~BV199: Board 검증 에러
 */
public enum CustomErrorCode implements ErrorCode {

    // ===================== Board 비즈니스 에러 (B100~B199) =====================
    INVALID_BOARD_TYPE(400, "B100", "err.invalid.board.type"),
    BOARD_LIST_ERROR(500, "B101", "err.board.list.error"),
    BOARD_TYPE_MISMATCH(400, "B102", "err.board.type.mismatch"),
    INVALID_TITLE_CONTENT(400, "B103", "err.invalid.title.content"),
    ADMIN_WRITE_FORBIDDEN(403, "B104", "err.admin.write.forbidden"),
    FORBIDDEN_CONTENT(400, "B105", "err.forbidden.content"),
    DELETE_FORBIDDEN(403, "B106", "err.delete.forbidden"),

    // ===================== Board 검증 에러 (BV100~BV199) =====================
    BOARD_TYPE_REQUIRED(400, "BV100", "validation.board.type.required"),
    TITLE_REQUIRED(400, "BV101", "validation.title.required"),
    TITLE_SIZE(400, "BV102", "validation.title.size"),
    CONTENT_REQUIRED(400, "BV103", "validation.content.required"),
    CONTENT_SIZE(400, "BV104", "validation.content.size"),
    AUTHOR_REQUIRED(400, "BV105", "validation.author.required"),
    AUTHOR_SIZE(400, "BV106", "validation.author.size");

    private final int status;
    private final String code;
    private final String messageKey;

    CustomErrorCode(final int status, final String code, final String messageKey) {
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
