package org.egovframe.cloud.userservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.User;
import org.egovframe.cloud.userservice.dto.AuthResult;
import org.egovframe.cloud.userservice.service.AuthService;
import org.egovframe.cloud.userservice.service.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 통합 인증/권한/사용자 API Controller
 * 로그인, 로그아웃, 세션 검증, 권한 체크, 사용자 프로필 관리 등 모든 인증 관련 기능 제공
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final AuthorizationService authorizationService;
    
    // ========== 기본 인증 기능 ==========
    
    /**
     * 로그인
     */
    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        if (username == null || password == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사용자명과 비밀번호를 입력해주세요.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        String sessionId = authService.login(username, password);
        
        if (sessionId == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "로그인 실패: 사용자명 또는 비밀번호가 잘못되었습니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그인 성공");
        response.put("sessionId", sessionId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 로그아웃
     */
    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
        if (sessionId != null) {
            authService.logout(sessionId);
        }
        
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("message", "로그아웃 성공");
        return ResponseEntity.ok(successResponse);
    }
    
    // ========== Gateway 인증/권한 기능 ==========
    
    /**
     * Gateway용 - 인증 + 권한 체크를 한 번에 처리
     * 캐시 최적화된 세션 검증과 권한 체크
     */
    @GetMapping("/api/v1/auth/validate-and-authorize")
    public ResponseEntity<?> validateAndAuthorize(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestParam String httpMethod,
            @RequestParam String requestPath,
            @RequestParam(value = "responseType", defaultValue = "detailed") String responseType) {
        
        log.debug("Gateway 인증/권한 요청: sessionId={}, method={}, path={}, responseType={}", 
                sessionId, httpMethod, requestPath, responseType);
        
        // 1. 세션 검증 (캐시 최적화됨)
        if (sessionId == null || sessionId.trim().isEmpty()) {
            if ("simple".equals(responseType)) {
                return ResponseEntity.ok(false);
            }
            return ResponseEntity.ok(AuthResult.unauthenticated());
        }
        
        User user = authService.getUser(sessionId);
        if (user == null) {
            log.debug("유효하지 않은 세션: {}", sessionId);
            if ("simple".equals(responseType)) {
                return ResponseEntity.ok(false);
            }
            return ResponseEntity.ok(AuthResult.unauthenticated());
        }
        
        // 2. 권한 체크 (🟢 User 객체를 직접 전달하여 중복 호출 방지)
        boolean hasPermission = authorizationService.checkPermission(user, requestPath, httpMethod);
        
        // 🟢 응답 타입에 따라 다른 형태로 반환
        if ("simple".equals(responseType)) {
            // Boolean 반환 (기존 /authorizations/check 호환)
            return ResponseEntity.ok(hasPermission);
        } else {
            // AuthResult 반환 (상세 정보 포함)
            if (hasPermission) {
                log.debug("인증/권한 성공: user={}, method={}, path={}", 
                        user.getUserId(), httpMethod, requestPath);
                return ResponseEntity.ok(AuthResult.authorized(user.getUserId(), user.getRole()));
            } else {
                log.debug("권한 없음: user={}, method={}, path={}", 
                        user.getUserId(), httpMethod, requestPath);
                return ResponseEntity.ok(AuthResult.unauthorized(user.getUserId(), user.getRole()));
            }
        }
    }
    
    /**
     * 세션 상태만 체크 (빠른 검증용)
     */
    @GetMapping("/api/v1/auth/validate")
    public ResponseEntity<AuthResult> validateSession(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return ResponseEntity.ok(AuthResult.unauthenticated());
        }
        
        User user = authService.getUser(sessionId);
        if (user == null) {
            return ResponseEntity.ok(AuthResult.unauthenticated());
        }
        
        return ResponseEntity.ok(AuthResult.authorized(user.getUserId(), user.getRole()));
    }
    
    // ========== 사용자 정보 관리 ==========
    
    /**
     * 사용자 프로필 조회
     */
    @GetMapping("/api/v1/users/profile")
    public ResponseEntity<?> getProfile(@RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        if (sessionId == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "세션 ID가 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        User user = authService.getUser(sessionId);
        if (user == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "유효하지 않은 세션입니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getUserId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("loginTime", user.getLoginTime());
        profile.put("lastAccessTime", user.getLastAccessTime());
        
        Map<String, Object> profileResponse = new HashMap<>();
        profileResponse.put("success", true);
        profileResponse.put("profile", profile);
        return ResponseEntity.ok(profileResponse);
    }
    
    // ========== 권한 검증 기능 (하위 호환성) ==========
    
    /**
     * 권한 체크 (기존 API Gateway 호환용)
     * @deprecated /validate-and-authorize?responseType=simple 사용 권장
     */
    @GetMapping("/api/v1/auth/check")
    @Deprecated
    public ResponseEntity<Boolean> checkAuthorization(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestParam String httpMethod,
            @RequestParam String requestPath) {
        
        log.info("권한 체크 요청: sessionId={}, httpMethod={}, requestPath={}", 
                sessionId, httpMethod, requestPath);
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("세션 ID가 없음");
            return ResponseEntity.ok(false);
        }
        
        boolean hasPermission = authorizationService.checkPermission(sessionId, requestPath, httpMethod);
        
        return ResponseEntity.ok(hasPermission);
    }
}
