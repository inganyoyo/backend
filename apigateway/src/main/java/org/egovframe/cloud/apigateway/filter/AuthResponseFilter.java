package org.egovframe.cloud.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Component
public class AuthResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        return chain.filter(exchange).then(
                Mono.defer(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    String sessionExpired = response.getHeaders().getFirst("X-Session-Expired");

                    if (isLoginRequest(request)) {
                        String sessionId = request.getHeaders().getFirst("X-Session-Id"); // 임시 방식
                        if (sessionId != null && !sessionId.isEmpty()) {
                            log.info("Login detected, setting session cookie");

                            ResponseCookie sessionCookie = ResponseCookie.from("GSNS-SESSION", sessionId)
                                    .httpOnly(true)
                                    .secure(false)
                                    .sameSite("Strict")
                                    .path("/")
                                    .maxAge(-1)
                                    .build();

                            response.addCookie(sessionCookie);
                        }

                        response.setStatusCode(HttpStatus.FOUND);
                        response.getHeaders().setLocation(URI.create("/main"));
                        return response.setComplete();
                    }

                    if (isLogoutRequest(request)) {
                        log.info("Logout detected, clearing session cookie");
                        clearSessionCookie(response);
                        response.setStatusCode(HttpStatus.FOUND);
                        response.getHeaders().setLocation(URI.create("/login"));
                        return response.setComplete();
                    }

                    if ("true".equalsIgnoreCase(sessionExpired)) {
                        log.info("Session expired, clearing session cookie");
                        clearSessionCookie(response);
                        response.setStatusCode(HttpStatus.FOUND);
                        response.getHeaders().setLocation(URI.create("/login"));
                        return response.setComplete();
                    }

                    return Mono.empty();
                })
        );
    }

    private void clearSessionCookie(ServerHttpResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from("GSNS-SESSION", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addCookie(deleteCookie);
    }

    private boolean isLoginRequest(ServerHttpRequest request) {
        String path = request.getPath().toString();
        String method = request.getMethodValue();
        return "POST".equalsIgnoreCase(method) && path.contains("/user-service/api/auth/login");
    }

    private boolean isLogoutRequest(ServerHttpRequest request) {
        String path = request.getPath().toString();
        String method = request.getMethodValue();
        return "POST".equalsIgnoreCase(method) && path.contains("/user-service/api/auth/logout");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
