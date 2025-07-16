package org.egovframe.cloud.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.apigateway.exception.dto.ErrorCode;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * org.egovframe.cloud.apigateway.exception.GatewayErrorWebExceptionHandler
 * <p>
 * Gateway 전용 ErrorWebExceptionHandler
 * Spring Cloud Gateway에서 발생하는 모든 에러를 처리
 *
 * @version 1.0
 * @since 2025/07/14
 */
@Slf4j
@Order(-1) // DefaultErrorWebExceptionHandler보다 우선순위 높게 설정
@Component
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    /**
     * GatewayErrorWebExceptionHandler 생성자
     *
     * @param messageSource 메시지 소스 객체
     * @param objectMapper JSON 매퍼 객체
     */
    public GatewayErrorWebExceptionHandler(MessageSource messageSource, ObjectMapper objectMapper) {
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
        // 🆕 Java 8 LocalDateTime 직렬화 지원 (혹시 모를 경우를 대비)
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    /**
     * 예외를 처리하여 일관된 에러 응답을 생성한다
     *
     * @param exchange 서버 웹 교환 객체
     * @param ex 발생한 예외
     * @return Mono<Void> 에러 응답 처리 결과
     */

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        // 이미 커밋된 응답이면 처리하지 않음
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        ErrorCode errorCode = determineErrorCode(ex);
        HttpStatus httpStatus = HttpStatus.valueOf(errorCode.getStatus());
        
        // 로깅 (상세 에러는 DEBUG 레벨에서만)
        String path = exchange.getRequest().getPath().value();
        log.warn("Gateway Error - Status: {}, Code: {}, Path: {}, Error: {}", 
            httpStatus.value(), errorCode.getCode(), path, ex.getClass().getSimpleName());
        log.debug("Detailed error information", ex);

        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

        String message = messageSource.getMessage(errorCode.getMessage(), null, LocaleContextHolder.getLocale());
        
        // ApiResponse 구조 사용
        org.egovframe.cloud.apigateway.dto.ApiResponse<Void> apiResponse = 
            org.egovframe.cloud.apigateway.dto.ApiResponse.error(message, errorCode.getCode());

        try {
            String json = objectMapper.writeValueAsString(apiResponse);
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing JSON response", e);
            String fallbackJson = String.format(
                "{\"success\":false,\"message\":\"%s\",\"errorCode\":\"%s\",\"timestamp\":\"%s\"}",
                message, errorCode.getCode(), LocalDateTime.now()
            );
            DataBuffer buffer = response.bufferFactory().wrap(fallbackJson.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    /**
     * 예외 타입에 따라 적절한 ErrorCode를 결정한다
     *
     * @param ex 발생한 예외
     * @return ErrorCode 매핑된 에러 코드
     */
    private ErrorCode determineErrorCode(Throwable ex) {
        // Connection refused, timeout 등의 네트워크 에러
        if (ex.getCause() instanceof java.net.ConnectException) {
            return ErrorCode.SERVICE_UNAVAILABLE;
        }
        
        // Gateway timeout
        if (ex instanceof TimeoutException) {
            return ErrorCode.SERVICE_UNAVAILABLE;
        }
        
        // Gateway not found (라우트 없음)
        if (ex instanceof NotFoundException) {
            return ErrorCode.NOT_FOUND;
        }

        // 기타 모든 에러
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }
}
