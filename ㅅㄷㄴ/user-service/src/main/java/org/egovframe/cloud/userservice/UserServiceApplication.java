package org.egovframe.cloud.userservice;

import org.egovframe.cloud.userservice.config.PermissionConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * org.egovframe.cloud.userservice.UserServiceApplication
 * <p>
 * 유저 서비스 어플리케이션 클래스
 * Redis Session 기반 인증 시스템
 *
 * @author 표준프레임워크센터 jaeyeolkim
 * @version 1.0
 * @since 2021/06/30
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2021/06/30    jaeyeolkim  최초 생성
 *  2025/07/11    수정       Redis Session 기반으로 변경
 *  2025/07/12    수정       Eureka 설정 제거
 * </pre>
 */
@ComponentScan({"org.egovframe.cloud.userservice"})
//@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30분 세션 유지
@EnableConfigurationProperties(PermissionConfig.class)
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

//    @Bean
//    public BCryptPasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
}
