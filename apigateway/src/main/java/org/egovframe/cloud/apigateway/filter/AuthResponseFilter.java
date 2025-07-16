package org.egovframe.cloud.apigateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class AuthResponseFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SESSION_COOKIE_NAME = "GSNS-SESSION";
    private static final String LOGIN_SUCCESS_REDIRECT = "/dashboard";
    private static final String LOGOUT_SUCCESS_REDIRECT = "/login";
    private static final String SESSION_EXPIRED_REDIRECT = "/login";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                if (!(body instanceof Flux)) {
                    return super.writeWith(body);
                }

                Flux<? extends DataBuffer> flux = (Flux<? extends DataBuffer>) body;

                return DataBufferUtils.join(flux).flatMap(dataBuffer -> {
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    DataBufferUtils.release(dataBuffer);

                    String responseBody = new String(content, StandardCharsets.UTF_8);
                    log.debug("AuthResponseFilter response body: {}", responseBody);

                    // 세션 만료 응답 처리
                    if ("true".equals(originalResponse.getHeaders().getFirst("X-Session-Expired"))) {
                        log.info("Session expired - clearing cookie and setting redirect header");
                        clearSessionCookie(originalResponse);
                        originalResponse.getHeaders().add("X-Redirect-URI", SESSION_EXPIRED_REDIRECT);
                    }

                    // 인증 요청이 아닌 경우 원본 응답 그대로 반환
                    if (!isAuthRequest(request)) {
                        return super.writeWith(Flux.just(bufferFactory.wrap(responseBody.getBytes())));
                    }

                    try {
                        JsonNode jsonNode = objectMapper.readTree(responseBody);

                        if (isLoginRequest(request) && isLoginSuccess(jsonNode)) {
                            String sessionId = jsonNode.get("data").get("sessionId").asText();
                            setSessionCookie(originalResponse, sessionId);
                            ((ObjectNode) jsonNode).put("redirect_uri", LOGIN_SUCCESS_REDIRECT);
                        } else if (isLogoutRequest(request) && isLogoutSuccess(jsonNode)) {
                            clearSessionCookie(originalResponse);
                            ((ObjectNode) jsonNode).put("redirect_uri", LOGOUT_SUCCESS_REDIRECT);
                        }

                        String modifiedBody = objectMapper.writeValueAsString(jsonNode);
                        return super.writeWith(Flux.just(bufferFactory.wrap(modifiedBody.getBytes(StandardCharsets.UTF_8))));

                    } catch (Exception e) {
                        log.error("Failed to process auth response", e);
                        return super.writeWith(Flux.just(bufferFactory.wrap(responseBody.getBytes())));
                    }
                });
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    private void setSessionCookie(ServerHttpResponse response, String sessionId) {
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, sessionId)
                .httpOnly(true)
                .secure(false) // 운영환경에서는 true
                .sameSite("Strict")
                .path("/")
                .maxAge(-1)
                .build();
        response.addCookie(cookie);
        log.info("Session cookie set: {}...", sessionId.substring(0, Math.min(sessionId.length(), 8)));
    }

    private void clearSessionCookie(ServerHttpResponse response) {
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addCookie(cookie);
        log.info("Session cookie cleared");
    }

    private boolean isAuthRequest(ServerHttpRequest request) {
        String path = request.getPath().toString();
        String method = request.getMethodValue();
        return "POST".equalsIgnoreCase(method) &&
                (path.contains("/user-service/api/auth/login") ||
                        path.contains("/user-service/api/auth/logout"));
    }

    private boolean isLoginRequest(ServerHttpRequest request) {
        return request.getPath().toString().contains("/auth/login");
    }

    private boolean isLogoutRequest(ServerHttpRequest request) {
        return request.getPath().toString().contains("/auth/logout");
    }

    private boolean isLoginSuccess(JsonNode json) {
        return json.has("success") && json.get("success").asBoolean()
                && json.has("data") && json.get("data").has("sessionId");
    }

    private boolean isLogoutSuccess(JsonNode json) {
        return json.has("success") && json.get("success").asBoolean();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
