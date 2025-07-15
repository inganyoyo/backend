package com.example.demo.common.util;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.code.SuccessCode;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * HTTP 응답 생성을 위한 유틸리티 클래스 (빌더 패턴)
 * 일관된 응답 형태를 제공하고 코드의 가독성을 향상
 * 
 * 사용법:
 * - ResponseUtil.success(successCode).data(data).args(args...).build()
 * - ResponseUtil.created(successCode).data(data).args(args...).build()
 * - ResponseUtil.success(successCode).args(args...).build()  // 데이터 없음
 */
@Component
public class SuccessResponseUtil {
    
    private final MessageUtil messageUtil;
    
    public SuccessResponseUtil(MessageUtil messageUtil) {
        this.messageUtil = messageUtil;
    }
    
    // ===================== 빌더 시작점 =====================
    
    /**
     * 200 OK 성공 응답 빌더 시작
     */
    public ResponseBuilder success(SuccessCode successCode) {
        return new ResponseBuilder(messageUtil, successCode, HttpStatus.OK);
    }
    
    /**
     * 201 Created 생성 응답 빌더 시작
     */
    public ResponseBuilder created(SuccessCode successCode) {
        return new ResponseBuilder(messageUtil, successCode, HttpStatus.CREATED);
    }
    
    // ===================== 빌더 클래스 =====================
    
    public static class ResponseBuilder {
        private final MessageUtil messageUtil;
        private final SuccessCode successCode;
        private final HttpStatus httpStatus;
        private Object data;
        private Object[] args = new Object[0];
        
        public ResponseBuilder(MessageUtil messageUtil, SuccessCode successCode, HttpStatus httpStatus) {
            this.messageUtil = messageUtil;
            this.successCode = successCode;
            this.httpStatus = httpStatus;
        }
        
        /**
         * 응답 데이터 설정
         */
        public ResponseBuilder data(Object data) {
            this.data = data;
            return this;
        }
        
        /**
         * 메시지 템플릿 인자 설정
         */
        public ResponseBuilder args(Object... args) {
            this.args = args;
            return this;
        }
        
        /**
         * 최종 ResponseEntity 생성
         */
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<ApiResponse<T>> build() {
            String message = messageUtil.getMessage(successCode.getMessageKey(), 
                    args, LocaleContextHolder.getLocale());
            
            ApiResponse<T> apiResponse;
            if (data != null) {
                apiResponse = ApiResponse.success(message, (T) data);
            } else {
                apiResponse = ApiResponse.success(message);
            }
            
            return ResponseEntity.status(httpStatus).body(apiResponse);
        }
    }
}
