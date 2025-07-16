package org.egovframe.cloud.apigateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.apigateway.dto.ApiResponse;
import org.egovframe.cloud.apigateway.exception.dto.ErrorCode;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.egovframe.cloud.apigateway.config.GlobalConstant.PERMITALL_ANTPATTERNS;

/**
 * org.egovframe.cloud.apigateway.config.WebFluxSecurityConfig
 * <p>
 * Spring Security Config 클래스
 * ReactiveAuthorizationManager<AuthorizationContext> 구현체 ReactiveAuthorization 클래스를 통해 인증/인가 처리를 구현한다.
 *
 * @version 1.0
 * @since 2025/07/14
 */
@Slf4j
@EnableWebFluxSecurity // Spring Security 설정들을 활성화시켜 준다
public class WebFluxSecurityConfig {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public WebFluxSecurityConfig(MessageSource messageSource, ObjectMapper objectMapper) {
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * WebFlux 스프링 시큐리티 설정을 구성한다
     *
     * @param http  ServerHttpSecurity 객체
     * @param check 인증/인가 체크 매니저
     * @return SecurityWebFilterChain 설정된 보안 필터 체인
     * @see ReactiveAuthorization
     */
    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http, ReactiveAuthorizationManager<AuthorizationContext> check) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .headers(headers -> headers.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) // 기본 인증 완전 비활성화
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PERMITALL_ANTPATTERNS).permitAll()
                        .anyExchange().access(check))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler()))
                .build();
    }

    /**
     * 인증 실패 시 JSON 응답을 반환하는 커스텀 EntryPoint를 생성한다
     * user-service의 status 코드를 확인하여 적절한 응답을 반환한다
     * GatewayErrorWebExceptionHandler와 동일한 ApiResponse 형식 사용
     *
     * @return ServerAuthenticationEntryPoint 인증 실패 처리 핸들러
     */
    @Bean
    public ServerAuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (exchange, ex) -> {
            ServerHttpResponse response = exchange.getResponse();
            
            // Exchange attributes에서 status 코드 확인
            Object statusCodeAttr = exchange.getAttributes().get("AUTH_STATUS_CODE");
            
            ErrorCode errorCode;
            
            if (statusCodeAttr instanceof Integer) {
                int statusCode = (Integer) statusCodeAttr;
                if (statusCode == HttpStatus.FORBIDDEN.value()) {
                    // 403: 인가 실패 (로그인했지만 권한 없음)
                    errorCode = ErrorCode.ACCESS_DENIED;
                    log.info("Returning 403 - Access denied for authenticated user");
                } else {
                    // 401: 인증 실패 (기본값)
                    errorCode = ErrorCode.UNAUTHORIZED;
                    log.info("Returning 401 - Authentication required");
                }
            } else {
                // 기본값: 401
                errorCode = ErrorCode.UNAUTHORIZED;
                log.info("Returning 401 - Default authentication required");
            }
            
            response.setStatusCode(HttpStatus.valueOf(errorCode.getStatus()));
            response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
            // WWW-Authenticate 헤더 제거로 브라우저 로그인 다이얼로그 방지
            response.getHeaders().remove("WWW-Authenticate");
            
            // 🆕 ErrorCode enum과 MessageSource 활용
            String message = messageSource.getMessage(errorCode.getMessage(), null, 
                    LocaleContextHolder.getLocale());
            ApiResponse<Void> apiResponse = ApiResponse.error(message, errorCode.getCode());
            
            String json;
            try {
                json = objectMapper.writeValueAsString(apiResponse);
            } catch (JsonProcessingException e) {
                // Fallback JSON
                json = String.format("{\"success\":false,\"message\":\"%s\",\"errorCode\":\"%s\",\"timestamp\":\"%s\"}", 
                        message, errorCode.getCode(), java.time.LocalDateTime.now());
                log.error("Error writing auth JSON response", e);
            }
            
            log.info("Auth response: status={}, code={}, message={}", 
                    errorCode.getStatus(), errorCode.getCode(), message);
            
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        };
    }

    /**
     * 인가 실패 시 JSON 응답을 반환하는 커스텀 AccessDeniedHandler를 생성한다
     * 이 핸들러는 주로 Spring Security 내부에서 사용되는 경우를 위한 fallback
     * GatewayErrorWebExceptionHandler와 동일한 ApiResponse 형식 사용
     *
     * @return ServerAccessDeniedHandler 인가 실패 처리 핸들러
     */
    @Bean
    public ServerAccessDeniedHandler customAccessDeniedHandler() {
        return (exchange, denied) -> {
            ServerHttpResponse response = exchange.getResponse();
            
            // Exchange attributes에서 status 코드 확인
            Object statusCodeAttr = exchange.getAttributes().get("AUTH_STATUS_CODE");
            
            ErrorCode errorCode = ErrorCode.ACCESS_DENIED; // 기본값: 403
            
            // user-service의 status 코드가 있다면 그것을 우선 사용
            if (statusCodeAttr instanceof Integer) {
                int statusCode = (Integer) statusCodeAttr;
                if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
                    errorCode = ErrorCode.UNAUTHORIZED;
                }
            }
            
            response.setStatusCode(HttpStatus.valueOf(errorCode.getStatus()));
            response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

            // 🆕 ErrorCode enum과 MessageSource 활용
            String message = messageSource.getMessage(errorCode.getMessage(), null, 
                    LocaleContextHolder.getLocale());
            ApiResponse<Void> apiResponse = ApiResponse.error(message, errorCode.getCode());

            String json;
            try {
                json = objectMapper.writeValueAsString(apiResponse);
            } catch (JsonProcessingException e) {
                // Fallback JSON
                json = String.format("{\"success\":false,\"message\":\"%s\",\"errorCode\":\"%s\",\"timestamp\":\"%s\"}",
                        message, errorCode.getCode(), java.time.LocalDateTime.now());
                log.error("Error writing access denied JSON response", e);
            }

            log.info("Access denied response: status={}, code={}, message={}", 
                    errorCode.getStatus(), errorCode.getCode(), message);

            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        };
    }

}
