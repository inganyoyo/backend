package org.egovframe.cloud.apigateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * org.egovframe.cloud.apigateway.filter.AuthResponseFilter
 * <p>
 * 로그인/로그아웃 응답에서 세션 쿠키를 설정/해제하고 redirect_uri를 추가하는 필터
 * 리다이렉트는 프론트엔드에서 API 응답을 받아서 처리
 *
 * @version 1.0
 * @since 2025/07/14
 */
@Slf4j
@Component
public class AuthResponseFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 🆕 리다이렉트 URI 상수
    private static final String LOGIN_SUCCESS_REDIRECT = "/dashboard";
    private static final String LOGOUT_SUCCESS_REDIRECT = "/login";
    private static final String SESSION_EXPIRED_REDIRECT = "/login";

    /**
     * 필터를 적용하여 인증 요청에 대한 응답을 처리한다
     * 쿠키 설정/해제만 수행하고, 리다이렉트는 프론트엔드에서 처리
     *
     * @param exchange 서버 웹 교환 객체
     * @param chain 게이트웨이 필터 체인
     * @return Mono<Void> 필터 처리 결과
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.debug("AuthResponseFilter start");
        ServerHttpRequest request = exchange.getRequest();

        // 🆕 응답을 가로채서 세션 만료 및 쿠키 처리
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {

                // 🆕 세션 만료 헤더 체크 (모든 응답에 대해)
                String sessionExpired = getDelegate().getHeaders().getFirst("X-Session-Expired");
                
                // 세션 만료 시 쿠키 삭제 + redirect_uri 추가
                if ("true".equals(sessionExpired)) {
                    log.info("Session expired detected, clearing session cookie");
                    clearSessionCookie(getDelegate());
                    
                    // 🆕 세션 만료 응답에 redirect_uri 추가
                    getDelegate().getHeaders().add("X-Redirect-URI", SESSION_EXPIRED_REDIRECT);
                    log.info("Added X-Redirect-URI header for session expiry: {}", SESSION_EXPIRED_REDIRECT);
                }

                // 🆕 인증 관련 요청이 아니면 바로 통과
                if (!isAuthRequest(request)) {
                    return super.writeWith(body);
                }

                log.info("Processing auth request: {}", request.getPath());

                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.collectList().map(dataBuffers -> {
                        // 응답 본문을 문자열로 변환
                        StringBuilder responseBodyBuilder = new StringBuilder();
                        for (DataBuffer dataBuffer : dataBuffers) {
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            responseBodyBuilder.append(new String(content, StandardCharsets.UTF_8));
                            DataBufferUtils.release(dataBuffer);
                        }

                        String responseBody = responseBodyBuilder.toString();
                        log.debug("Auth response body: {}", responseBody);

                        // JSON에서 성공 여부 확인하고 쿠키 및 리다이렉트 URI 처리
                        try {
                            JsonNode jsonNode = objectMapper.readTree(responseBody);

                            if (isLoginRequest(request)) {
                                // 🆕 로그인 성공 시 세션 쿠키 설정 + redirect_uri 추가
                                if (jsonNode.has("success") && jsonNode.get("success").asBoolean()
                                        && jsonNode.has("data") && jsonNode.get("data").has("sessionId")) {

                                    String sessionId = jsonNode.get("data").get("sessionId").asText();
                                    log.info("Login successful, setting session cookie: {}",
                                            sessionId.substring(0, Math.min(8, sessionId.length())) + "...");

                                    // 세션 쿠키 설정
                                    ResponseCookie sessionCookie = ResponseCookie.from("GSNS-SESSION", sessionId)
                                            .httpOnly(true)
                                            .secure(false) // 개발환경에서는 false
                                            .sameSite("Strict")
                                            .path("/")
                                            .maxAge(-1) // 브라우저 세션 종료 시까지 유지
                                            .build();

                                    getDelegate().addCookie(sessionCookie);
                                    log.info("Session cookie set successfully");

                                    // 🆕 응답에 redirect_uri 추가
                                    com.fasterxml.jackson.databind.node.ObjectNode modifiedResponse = 
                                            objectMapper.createObjectNode();
                                    modifiedResponse.setAll((com.fasterxml.jackson.databind.node.ObjectNode) jsonNode);
                                    modifiedResponse.put("redirect_uri", LOGIN_SUCCESS_REDIRECT);
                                    
                                    String modifiedBody = objectMapper.writeValueAsString(modifiedResponse);
                                    log.info("Added redirect_uri to login response: {}", LOGIN_SUCCESS_REDIRECT);
                                    return getDelegate().bufferFactory().wrap(modifiedBody.getBytes(StandardCharsets.UTF_8));
                                }
                            } else if (isLogoutRequest(request)) {
                                // 🆕 로그아웃 성공 시 세션 쿠키 삭제 + redirect_uri 추가
                                if (jsonNode.has("success") && jsonNode.get("success").asBoolean()) {
                                    log.info("Logout successful, clearing session cookie");
                                    clearSessionCookie(getDelegate());

                                    // 🆕 응답에 redirect_uri 추가
                                    com.fasterxml.jackson.databind.node.ObjectNode modifiedResponse = 
                                            objectMapper.createObjectNode();
                                    modifiedResponse.setAll((com.fasterxml.jackson.databind.node.ObjectNode) jsonNode);
                                    modifiedResponse.put("redirect_uri", LOGOUT_SUCCESS_REDIRECT);
                                    
                                    String modifiedBody = objectMapper.writeValueAsString(modifiedResponse);
                                    log.info("Added redirect_uri to logout response: {}", LOGOUT_SUCCESS_REDIRECT);
                                    return getDelegate().bufferFactory().wrap(modifiedBody.getBytes(StandardCharsets.UTF_8));
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse auth response", e);
                        }

                        // 🆕 원본 JSON 응답 그대로 반환 (리다이렉트 없음)
                        return getDelegate().bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
                    }));
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * 세션 쿠키를 삭제한다
     *
     * @param response HTTP 응답 객체
     */
    private void clearSessionCookie(org.springframework.http.server.reactive.ServerHttpResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from("GSNS-SESSION", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(0) // 즉시 만료
                .build();
        response.addCookie(deleteCookie);
        log.info("Session cookie cleared successfully");
    }

    /**
     * 인증 관련 요청인지 확인한다
     *
     * @param request HTTP 요청 객체
     * @return boolean 인증 요청 여부
     */
    private boolean isAuthRequest(ServerHttpRequest request) {
        String path = request.getPath().toString();
        String method = request.getMethod().toString();

        boolean isAuth = "POST".equals(method) &&
                (path.contains("/user-service/api/auth/login") ||
                        path.contains("/user-service/api/auth/logout"));
        
        return isAuth;
    }

    /**
     * 로그인 요청인지 확인한다
     *
     * @param request HTTP 요청 객체
     * @return boolean 로그인 요청 여부
     */
    private boolean isLoginRequest(ServerHttpRequest request) {
        String path = request.getPath().toString();
        String method = request.getMethod().toString();
        return "POST".equals(method) && path.contains("/user-service/api/auth/login");
    }

    /**
     * 로그아웃 요청인지 확인한다
     *
     * @param request HTTP 요청 객체
     * @return boolean 로그아웃 요청 여부
     */
    private boolean isLogoutRequest(ServerHttpRequest request) {
        String path = request.getPath().toString();
        String method = request.getMethod().toString();
        return "POST".equals(method) && path.contains("/user-service/api/auth/logout");
    }

    /**
     * 필터의 실행 순서를 반환한다
     *
     * @return int 필터 실행 순서 (낮을수록 먼저 실행)
     */
    @Override
    public int getOrder() {
        return -1;
    }
}