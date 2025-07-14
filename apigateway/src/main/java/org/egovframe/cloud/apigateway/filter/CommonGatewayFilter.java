package org.egovframe.cloud.apigateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.apigateway.config.GlobalConstant;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

import static org.egovframe.cloud.apigateway.config.GlobalConstant.SESSION_COOKIE_NAME;
import static org.egovframe.cloud.apigateway.config.GlobalConstant.SESSION_HEADER_NAME;

/**
 * org.egovframe.cloud.apigateway.filter.GlobalFilter
 * <p>
 * 글로벌 필터 클래스
 * 모든 요청에 대해 로깅 및 세션 헤더 처리를 수행한다
 *
 * @version 1.0
 * @since 2025/07/14
 */
@Slf4j
@Component
public class CommonGatewayFilter extends AbstractGatewayFilterFactory<CommonGatewayFilter.Config> {


    public CommonGatewayFilter() {
        super(Config.class);
    }

    /**
     * GlobalFilter를 생성하여 설정을 적용한다
     *
     * @param config 필터 설정 객체
     * @return GatewayFilter 설정된 게이트웨이 필터
     */
    @Override
    public GatewayFilter apply(Config config) {
        // Pre filter
        return ((exchange, chain) -> {
            // Netty 비동기 방식 서버 사용시에는 ServerHttpRequest 를 사용해야 한다.
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            if (config.isPreLogger()) {
                log.info("[GlobalFilter Start] request ID: {}, method: {}, path: {}",
                        request.getId(), request.getMethod(), request.getPath());
            }

            // 쿠키에서 SESSION-ID 추출하여 X-Session-ID 헤더로 변환
            ServerHttpRequest modifiedRequest = addSessionAndServiceHeaders(request);

            // Post Filter
            // 비동기 방식의 단일값 전달시 Mono 사용(Webflux)
            return chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .then(Mono.fromRunnable(() -> {
                        if (config.isPostLogger()) {
                            log.info("[GlobalFilter End  ] request ID: {}, method: {}, path: {}, statusCode: {}",
                                    request.getId(), request.getMethod(), request.getPath(), response.getStatusCode());
                        }
                    }));
        });
    }

    /**
     * 쿠키에서 GSNS-SESSION을 추출하여 X-Session-ID 헤더로 추가한다
     *
     * @param request 원본 HTTP 요청
     * @return ServerHttpRequest 헤더가 추가된 수정된 요청
     */
    private ServerHttpRequest addSessionAndServiceHeaders(ServerHttpRequest request) {
        // 쿠키에서 GSNS-SESSION 추출
        HttpCookie sessionCookie = request.getCookies().getFirst(SESSION_COOKIE_NAME);

        // 요청 경로에서 서비스 이름 추출
        String serviceName = extractServiceNameFromPath(request.getURI().getPath());

        ServerHttpRequest.Builder mutatedRequest = request.mutate();

        // X-Session-ID 헤더 추가
        if (sessionCookie != null && StringUtils.hasLength(sessionCookie.getValue())) {
            String sessionId = sessionCookie.getValue();
            log.info("Found session cookie: {}", sessionId);
            mutatedRequest.header(SESSION_HEADER_NAME, sessionId);
        } else {
            log.debug("No session cookie found");
        }

        // X-Service-Name 헤더 추가
        if (StringUtils.hasLength(serviceName)) {
            mutatedRequest.header(GlobalConstant.HEADER_SERVICE_NAME, serviceName);
        }

        return mutatedRequest.build();
    }

    /**
     * 요청 경로에서 서비스 이름을 추출한다
     *
     * @param path 요청 경로
     * @return String 서비스 이름 (추출 실패 시 null)
     * <p>
     * 예시:
     * - /user-service/api/users/profile -> user-service
     * - /board-service/api/boards -> board-service
     * - /auth-service/api/auth/login -> auth-service
     */
    private String extractServiceNameFromPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        // 경로 정규화
        String normalizedPath = path.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        // 경로를 슬래시로 분리
        String[] pathParts = normalizedPath.split("/");

        // 빈 문자열 제거하고 첫 번째 세그먼트 추출
        for (String part : pathParts) {
            if (StringUtils.hasLength(part)) {
                // 서비스 이름 패턴 검증 (xxx-service 형태)
                if (part.endsWith("-service") || isKnownService(part)) {
                    return part;
                }
            }
        }

        // 서비스 이름을 찾지 못한 경우 기본값 또는 null 반환
        log.warn("Could not extract service name from path: {}", path);
        return null;
    }

    /**
     * 주어진 서비스명이 알려진 서비스인지 확인한다 (Java 1.8 호환)
     *
     * @param serviceName 확인할 서비스명
     * @return boolean 알려진 서비스 여부
     */
    private boolean isKnownService(String serviceName) {
        // Java 1.8 방식으로 서비스 목록 정의
        Set<String> knownServices = new HashSet<>();
        knownServices.add("user-service");
        knownServices.add("board-service");
        knownServices.add("auth-service");
        knownServices.add("portal-service");
        knownServices.add("reserve-service");
        knownServices.add("api-gateway");

        return knownServices.contains(serviceName);
    }

    @Data
    public static class Config {
        // put the configure
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;
    }
}
