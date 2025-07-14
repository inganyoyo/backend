package org.egovframe.cloud.apigateway.filter;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.apigateway.domain.User;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class UserHeaderInjectionFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 🆕 AuthorizationManager에서 저장한 사용자 정보 조회
        User user = exchange.getAttribute("USER_INFO");

        if (user != null) {
            log.info("Adding user headers: X-User-ID={}, X-User-Role={}",
                    user.getUserId(), user.getRole());

            // 🆕 요청에 사용자 정보 헤더 추가
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-ID", user.getUserId())
                    .header("X-User-Role", user.getRole())
                    .header("X-User-Email", user.getEmail()) // 필요시 추가
                    .header("X-Username", user.getUsername()) // 필요시 추가
                    .build();

            // 🆕 수정된 요청으로 교체해서 다운스트림 서비스로 전달
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } else {
            log.info("No user info found in exchange - proceeding without user headers");
            return chain.filter(exchange);
        }
    }

    @Override
    public int getOrder() {
        // 🆕 AuthorizationManager 이후, 라우팅 이전에 실행되어야 함
        return 150; // 낮은 숫자일수록 먼저 실행됨
    }
}