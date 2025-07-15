package com.example.demo.common.exception.dto;

/**
 * org.egovframe.cloud.common.exception.dto.ErrorCode
 * <p>
 * REST API 요청에 대한 오류 반환값을 정의
 * ErrorResponse 클래스에서 status, code, message 세 가지 속성을 의존한다
 * message 는 MessageSource 의 키 값을 정의하여 다국어 처리를 지원한다
 *
 * <pre>

 * </pre>
 */
public enum CustomErrorCode implements ErrorCode {

    INVALID_BOARD_TYPE(400, "B004", "err.invalid.board.type"),
    BOARD_LIST_ERROR(500, "B100", "err.board.list.error"),
    SYSTEM_MAINTENANCE(503, "B101", "err.system.maintenance"),
    BOARD_TYPE_MISMATCH(400, "B102", "err.board.type.mismatch"),
    INVALID_TITLE_CONTENT(400, "B103", "err.invalid.title.content"),
    ADMIN_WRITE_FORBIDDEN(403, "B104", "err.admin.write.forbidden"),
    FORBIDDEN_CONTENT(400, "B105", "err.forbidden.content"),
    DELETE_FORBIDDEN(403, "B106", "err.delete.forbidden"),
    BOARD_TYPE_REQUIRED(400, "BV001", "validation.board.type.required"),
    TITLE_REQUIRED(400, "BV002", "validation.title.required"),
    TITLE_SIZE(400, "BV003", "validation.title.size"),
    CONTENT_REQUIRED(400, "BV004", "validation.content.required"),
    CONTENT_SIZE(400, "BV005", "validation.content.size"),
    AUTHOR_REQUIRED(400, "BV006", "validation.author.required"),
    AUTHOR_SIZE(400, "BV007", "validation.author.size"),
    PAGE_MIN(400, "BV008", "validation.page.min"),
    SIZE_MIN(400, "BV009", "validation.size.min"),
    SORT_PATTERN(400, "BV010", "validation.sort.pattern"),
    ORDER_PATTERN(400, "BV011", "validation.order.pattern"),
    KEYWORD_SIZE(400, "BV012", "validation.keyword.size");


    private final int status;
    private final String code;
    private final String message;

    CustomErrorCode(final int status, final String code, final String message) {
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
