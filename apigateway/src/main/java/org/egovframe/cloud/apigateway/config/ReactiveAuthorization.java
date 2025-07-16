package org.egovframe.cloud.apigateway.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.apigateway.dto.AuthCheckResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * org.egovframe.cloud.apigateway.config.ReactiveAuthorization
 * <p>
 * Spring Security 에 의해 요청 url에 대한 사용자 인가 서비스를 수행하는 클래스
 * X-Session-ID 헤더를 auth-service로 전달하여 사용자의 권한여부 체크하여 true/false 리턴한다
 *
 * @version 1.0
 * @since 2025/07/14
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ReactiveAuthorization implements ReactiveAuthorizationManager<AuthorizationContext> {

    // auth-service의 인증/인가 API
    public static final String AUTHORIZATION_URI = "/auth-service" + "/api/auth/check";
    @Value("${auth-service.url:http://localhost:8001}")
    private String AUTH_SERVICE_URL;

    /**
     * 요청에 대한 사용자의 권한여부를 체크하여 AuthorizationDecision을 반환한다
     * X-Session-ID 헤더를 auth-service로 전달하여 인증/인가 처리한다
     *
     * @param authentication 인증 정보
     * @param context 권한 부여 컨텍스트
     * @return Mono<AuthorizationDecision> 권한 부여 결정 객체
     * @see WebFluxSecurityConfig
     */
    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication,
                                             AuthorizationContext context) {
        log.info("ReactiveAuthorization start");
        ServerHttpRequest request = context.getExchange().getRequest();
        RequestPath requestPath = request.getPath();
        HttpMethod httpMethod = request.getMethod();

        // 경로에서 서비스명 추출 및 실제 경로 분리
        String fullPath = requestPath.toString();

        ServicePathResult serviceAndPath = extractServiceAndPath(fullPath);

        // auth-service에는 실제 경로만 전달 (서비스명 제거)
        String baseUrl = AUTH_SERVICE_URL + "/api/auth/check"
                + "?httpMethod=" + httpMethod
                + "&requestPath=" + serviceAndPath.getRequestPath();

        String sessionId = "";

        // 쿠키에서 직접 GSNS-SESSION 추출
        if (request.getCookies().containsKey("GSNS-SESSION")) {
            sessionId = request.getCookies().getFirst("GSNS-SESSION").getValue();
        }

        String finalSessionId = sessionId;
        String serviceName = serviceAndPath.getServiceName();

        // 🆕 완전 비동기 처리로 변경
        return WebClient.create(baseUrl)
                .get()
                .headers(httpHeaders -> {
                    if (StringUtils.hasLength(finalSessionId)) {
                        httpHeaders.add(GlobalConstant.SESSION_HEADER_NAME, finalSessionId);
                    }
                    if (StringUtils.hasLength(serviceName)) {
                        httpHeaders.add(GlobalConstant.HEADER_SERVICE_NAME, serviceName);
                    }
                })
                .retrieve()
                .bodyToMono(AuthCheckResponse.class)
                .map(authResponse -> {
                    boolean granted = authResponse.isAuthorized();
                    log.info("authResponse: granted={}", granted);
                    
                    if (granted && authResponse.getUser() != null) {
                        // 🆕 사용자 정보 로깅
                        log.info("Authenticated user: userId={}, role={}", 
                                authResponse.getUser().getUserId(),
                                authResponse.getUser().getRole());

                        // 🆕 Exchange에 사용자 정보 저장
                        context.getExchange().getAttributes().put("USER_INFO", authResponse.getUser());
                        log.info("User info stored in exchange");
                    }
                    
                    log.info("Security AuthorizationDecision granted={}", granted);
                    return new AuthorizationDecision(granted);
                })
                .onErrorResume(throwable -> {
                    log.error("인가 서비스 호출 실패: {}", throwable.getMessage());
                    return Mono.just(new AuthorizationDecision(false));
                });
    }

    /**
     * 전체 경로에서 서비스명과 실제 경로를 분리한다
     *
     * @param fullPath 전체 경로 (예: /user-service/api/users/profile)
     * @return ServicePathResult 서비스명과 요청 경로를 포함한 결과 객체
     */
    private ServicePathResult extractServiceAndPath(String fullPath) {

        if (fullPath == null || fullPath.trim().isEmpty()) {
            return new ServicePathResult("unknown-service", "/");
        }

        // 경로 정규화
        String normalizedPath = fullPath.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        // 경로를 슬래시로 분리
        String[] pathParts = normalizedPath.split("/");

        // 빈 문자열 제거
        List<String> filteredParts = new ArrayList<>();
        for (String part : pathParts) {
            if (StringUtils.hasLength(part)) {
                filteredParts.add(part);
            }
        }

        if (filteredParts.isEmpty()) {
            return new ServicePathResult("unknown-service", normalizedPath);
        }

        // 첫 번째 부분이 서비스명인지 확인
        String firstPart = filteredParts.get(0);
        if (isKnownService(firstPart)) {
            // 서비스명 발견 - 나머지 경로 재구성
            String serviceName = firstPart;
            String requestPath = "/";

            if (filteredParts.size() > 1) {
                // 서비스명 이후의 경로들 재조합
                List<String> pathSegments = filteredParts.subList(1, filteredParts.size());
                requestPath = "/" + String.join("/", pathSegments);
            }

            return new ServicePathResult(serviceName, requestPath);
        } else {
            // 서비스명 없음 - 전체 경로를 그대로 사용
            return new ServicePathResult("unknown-service", normalizedPath);
        }
    }

    /**
     * 주어진 서비스명이 알려진 서비스인지 확인한다
     *
     * @param serviceName 확인할 서비스명
     * @return boolean 알려진 서비스 여부
     */
    private boolean isKnownService(String serviceName) {
        return GlobalConstant.isKnownService(serviceName);
    }

    /**
     * 서비스명과 경로를 담는 결과 클래스
     */
    private static class ServicePathResult {
        private final String serviceName;
        private final String requestPath;

        /**
         * ServicePathResult 생성자
         *
         * @param serviceName 서비스명
         * @param requestPath 요청 경로
         */
        public ServicePathResult(String serviceName, String requestPath) {
            this.serviceName = serviceName;
            this.requestPath = requestPath;
        }

        /**
         * 서비스명을 반환한다
         *
         * @return String 서비스명
         */
        public String getServiceName() {
            return serviceName;
        }

        /**
         * 요청 경로를 반환한다
         *
         * @return String 요청 경로
         */
        public String getRequestPath() {
            return requestPath;
        }
    }
}
