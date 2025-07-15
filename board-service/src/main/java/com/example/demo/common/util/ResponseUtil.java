package com.example.demo.common.util;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.code.SuccessCode;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * HTTP 응답 생성을 위한 유틸리티 클래스
 * 일관된 응답 형태를 제공하고 코드의 가독성을 향상
 * 
 * - Static methods: 기존 호환성을 위한 하드코딩 방식
 * - Instance methods: SuccessCode를 사용한 개선된 방식
 */
@Component
public class ResponseUtil {
    
    private final MessageUtil messageUtil;
    
    public ResponseUtil(MessageUtil messageUtil) {
        this.messageUtil = messageUtil;
    }
    
    // ===================== Instance Methods (SuccessCode 사용) =====================
    
    /**
     * 200 OK - 성공 응답 (SuccessCode와 데이터 포함)
     */
    public <T> ResponseEntity<ApiResponse<T>> ok(SuccessCode successCode, T data) {
        String message = messageUtil.getMessage(successCode.getMessageKey(), 
                null, LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
    
    /**
     * 200 OK - 성공 응답 (SuccessCode와 데이터, 템플릿 인자 포함)
     */
    public <T> ResponseEntity<ApiResponse<T>> okWithData(SuccessCode successCode, T data, Object... args) {
        String message = messageUtil.getMessage(successCode.getMessageKey(), 
                args, LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
    
    /**
     * 200 OK - 성공 응답 (SuccessCode만)
     */
    public <T> ResponseEntity<ApiResponse<T>> ok(SuccessCode successCode) {
        String message = messageUtil.getMessage(successCode.getMessageKey(), 
                null, LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(message));
    }
    
    /**
     * 200 OK - 성공 응답 (SuccessCode와 템플릿 인자, 데이터 없음)
     */
    public <T> ResponseEntity<ApiResponse<T>> okMessage(SuccessCode successCode, Object... args) {
        String message = messageUtil.getMessage(successCode.getMessageKey(), 
                args, LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(message));
    }
    
    /**
     * 201 Created - 생성 성공 응답
     */
    public <T> ResponseEntity<ApiResponse<T>> created(SuccessCode successCode, T data) {
        String message = messageUtil.getMessage(successCode.getMessageKey(), 
                null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, data));
    }
    
    /**
     * 201 Created - 생성 성공 응답 (템플릿 인자 포함)
     */
    public <T> ResponseEntity<ApiResponse<T>> createdWithData(SuccessCode successCode, T data, Object... args) {
        String message = messageUtil.getMessage(successCode.getMessageKey(), 
                args, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, data));
    }
    
    // ===================== Static Methods (기존 호환성 유지) =====================
    
    /**
     * 200 OK - 성공 응답 (데이터 포함)
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    /**
     * 200 OK - 성공 응답 (메시지와 데이터 포함)
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
    
    /**
     * 200 OK - 성공 응답 (메시지만)
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message) {
        return ResponseEntity.ok(ApiResponse.success(message));
    }
    
    /**
     * 200 OK - 성공 응답 (기본 메시지)
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok() {
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 201 Created - 생성 성공 응답
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", data));
    }
    
    /**
     * 201 Created - 생성 성공 응답 (메시지 커스텀)
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, data));
    }
    
    /**
     * 204 No Content - 성공 응답 (데이터 없음)
     */
    public static <T> ResponseEntity<ApiResponse<T>> noContent() {
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 400 Bad Request - 잘못된 요청
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, "BAD_REQUEST"));
    }
    
    /**
     * 400 Bad Request - 잘못된 요청 (에러코드 포함)
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message, String errorCode) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, errorCode));
    }
    
    /**
     * 401 Unauthorized - 인증 실패
     */
    public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message, "UNAUTHORIZED"));
    }
    
    /**
     * 403 Forbidden - 권한 없음
     */
    public static <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message, "FORBIDDEN"));
    }
    
    /**
     * 404 Not Found - 리소스 없음
     */
    public static <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message, "NOT_FOUND"));
    }
    
    /**
     * 500 Internal Server Error - 서버 에러
     */
    public static <T> ResponseEntity<ApiResponse<T>> internalServerError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, "INTERNAL_SERVER_ERROR"));
    }
    
    /**
     * 커스텀 상태코드 응답
     */
    public static <T> ResponseEntity<ApiResponse<T>> status(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, status.name()));
    }
    
    /**
     * 커스텀 상태코드 성공 응답
     */
    public static <T> ResponseEntity<ApiResponse<T>> status(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status)
                .body(ApiResponse.success(message, data));
    }
}
