package org.egovframe.cloud.apigateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
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

    public WebFluxSecurityConfig(MessageSource messageSource) {
        this.messageSource = messageSource;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * WebFlux 스프링 시큐리티 설정을 구성한다
     *
     * @param http  ServerHttpSecurity 객체
     * @param check 인증/인가 체크 매니저
     * @return SecurityWebFilterChain 설정된 보안 필터 체인
     * @throws Exception 설정 중 오류 발생 시
     * @see ReactiveAuthorization
     */
    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http, ReactiveAuthorizationManager<AuthorizationContext> check) throws Exception {
        http
                .csrf().disable()
                .headers().frameOptions().disable()
                .and()
                .formLogin().disable()
                .httpBasic().authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)) // login dialog disabled & 401 HttpStatus return
                .and()
                .authorizeExchange()
                .pathMatchers(PERMITALL_ANTPATTERNS).permitAll()
                .anyExchange().access(check)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(customAuthenticationEntryPoint()) // 인증 실패 시 JSON
                .accessDeniedHandler(customAccessDeniedHandler()); // 인가 실패 시 JSON
        return http.build();
    }

    /**
     * 인증 실패 시 JSON 응답을 반환하는 커스텀 EntryPoint를 생성한다
     *
     * @return ServerAuthenticationEntryPoint 인증 실패 처리 핸들러
     */
    @Bean
    public ServerAuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (exchange, ex) -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

            ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
            String message = messageSource.getMessage(errorCode.getMessage(), null, LocaleContextHolder.getLocale());

            // user-service와 동일한 ApiResponse 구조 사용
            ApiResponse<Void> apiResponse = ApiResponse.error(message, errorCode.getCode());

            String json;
            try {
                json = objectMapper.writeValueAsString(apiResponse);
            } catch (JsonProcessingException e) {
                // Fallback JSON
                json = String.format("{\"success\":false,\"message\":\"%s\",\"errorCode\":\"%s\",\"timestamp\":\"%s\"}",
                        message, errorCode.getCode(), java.time.LocalDateTime.now());
            }

            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        };
    }


    /**
     * 인가 실패 시 JSON 응답을 반환하는 커스텀 AccessDeniedHandler를 생성한다
     *
     * @return ServerAccessDeniedHandler 인가 실패 처리 핸들러
     */
    @Bean
    public ServerAccessDeniedHandler customAccessDeniedHandler() {
        return (exchange, denied) -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

            ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
            String message = messageSource.getMessage(errorCode.getMessage(), null, LocaleContextHolder.getLocale());

            // user-service와 동일한 ApiResponse 구조 사용
            ApiResponse<Void> apiResponse = ApiResponse.error(message, errorCode.getCode());

            String json;
            try {
                json = objectMapper.writeValueAsString(apiResponse);
            } catch (JsonProcessingException e) {
                // Fallback JSON
                json = String.format("{\"success\":false,\"message\":\"%s\",\"errorCode\":\"%s\",\"timestamp\":\"%s\"}",
                        message, errorCode.getCode(), java.time.LocalDateTime.now());
            }

            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        };
    }

}
