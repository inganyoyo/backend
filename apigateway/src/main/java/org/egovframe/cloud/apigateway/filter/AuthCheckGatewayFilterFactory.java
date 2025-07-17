package org.egovframe.cloud.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.apigateway.config.GlobalConstant;
import org.egovframe.cloud.apigateway.domain.User;
import org.egovframe.cloud.apigateway.dto.ApiResponse;
import org.egovframe.cloud.apigateway.dto.AuthCheckResponse;
import org.egovframe.cloud.apigateway.dto.ServicePathResult;
import org.egovframe.cloud.apigateway.exception.dto.ErrorCode;
import org.egovframe.cloud.apigateway.utils.MessageUtil;
import org.egovframe.cloud.apigateway.utils.PathExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class AuthCheckGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthCheckGatewayFilterFactory.Config> {
    
    @Value("${auth-service.url:http://localhost:8001}")
    private String AUTH_SERVICE_URL;

    private final String SESSION_COOKIE_NAME = "GSNS-SESSION";
    private final String LOGIN_URI = "/user-service/api/auth/login";
    private final String LOGOUT_URI = "/user-service/api/auth/logout";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final MessageUtil messageUtil;

    public AuthCheckGatewayFilterFactory(WebClient.Builder webClientBuilder, 
                                         ObjectMapper objectMapper, 
                                         MessageUtil messageUtil) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.messageUtil = messageUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            log.info("[AuthCheckGatewayFilter Start] request ID: {}, method: {}, path: {}",
                request.getId(), request.getMethod(), request.getPath());
            
            String path = request.getPath().value();
            String httpMethod = request.getMethodValue();

            ServicePathResult serviceAndPath = PathExtractor.extractServiceAndPath(path);

            String sessionCookie = Optional.ofNullable(exchange.getRequest().getCookies().getFirst(SESSION_COOKIE_NAME))
                    .map(HttpCookie::getValue)
                    .orElse(null);

            String baseUrl = AUTH_SERVICE_URL + "/api/auth/check"
                + "?httpMethod=" + httpMethod
                + "&requestPath=" + serviceAndPath.getRequestPath();

            if (path.equals(LOGIN_URI) || path.equals(LOGOUT_URI)) {
                log.info("[AuthCheckGatewayFilter Skip] Skipping auth check for path: {}", path);
                log.info("[AuthCheckGatewayFilter Skip] Cookies: {}", exchange.getRequest().getCookies());
                return chain.filter(exchange);
            }

            log.info("[AuthCheckGatewayFilter Process] Processing auth check for path: {}", path);

            // AUTH-SERVICE로 세션 검증 요청
            return webClient.get()
                    .uri(baseUrl)
                    .headers(httpHeaders -> {
                        if (StringUtils.hasLength(serviceAndPath.getServiceName())) {
                            httpHeaders.add(GlobalConstant.HEADER_SERVICE_NAME, serviceAndPath.getServiceName());
                        }
                    })
                    .cookies(cookies -> {
                        if (sessionCookie != null) {
                            cookies.add(SESSION_COOKIE_NAME, sessionCookie);
                        }
                    })
                    .retrieve()
                    .bodyToMono(AuthCheckResponse.class)
                    .onErrorResume(WebClientException.class, e -> {
                        log.error("Auth service call failed", e);
                        // 서비스 호출 실패 시 503 상태로 직접 에러 응답
                        return Mono.just(AuthCheckResponse.builder()
                                .status(503)
                                .isAuthorized(false)
                                .build());
                    })
                    .flatMap(authCheckResponse -> {
                        log.info("[AuthCheckGatewayFilter] authCheckResponse: {}", authCheckResponse);
                        int status = authCheckResponse.getStatus();

                        if (status == 200 && authCheckResponse.isAuthorized()) {
                            User user = authCheckResponse.getUser();

                            // 사용자 정보 헤더에 추가
                            ServerHttpRequest mutatedRequest = exchange.getRequest()
                                    .mutate()
                                    .header("X-User-ID", user.getUserId())
                                    .header("X-User-Role", user.getRole())
                                    .header("X-User-Email", user.getEmail())
                                    .header("X-Username", user.getUsername())
                                    .build();

                            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                            return chain.filter(mutatedExchange);
                        } else {
                            // 일관된 JSON 에러 응답 생성
                            return createErrorResponse(exchange, status);
                        }
                    });
        };
    }

    /**
     * 일관된 에러 응답을 생성한다
     * GatewayErrorWebExceptionHandler와 동일한 형태의 JSON 응답을 생성
     *
     * @param exchange ServerWebExchange 객체
     * @param status HTTP 상태 코드
     * @return Mono<Void> 에러 응답 처리 결과
     */
    private Mono<Void> createErrorResponse(ServerWebExchange exchange, int status) {
        ServerHttpResponse response = exchange.getResponse();
        
        // 상태 코드에 따른 ErrorCode 결정
        ErrorCode errorCode = determineErrorCode(status);
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        
        // 에러 메시지 생성
        String message = messageUtil.getMessage(errorCode.getMessage());
        
        // ApiResponse 구조 사용 (GatewayErrorWebExceptionHandler와 동일)
        ApiResponse<Void> apiResponse = ApiResponse.error(message, errorCode.getCode());
        
        // 완전 비동기 JSON 직렬화 및 응답 처리
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(apiResponse))
                .map(json -> response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8)))
                .flatMap(buffer -> response.writeWith(Mono.just(buffer)))
                .onErrorResume(JsonProcessingException.class, e -> {
                    log.error("Error writing JSON response", e);
                    String fallbackJson = String.format(
                        "{\"success\":false,\"message\":\"%s\",\"errorCode\":\"%s\",\"timestamp\":\"%s\"}",
                        message.replace("\"", "\\\""), errorCode.getCode(), LocalDateTime.now()
                    );
                    DataBuffer fallbackBuffer = response.bufferFactory().wrap(fallbackJson.getBytes(StandardCharsets.UTF_8));
                    return response.writeWith(Mono.just(fallbackBuffer));
                });
    }

    /**
     * 상태 코드에 따라 적절한 ErrorCode를 결정한다
     *
     * @param status HTTP 상태 코드
     * @return ErrorCode 매핑된 에러 코드
     */
    private ErrorCode determineErrorCode(int status) {
        switch (status) {
            case 401:
                return ErrorCode.UNAUTHORIZED;
            case 403:
                return ErrorCode.ACCESS_DENIED;
            case 404:
                return ErrorCode.NOT_FOUND;
            case 503:
                return ErrorCode.SERVICE_UNAVAILABLE;
            default:
                return ErrorCode.INTERNAL_SERVER_ERROR;
        }
    }

    @Data
    public static class Config {
        private boolean preLogger;
        private boolean postLogger;
    }
}
