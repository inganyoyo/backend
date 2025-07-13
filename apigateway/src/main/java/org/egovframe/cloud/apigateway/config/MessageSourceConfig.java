package org.egovframe.cloud.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

/**
 * org.egovframe.cloud.apigateway.config.MessageSourceConfig
 * <p>
 * Spring MessageSource 설정
 * resources 디렉토리의 messages.properties 파일을 사용하여 다국어를 지원한다.
 *
 * @version 1.0
 * @since 2025/07/14
 */
@Slf4j
@Configuration
public class MessageSourceConfig {

    /**
     * MessageSource Bean을 생성하여 다국어 메시지 처리를 설정한다
     *
     * @return MessageSource 설정된 메시지 소스 객체
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        
        // resources 디렉토리의 messages.properties 사용
        messageSource.setBasenames("classpath:messages");
        
        log.info("MessageSource configured with classpath:messages");
        
        messageSource.setCacheSeconds(60); // 메세지 파일 변경 감지 간격 (개발시에만)
        messageSource.setUseCodeAsDefaultMessage(true); // 메세지가 없으면 코드를 메세지로 사용
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false); // 시스템 로케일 사용 안함
        
        return messageSource;
    }
}
