package org.egovframe.cloud.apigateway.util;

import org.egovframe.cloud.apigateway.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * WebFlux HTTP 응답 생성을 위한 유틸리티 클래스
 * 일관된 응답 형태를 제공하고 코드의 가독성을 향상
 */
public class ResponseUtil {
    
    /**
     * 200 OK - 성공 응답 (데이터 포함)
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> ok(T data) {
        return Mono.just(ResponseEntity.ok(ApiResponse.success(data)));
    }
    
    /**
     * 200 OK - 성공 응답 (메시지와 데이터 포함)
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> ok(String message, T data) {
        return Mono.just(ResponseEntity.ok(ApiResponse.success(message, data)));
    }
    
    /**
     * 200 OK - 성공 응답 (메시지만)
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> ok(String message) {
        return Mono.just(ResponseEntity.ok(ApiResponse.success(message)));
    }
    
    /**
     * 200 OK - 성공 응답 (기본 메시지)
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> ok() {
        return Mono.just(ResponseEntity.ok(ApiResponse.success()));
    }
    
    /**
     * 201 Created - 생성 성공 응답
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> created(T data) {
        return Mono.just(ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", data)));
    }
    
    /**
     * 201 Created - 생성 성공 응답 (메시지 커스텀)
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> created(String message, T data) {
        return Mono.just(ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, data)));
    }
    
    /**
     * 400 Bad Request - 잘못된 요청
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> badRequest(String message) {
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, "BAD_REQUEST")));
    }
    
    /**
     * 400 Bad Request - 잘못된 요청 (에러코드 포함)
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> badRequest(String message, String errorCode) {
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, errorCode)));
    }
    
    /**
     * 401 Unauthorized - 인증 실패
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> unauthorized(String message) {
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message, "UNAUTHORIZED")));
    }
    
    /**
     * 403 Forbidden - 권한 없음
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> forbidden(String message) {
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message, "FORBIDDEN")));
    }
    
    /**
     * 404 Not Found - 리소스 없음
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> notFound(String message) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message, "NOT_FOUND")));
    }
    
    /**
     * 500 Internal Server Error - 서버 에러
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> internalServerError(String message) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, "INTERNAL_SERVER_ERROR")));
    }
    
    /**
     * 503 Service Unavailable - 서비스 이용 불가
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> serviceUnavailable(String message) {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(message, "SERVICE_UNAVAILABLE")));
    }
    
    /**
     * 커스텀 상태코드 응답
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> status(HttpStatus status, String message) {
        return Mono.just(ResponseEntity.status(status)
                .body(ApiResponse.error(message, status.name())));
    }
    
    /**
     * 커스텀 상태코드 성공 응답
     */
    public static <T> Mono<ResponseEntity<ApiResponse<T>>> status(HttpStatus status, String message, T data) {
        return Mono.just(ResponseEntity.status(status)
                .body(ApiResponse.success(message, data)));
    }
}
