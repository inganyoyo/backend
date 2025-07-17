package org.egovframe.cloud.userservice.config;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 데이터베이스 기반 권한 관리 설정
 * 
 * @version 1.0
 * @since 2025/07/18
 */
@Slf4j
@Configuration
@EnableScheduling  // 스케줄링 활성화 (10분 간격 갱신)
@EnableAsync       // 비동기 처리 활성화 (세션 TTL 연장 등)
@MapperScan("org.egovframe.cloud.userservice.mapper")  // MyBatis 매퍼 스캔
@ConditionalOnProperty(name = "permission.mode", havingValue = "database", matchIfMissing = true)
public class DatabasePermissionConfig {

    /**
     * 설정 완료 로그
     */
    @PostConstruct
    public void init() {
        log.info("=== 데이터베이스 권한 관리 모드 활성화 ===");
        log.info("- 10분마다 자동 권한 갱신");
        log.info("- PostgreSQL 기반 권한 저장");
        log.info("- 권한 상속 지원");
        log.info("- 비동기 처리 지원");
        log.info("- MyBatis 매퍼 스캔: org.egovframe.cloud.userservice.mapper");
    }
}
