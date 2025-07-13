package org.egovframe.cloud.apigateway.exception;


import org.egovframe.cloud.apigateway.exception.dto.ErrorCode;

/**
 * org.egovframe.cloud.apigateway.exception.BusinessMessageException
 * <p>
 * 런타임시 비즈니스 로직상 사용자에게 알려줄 오류 메시지를 만들어 던지는 처리를 담당한다
 *
 * @version 1.0
 * @since 2025/07/14
 */
public class BusinessMessageException extends BusinessException {

    /**
     * 사용자에게 표시될 메시지와 상태코드 400을 설정한다
     *
     * @param customMessage 사용자에게 표시할 메시지
     */
    public BusinessMessageException(String customMessage) {
        super(ErrorCode.BUSINESS_CUSTOM_MESSAGE, customMessage);
    }

}
