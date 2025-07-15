package com.example.demo.common.exception.dto;

/**
 * org.egovframe.cloud.common.exception.dto.ErrorCode
 * <p>
 * REST API 요청에 대한 오류 반환값을 정의
 * ErrorResponse 클래스에서 status, code, message 세 가지 속성을 의존한다
 * message 는 MessageSource 의 키 값을 정의하여 다국어 처리를 지원한다
 *
 * @author 표준프레임워크센터 jaeyeolkim
 * @version 1.0
 * @since 2021/07/16
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2021/07/16    jaeyeolkim  최초 생성
 * </pre>
 */
public enum ErrorCode {

    INVALID_INPUT_VALUE(400, "E001", "err.invalid.input.value"), // Bad Request
    INVALID_TYPE_VALUE(400, "E002", "err.invalid.type.value"), // Bad Request
    ENTITY_NOT_FOUND(400, "E003", "err.entity.not.found"), // Bad Request
    UNAUTHORIZED(401, "E004", "err.unauthorized"), // The request requires an user authentication
    JWT_EXPIRED(401, "E005", "err.unauthorized"), // The request requires an user authentication
    ACCESS_DENIED(403, "E006", "err.access.denied"), // Forbidden, Access is Denied
    NOT_FOUND(404, "E010", "err.page.not.found"), // Not found
    METHOD_NOT_ALLOWED(405, "E011", "err.method.not.allowed"), // 요청 방법이 서버에 의해 알려졌으나, 사용 불가능한 상태
    REQUIRE_USER_JOIN(412, "E012", "err.user.notexists"), // Server Error
    UNPROCESSABLE_ENTITY(422, "E020", "err.unprocessable.entity"), // Unprocessable Entity
    INTERNAL_SERVER_ERROR(500, "E999", "err.internal.server"), // Server Error
    SERVICE_UNAVAILABLE(503, "E021", "err.service.unavailable"), // Service Unavailable

    // business error code
    BUSINESS_CUSTOM_MESSAGE(400, "B001", ""), // 사용자 정의 메시지를 넘기는 business exception
    DUPLICATE_INPUT_INVALID(400, "B002", "err.duplicate.input.value"), // 중복된 값을 입력하였습니다
    DB_CONSTRAINT_DELETE(400, "B003", "err.constraint.delete"), // 참조하는 데이터가 있어서 삭제할 수 없습니다
    INVALID_BOARD_TYPE(400, "B004", "err.invalid.board.type"), // 유효하지 않은 게시판 타입
    
    // Board Service 관련 에러
    BOARD_LIST_ERROR(500, "B100", "err.board.list.error"), // 목록 조회 중 오류가 발생했습니다
    SYSTEM_MAINTENANCE(503, "B101", "err.system.maintenance"), // 시스템 점검 중입니다
    BOARD_TYPE_MISMATCH(400, "B102", "err.board.type.mismatch"), // 요청한 게시판 타입과 게시글 타입이 일치하지 않습니다
    INVALID_TITLE_CONTENT(400, "B103", "err.invalid.title.content"), // 게시글 제목에 허용되지 않는 문구가 포함되었습니다
    ADMIN_WRITE_FORBIDDEN(403, "B104", "err.admin.write.forbidden"), // 관리자 계정으로는 게시글을 작성할 수 없습니다  
    FORBIDDEN_CONTENT(400, "B105", "err.forbidden.content"), // 수정할 수 없는 내용이 포함되었습니다
    DELETE_FORBIDDEN(403, "B106", "err.delete.forbidden") // 첫 번째 게시글은 삭제할 수 없습니다
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
