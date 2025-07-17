package org.egovframe.cloud.apigateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 설정 클래스
 * 커넥션 풀, 타임아웃, 성능 최적화 설정을 관리
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${webclient.connection-pool.max-connections:100}")
    private int maxConnections;

    @Value("${webclient.connection-pool.max-idle-time:30s}")
    private Duration maxIdleTime;

    @Value("${webclient.connection-pool.max-life-time:60s}")
    private Duration maxLifeTime;

    @Value("${webclient.connection-pool.pending-acquire-timeout:5s}")
    private Duration pendingAcquireTimeout;

    @Value("${webclient.timeout.connection:3s}")
    private Duration connectionTimeout;

    @Value("${webclient.timeout.read:10s}")
    private Duration readTimeout;

    @Value("${webclient.timeout.write:10s}")
    private Duration writeTimeout;

    @Value("${webclient.logging.enabled:false}")
    private boolean loggingEnabled;

    /**
     * 커넥션 풀 설정
     */
    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("gateway-connection-pool")
                .maxConnections(maxConnections)
                .maxIdleTime(maxIdleTime)
                .maxLifeTime(maxLifeTime)
                .pendingAcquireTimeout(pendingAcquireTimeout)
                .evictInBackground(Duration.ofSeconds(120))
                .build();
    }

    /**
     * HttpClient 설정 (Netty 기반)
     */
    @Bean
    public HttpClient httpClient(ConnectionProvider connectionProvider) {
        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectionTimeout.toMillis())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler((int) readTimeout.getSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler((int) writeTimeout.getSeconds(), TimeUnit.SECONDS))
                )
                .compress(true)  // 압축 활성화
                .keepAlive(true)  // Keep-Alive 활성화
                .wiretap(loggingEnabled);  // 로깅 설정
    }

    /**
     * WebClient.Builder 설정 (성능 최적화 적용)
     */
    @Bean
    public WebClient.Builder webClientBuilder(HttpClient httpClient) {
        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)); // 1MB

        // 로깅 필터 추가 (개발 환경에서만)
        if (loggingEnabled) {
            builder.filter(logRequest())
                   .filter(logResponse());
        }

        return builder;
    }

    /**
     * 요청 로깅 필터
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) -> 
                    log.debug("Request Header: {}={}", name, values)
                );
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * 응답 로깅 필터
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("Response Status: {}", clientResponse.statusCode());
                clientResponse.headers().asHttpHeaders().forEach((name, values) -> 
                    log.debug("Response Header: {}={}", name, values)
                );
            }
            return Mono.just(clientResponse);
        });
    }
}
