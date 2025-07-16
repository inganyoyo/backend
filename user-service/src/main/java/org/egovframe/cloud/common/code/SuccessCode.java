package org.egovframe.cloud.common.code;

/**
 * 성공 응답 코드 정의
 * 템플릿 기반으로 메시지를 관리하여 중복 제거 및 유연성 확보
 */
public enum SuccessCode {

    // 템플릿 기반 성공 코드
    ACTION_SUCCESS("S001", "success.action"),
    LIST_RETRIEVED("S002", "success.list.retrieved"),
    ITEM_RETRIEVED("S003", "success.item.retrieved"),
    OPERATION_COMPLETED("S004", "success.operation.completed"),
    
    // 기본 성공 메시지
    DEFAULT_SUCCESS("S000", "success.default");

    private final String code;
    private final String messageKey;

    SuccessCode(final String code, final String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
