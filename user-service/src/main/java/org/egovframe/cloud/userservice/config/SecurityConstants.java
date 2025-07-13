package org.egovframe.cloud.userservice.config;

/**
 * Security 관련 상수 정의
 * SecurityConfig와 AuthorizationService에서 공통으로 사용
 */
public final class SecurityConstants {
    
    private SecurityConstants() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }
    
    /**
     * 인증 없이 접근 가능한 경로 패턴들
     * SecurityConfig의 permitAll()과 AuthorizationService의 public 경로 체크에서 공통 사용
     */
    public static final String[] PERMIT_ALL_PATTERNS = {
            "/api/v1/auth/**",     // 인증 API 허용
            "/api/v1/authorizations/**", // 권한 체크 API 허용 (Gateway에서 호출)
            "/actuator/**",        // 헬스체크
            "/v3/api-docs/**",     // API 문서
            "/swagger*/**",        // Swagger
            "/webjars/**",         // Swagger 리소스
            "/error"               // 에러 페이지 허용
    };
}
