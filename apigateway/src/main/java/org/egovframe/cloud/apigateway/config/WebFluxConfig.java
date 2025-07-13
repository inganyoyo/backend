package org.egovframe.cloud.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * org.egovframe.cloud.apigateway.config.WebFluxConfig
 * <p>
 * WebFlux UTF-8 인코딩 설정
 *
 * @version 1.0
 * @since 2025/07/14
 */
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    /**
     * HTTP 메시지 코덱을 구성하여 UTF-8 인코딩과 메모리 크기를 설정한다
     *
     * @param configurer 서버 코덱 설정 객체
     */
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        // JSON 코덱에 UTF-8 설정
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder());
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder());

        // 최대 메모리 크기 설정
        configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB
    }
}
