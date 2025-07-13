package org.egovframe.cloud.userservice.service;

import static org.egovframe.cloud.userservice.config.SecurityConstants.PERMIT_ALL_PATTERNS;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.config.PermissionConfig;
import org.egovframe.cloud.userservice.domain.User;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/** 권한 검증 서비스 API Gateway에서 X-Service-ID 헤더를 통해 서비스 식별 정보를 제공받아 권한을 검증합니다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService extends EgovAbstractServiceImpl {

  // Gateway에서 전달하는 서비스 ID 헤더명
  private static final String SERVICE_ID_HEADER = "X-Service-ID";
  private static final String DEFAULT_SERVICE_ID = "user-service";
  private final AuthService authService;
  private final PermissionConfig permissionConfig;

  /**
   * 🆕 Spring Security에서 호출되는 권한 검증 메서드 SecurityFilterChain의 access() 메서드에서 사용하는 SpEL 표현식용
   *
   * @param authentication Spring Security Authentication 객체
   * @param requestPath 요청 경로
   * @param httpMethod HTTP 메서드
   * @param serviceId 서비스 ID (X-Service-ID 헤더)
   * @return 권한 여부
   */
  public boolean isAuthorization(
      Authentication authentication, String requestPath, String httpMethod, String serviceId) {

    // Authentication에서 권한 목록 추출
    List<String> roles =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(this::cleanRole) // ROLE_ 접두사 제거
            .collect(Collectors.toList());

    return checkPermission(roles, serviceId, httpMethod, requestPath);
  }

  /**
   * 세션 기반 권한 검증
   *
   * @param sessionId 세션 ID
   * @param serviceId 서비스 ID (Gateway에서 검증됨)
   * @param requestPath 요청 경로
   * @param httpMethod HTTP 메서드
   * @return 권한 여부
   */
  public boolean hasPermissionBySession(
      String sessionId, String serviceId, String requestPath, String httpMethod) {
    if (sessionId == null || sessionId.trim().isEmpty()) {
      log.warn("세션 ID가 비어있음");
      return false;
    }

    // 세션에서 사용자 정보 조회
    User user = authService.getUser(sessionId);
    if (user == null) {
      log.warn("유효하지 않은 세션: {}", sessionId);
      return false;
    }

    // 서비스 ID 검증 및 기본값 설정
    String validServiceId =
        (serviceId != null && !serviceId.trim().isEmpty()) ? serviceId.trim() : DEFAULT_SERVICE_ID;

    return checkPermission(
        Collections.singletonList(user.getRole()), validServiceId, httpMethod, requestPath);
  }

  /**
   * 사용자 ID 기반 권한 검증 (DB에서 조회)
   *
   * @param userId 사용자 ID
   * @param serviceId 서비스 ID
   * @param httpMethod HTTP 메서드
   * @param requestPath 요청 경로
   * @return 권한 여부
   */
  public boolean hasPermissionByUserId(
      String userId, String serviceId, String httpMethod, String requestPath) {
    if (userId == null || userId.trim().isEmpty()) {
      log.warn("사용자 ID가 비어있음");
      return false;
    }

    // DB에서 사용자 정보 조회
    User user = authService.getUserByUsername(userId);
    if (user == null) {
      log.warn("사용자를 찾을 수 없음: {}", userId);
      return false;
    }

    // 서비스 ID 검증 및 기본값 설정
    String validServiceId =
        (serviceId != null && !serviceId.trim().isEmpty()) ? serviceId.trim() : DEFAULT_SERVICE_ID;

    return checkPermission(
        Collections.singletonList(user.getRole()), validServiceId, httpMethod, requestPath);
  }

  /**
   * 핵심 권한 검증 로직
   *
   * @param roles 사용자 역할 목록
   * @param serviceId 서비스 ID (Gateway에서 검증됨)
   * @param httpMethod HTTP 메서드
   * @param requestPath 요청 경로
   * @return 권한 여부
   */
  private boolean checkPermission(
      List<String> roles, String serviceId, String httpMethod, String requestPath) {
    if (roles == null || roles.isEmpty()) {
      log.warn("역할 목록이 비어있음");
      return false;
    }

    // 경로 정규화
    String normalizedPath = normalizePath(requestPath);

    // 각 역할에 대해 권한 체크 (하나라도 권한이 있으면 허용)
    for (String role : roles) {
      boolean hasPermission =
          permissionConfig.hasPermission(role, serviceId, httpMethod, normalizedPath);

      if (hasPermission) {
        //                log.info("권한 검증 성공: 역할[{}], 서비스[{}], 메소드[{}], 경로[{}]",
        //                        role, serviceId, httpMethod, normalizedPath);
        return true;
      }
    }

    log.warn(
        "권한 검증 실패: 역할목록[{}], 서비스[{}], 메소드[{}], 경로[{}]",
        roles,
        serviceId,
        httpMethod,
        normalizedPath);
    return false;
  }

  /**
   * Spring Security 역할명에서 ROLE_ 접두사 제거
   *
   * @param role 원본 역할명
   * @return 정제된 역할명
   */
  private String cleanRole(String role) {
    return role.startsWith("ROLE_") ? role.substring(5) : role;
  }

  /**
   * 요청 경로 정규화
   *
   * @param path 원본 경로
   * @return 정규화된 경로
   */
  private String normalizePath(String path) {
    if (path == null || path.trim().isEmpty()) {
      return "/";
    }

    String normalized = path.trim();
    if (!normalized.startsWith("/")) {
      normalized = "/" + normalized;
    }

    return normalized;
  }

  /**
   * Public 경로 체크 (SecurityConfig의 PERMIT_ALL_PATTERNS과 동일하게 유지)
   *
   * @param requestPath 요청 경로
   * @return public 경로 여부
   */
  private boolean isPublicPath(String requestPath) {
    // SecurityConstants에 정의된 PERMIT_ALL_PATTERNS 사용하여 Ant 패턴 매칭
    for (String pattern : PERMIT_ALL_PATTERNS) {
      if (matchesAntPattern(pattern, requestPath)) {
        return true;
      }
    }

    return false;
  }

  /**
   * 간단한 Ant 패턴 매칭 구현
   *
   * @param pattern Ant 패턴 (예: /api/v1/auth/**)
   * @param path 실제 경로 (예: /api/v1/auth/login)
   * @return 매칭 여부
   */
  private boolean matchesAntPattern(String pattern, String path) {
    if (pattern.endsWith("/**")) {
      // /** 패턴의 경우 prefix 매칭
      String prefix = pattern.substring(0, pattern.length() - 3);
      return path.startsWith(prefix);
    } else if (pattern.endsWith("*/**")) {
      // swagger*/** 같은 패턴
      String prefix = pattern.substring(0, pattern.length() - 4);
      return path.startsWith(prefix);
    } else {
      // 정확한 매칭
      return pattern.equals(path);
    }
  }
}
