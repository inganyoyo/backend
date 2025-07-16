package org.egovframe.cloud.userservice.config;

/**
 * Security 관련 상수 정의
 * SecurityConfig와 AuthorizationService에서 공통으로 사용
 */
public final class SecurityConstants {
    
    /**
     * 인증 없이 접근 가능한 경로 패턴들
     * 🆕 user-service에서 공개 메뉴 관리 - API Gateway에서 모든 요청을 받아 여기서 판단
     */
    public static final String[] PERMIT_ALL_PATTERNS = {
            "/actuator/**",        // 헬스체크
            "/v3/api-docs/**",     // API 문서
            "/swagger*/**",        // Swagger
            "/webjars/**",         // Swagger 리소스
            "/error",              // 에러 페이지 허용
            "/api/auth/check",     // 🆕 Gateway 권한 체크 API (내부 API)
            "/api/auth/login",     // 로그인 API
            "/api/auth/logout",    // 로그아웃 API
            "/test",               // 테스트 페이지
            "/test/**",            // 테스트 하위 페이지
            // 🆕 추가 공개 메뉴 경로들 (필요에 따라 추가/수정)
            "/api/boards/public/**",  // 공개 게시판 (읽기 전용)
            "/api/notices/**",        // 공지사항 (읽기 전용) 
            "/api/files/public/**",   // 공개 파일 다운로드
    };
    
    private SecurityConstants() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }
}
