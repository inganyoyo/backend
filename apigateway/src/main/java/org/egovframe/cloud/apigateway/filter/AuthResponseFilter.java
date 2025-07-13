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
 * 로그인 응답에서 sessionId를 쿠키로 설정하는 필터
 *
 * @version 1.0
 * @since 2025/07/14
 */
@Slf4j
@Component
public class AuthResponseFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 필터를 적용하여 인증 요청에 대한 응답을 처리한다
     *
     * @param exchange 서버 웹 교환 객체
     * @param chain 게이트웨이 필터 체인
     * @return Mono<Void> 필터 처리 결과
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 로그인 또는 로그아웃 요청인지 확인
        if (!isAuthRequest(request)) {
            return chain.filter(exchange);
        }

        log.info("Processing auth request: {}", request.getPath());

        // 응답을 가로채서 sessionId를 쿠키로 설정
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
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
                        log.info("Login response body: {}", responseBody);

                        // JSON에서 성공 여부 확인하고 쿠키 처리
                        try {
                            JsonNode jsonNode = objectMapper.readTree(responseBody);

                            if (isLoginRequest(request)) {
                                // 로그인 처리
                                if (jsonNode.has("sessionId") && jsonNode.has("success")
                                        && jsonNode.get("success").asBoolean()) {

                                    String sessionId = jsonNode.get("sessionId").asText();
                                    log.info("Login successful, setting session cookie: {}", sessionId);

                                    // 세션 쿠키 설정
                                    ResponseCookie sessionCookie = ResponseCookie.from("GSNS-SESSION", sessionId)
                                            .httpOnly(true)
                                            .secure(true) // HTTPS 환경에서는 true로 설정
                                            .sameSite("Strict")
                                            .path("/")
                                            .maxAge(-1)
                                            .build();

                                    getDelegate().addCookie(sessionCookie);
                                }
                            } else if (isLogoutRequest(request)) {
                                // 로그아웃 처리
                                if (jsonNode.has("success") && jsonNode.get("success").asBoolean()) {
                                    log.info("Logout successful, removing session cookie");

                                    // 세션 쿠키 삭제 (maxAge=0으로 설정)
                                    ResponseCookie deleteCookie = ResponseCookie.from("GSNS-SESSION", "")
                                            .httpOnly(true)
                                            .secure(false)
                                            .sameSite("Strict")
                                            .path("/")
                                            .maxAge(0) // 즉시 만료
                                            .build();

                                    getDelegate().addCookie(deleteCookie);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse auth response", e);
                        }

                        // 원본 응답 본문을 새로운 DataBuffer로 생성
                        return getDelegate().bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
                    }));
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * 로그인 또는 로그아웃 요청인지 확인한다
     *
     * @param request HTTP 요청 객체
     * @return boolean 인증 요청 여부
     */
    private boolean isAuthRequest(ServerHttpRequest request) {
        String path = request.getPath().toString();
        String method = request.getMethod().toString();

        boolean isAuth = "POST".equals(method) &&
                (path.contains("/auth-service/api/v1/auth/login") ||
                        path.contains("/auth-service/api/v1/auth/logout"));
        log.debug("Request path: {}, method: {}, isAuth: {}", path, method, isAuth);
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

        boolean isLogin = "POST".equals(method) && path.contains("/auth-service/api/v1/auth/login");
        log.debug("Request path: {}, method: {}, isLogin: {}", path, method, isLogin);
        return isLogin;
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

        boolean isLogout = "POST".equals(method) && path.contains("/auth-service/api/v1/auth/logout");
        log.debug("Request path: {}, method: {}, isLogout: {}", path, method, isLogout);
        return isLogout;
    }

    /**
     * 필터의 실행 순서를 반환한다
     *
     * @return int 필터 실행 순서 (낮을수록 먼저 실행)
     */
    @Override
    public int getOrder() {
        return -1; // GlobalFilter보다 먼저 실행
    }
}