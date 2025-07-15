package com.example.demo.common.exception;


import com.example.demo.common.code.ErrorCode;

/**
 * org.egovframe.cloud.common.exception.BusinessException
 * <p>
 * 런타임시 비즈니스 로직상 사용자에게 알려줄 오류 메시지를 만들어 던지는 처리를 담당한다
 * 이 클래스를 상속하여 다양한 형태의 business exception 을 만들 수 있고,
 * 그것들은 모두 ExceptionHandlerAdvice BusinessException 처리 메소드에서 잡아낸다.
 * 상황에 맞게 에러 코드를 추가하고 이 클래스를 상속하여 사용할 수 있다.
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

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String customMessage;
    private final Object[] args;

    public BusinessException(BusinessException exception) {
        super(exception.getErrorCode().getMessageKey());
        this.errorCode = exception.getErrorCode();
        this.customMessage = exception.getCustomMessage();
        this.args = exception.getArgs();
    }

    private BusinessException(Builder builder) {
        super(builder.errorCode.getMessageKey());
        this.errorCode = builder.errorCode;
        this.customMessage = builder.customMessage;
        this.args = builder.args;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public Object[] getArgs() {
        return args;
    }

    public static Builder builder(ErrorCode errorCode) {
        return new Builder(errorCode);
    }

    public static class Builder {
        private final ErrorCode errorCode;
        private String customMessage;
        private Object[] args;

        public Builder(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }

        public Builder customMessage(String customMessage) {
            this.customMessage = customMessage;
            return this;
        }

        public Builder args(Object... args) {
            this.args = args;
            return this;
        }

        public BusinessException build() {
            return new BusinessException(this);
        }
    }
}
