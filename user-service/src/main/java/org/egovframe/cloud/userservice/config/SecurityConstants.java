package org.egovframe.cloud.userservice.config;

/**
 * Security 관련 상수 정의
 * SecurityConfig와 AuthorizationService에서 공통으로 사용
 */
public final class SecurityConstants {
    
    /**
     * 인증 없이 접근 가능한 경로 패턴들
     * 🆕 시스템 필수 경로만 포함 (Auth API는 ANONYMOUS 권한으로 관리)
     */
    public static final String[] PERMIT_ALL_PATTERNS = {
            "/actuator/**",        // 헬스체크
            "/v3/api-docs/**",     // API 문서
            "/swagger*/**",        // Swagger
            "/webjars/**",         // Swagger 리소스
            "/error",              // 에러 페이지 허용
            "/api/auth/check"   // 🆕 Gateway 권한 체크 API (내부 API)
    };
    
    private SecurityConstants() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }
}
