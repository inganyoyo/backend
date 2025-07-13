package org.egovframe.cloud.userservice.api;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.User;
import org.egovframe.cloud.userservice.service.AuthService;
import org.egovframe.cloud.userservice.service.AuthorizationService;
import org.springframework.http.ResponseEntity;
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
  @GetMapping("/api/v1/auth/check")
  public ResponseEntity<Boolean> checkAuthorization(
      @RequestHeader(value = "X-Service-ID", required = false) String serviceId,
      @RequestParam String httpMethod,
      @RequestParam String requestPath) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // 🆕 새로운 메서드 시그니처 사용
    boolean isAuth =
        authorizationService.isAuthorization(authentication, requestPath, httpMethod, serviceId);

    return ResponseEntity.ok(isAuth);
  }

  /** 로그인 */
  @PostMapping("/api/v1/auth/login")
  public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
    String username = loginRequest.get("username");
    String password = loginRequest.get("password");

    if (username == null || password == null) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "사용자명과 비밀번호를 입력해주세요.");
      return ResponseEntity.badRequest().body(response);
    }

    String sessionId = authService.login(username, password);

    if (sessionId == null) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "로그인 실패: 사용자명 또는 비밀번호가 잘못되었습니다.");
      return ResponseEntity.status(401).body(response);
    }

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "로그인 성공");
    response.put("sessionId", sessionId);

    return ResponseEntity.ok(response);
  }

  /** 로그아웃 */
  @PostMapping("/api/v1/auth/logout")
  public ResponseEntity<?> logout(
      @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

    if (sessionId != null) {
      authService.logout(sessionId);
    }

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "로그아웃 성공");
    return ResponseEntity.ok(response);
  }

  /** 세션 검증 (Boolean 반환 - API Gateway 호환) */
  @GetMapping("/api/v1/auth/validate")
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
  @GetMapping("/api/v1/users/profile")
  public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {

    Map<String, Object> profile = new HashMap<>();
    profile.put("userId", user.getUserId());
    profile.put("username", user.getUsername());
    profile.put("email", user.getEmail());
    profile.put("role", user.getRole());
    profile.put("loginTime", user.getLoginTime());
    profile.put("lastAccessTime", user.getLastAccessTime());

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("profile", profile);
    return ResponseEntity.ok(response);
  }

}
