package com.example.demo.common.config;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * org.egovframe.cloud.apigateway.config.MessageSourceConfig
 * <p>
 * Spring MessageSource 설정
 * resources 디렉토리의 messages.properties 파일을 사용하여 다국어를 지원한다.
 *
 * @author 표준프레임워크센터 jaeyeolkim
 * @version 1.0
 * @since 2021/08/09
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2021/08/09    jaeyeolkim  최초 생성
 *  2025/07/13    수정자      resources 디렉토리 사용으로 변경
 * </pre>
 */
@Slf4j
@Configuration
public class MessageSourceConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        // resources 디렉토리의 common-messages.properties 사용
        messageSource.setBasenames("classpath:messages", "classpath:common-messages");

        log.info("MessageSource configured with classpath:common-messages");

        messageSource.setCacheSeconds(60); // 메세지 파일 변경 감지 간격 (개발시에만)
        messageSource.setUseCodeAsDefaultMessage(true); // 메세지가 없으면 코드를 메세지로 사용
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false); // 시스템 로케일 사용 안함

        return messageSource;
    }
}
