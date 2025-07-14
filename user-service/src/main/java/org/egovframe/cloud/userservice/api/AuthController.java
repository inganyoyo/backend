package org.egovframe.cloud.userservice.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.common.dto.ApiResponse;
import org.egovframe.cloud.common.util.ResponseUtil;
import org.egovframe.cloud.userservice.domain.User;
import org.egovframe.cloud.userservice.dto.AuthCheckResponse;
import org.egovframe.cloud.userservice.service.AuthService;
import org.egovframe.cloud.userservice.service.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/** 간단한 인증 API Controller 헤더 기반 권한 검증 지원 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final AuthorizationService authorizationService;

  /**
   * 권한 체크 (API Gateway용 - SecurityContext 기반) AuthenticationFilter에서 이미 SecurityContext가 설정되어 있음
   */
  @GetMapping("/api/auth/check")
  public ResponseEntity<AuthCheckResponse> checkAuthorization(
      @RequestHeader(value = "X-Service-ID", required = false) String serviceId,
      @RequestParam String httpMethod,
      @RequestParam String requestPath) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    log.info(authentication.toString() + " - " + requestPath + " - " + httpMethod + " - " + serviceId);

    boolean isAuth = authorizationService.isAuthorization(authentication, requestPath, httpMethod, serviceId);

    User user = null;
    if (authentication instanceof UsernamePasswordAuthenticationToken) {
      Object principal = authentication.getPrincipal();
      if (principal instanceof User) {
        user = (User) principal;
      }
    }

    return ResponseEntity.ok(AuthCheckResponse.builder()
            .isAuthorized(isAuth)
            .user(user)
            .build());
  }

  /** 로그인 */
  @PostMapping("/api/auth/login")
  public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> loginRequest) {
    String username = loginRequest.get("username");
    String password = loginRequest.get("password");

    if (username == null || password == null) {
      return ResponseUtil.badRequest("사용자명과 비밀번호를 입력해주세요.");
    }

    String sessionId = authService.login(username, password);
    log.info("sessionId "+sessionId);
    if (sessionId == null) {
      return ResponseUtil.unauthorized("로그인 실패: 사용자명 또는 비밀번호가 잘못되었습니다.");
    }

    Map<String, Object> loginData = new HashMap<>();
    loginData.put("sessionId", sessionId);
    loginData.put("username", username);

    return ResponseUtil.ok("로그인 성공", loginData);
  }

  /** 로그아웃 */
  @PostMapping("/api/auth/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

    if (sessionId != null) {
      authService.logout(sessionId);
    }

    return ResponseUtil.ok("로그아웃 성공");
  }

  /** 세션 검증 (Boolean 반환 - API Gateway 호환성 유지) */
  @GetMapping("/api/auth/validate")
  public ResponseEntity<Boolean> validate(
      @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

    if (sessionId == null || sessionId.trim().isEmpty()) {
      return ResponseEntity.ok(false);
    }

    User user = authService.getUser(sessionId);
    log.info(user.toString());
    boolean isValid = (user != null);

    log.debug("세션 검증: sessionId={}, valid={}", sessionId.substring(0, 8) + "...", isValid);
    return ResponseEntity.ok(isValid);
  }

  /** 사용자 프로필 조회 (Spring Security로 인증 처리됨) */
  @GetMapping("/api/users/profile")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(@AuthenticationPrincipal User user) {

    Map<String, Object> profile = new HashMap<>();
    profile.put("userId", user.getUserId());
    profile.put("username", user.getUsername());
    profile.put("email", user.getEmail());
    profile.put("role", user.getRole());
    profile.put("loginTime", user.getLoginTime());
    profile.put("lastAccessTime", user.getLastAccessTime());

    return ResponseUtil.ok("프로필 조회 성공", profile);
  }

  /** User Service 상태 확인 엔드포인트 */
  @GetMapping("/api/users/status")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getUserServiceStatus() {
    log.info("User service status check requested");
    
    Map<String, Object> status = new HashMap<>();
    status.put("service", "user-service");
    status.put("status", "UP");
    status.put("timestamp", java.time.LocalDateTime.now());
    status.put("version", "1.0.0");
    
    return ResponseUtil.ok("User Service가 정상적으로 작동 중입니다.", status);
  }

  /** User Service 헬스체크 엔드포인트 */
  @GetMapping("/api/users/health")
  public ResponseEntity<ApiResponse<String>> healthCheck() {
    log.info("User service health check requested");
    return ResponseUtil.ok("User Service Health Check", "OK");
  }

  /** 세션 정보 조회 (ApiResponse 형태) */
  @GetMapping("/api/auth/session-info")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionInfo(
      @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
    
    if (sessionId == null || sessionId.trim().isEmpty()) {
      return ResponseUtil.badRequest("세션 ID가 필요합니다.");
    }

    User user = authService.getUser(sessionId);
    if (user == null) {
      return ResponseUtil.unauthorized("유효하지 않은 세션입니다.");
    }

    Map<String, Object> sessionInfo = new HashMap<>();
    sessionInfo.put("sessionId", sessionId);
    sessionInfo.put("userId", user.getUserId());
    sessionInfo.put("username", user.getUsername());
    sessionInfo.put("role", user.getRole());
    sessionInfo.put("loginTime", user.getLoginTime());
    sessionInfo.put("lastAccessTime", user.getLastAccessTime());
    sessionInfo.put("isValid", true);
    
    return ResponseUtil.ok("세션 정보 조회 성공", sessionInfo);
  }

  /** 전체 사용자 목록 조회 (테스트용) */
  @GetMapping("/api/users")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsers(@AuthenticationPrincipal User currentUser) {
    log.info("전체 사용자 목록 조회 요청");
    
    // 실제로는 UserService나 Repository에서 조회해야 하지만, 
    // 테스트용으로 더미 데이터 반환
    Map<String, Object> usersData = new HashMap<>();
    usersData.put("totalCount", 3);
    usersData.put("users", Arrays.asList(
        Map.of("userId", "user1", "username", "사용자1", "email", "user1@test.com", "role", "USER"),
        Map.of("userId", "user2", "username", "사용자2", "email", "user2@test.com", "role", "USER"),
        Map.of("userId", "admin", "username", "관리자", "email", "admin@test.com", "role", "ADMIN")
    ));
    
    return ResponseUtil.ok("전체 사용자 조회 성공", usersData);
  }

}