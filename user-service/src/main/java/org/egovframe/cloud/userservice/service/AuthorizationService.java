package org.egovframe.cloud.userservice.service;

import static org.egovframe.cloud.userservice.config.SecurityConstants.PERMIT_ALL_PATTERNS;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.config.PermissionJsonConfig;
import org.egovframe.cloud.userservice.domain.User;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/** 
 * 권한 검증 서비스 
 * API Gateway에서 X-Service-ID 헤더를 통해 서비스 식별 정보를 제공받아 권한을 검증합니다.
 * 🆕 DB 기반과 JSON 기반을 조건부로 선택
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService extends EgovAbstractServiceImpl {

  // Gateway에서 전달하는 서비스 ID 헤더명
  private static final String DEFAULT_SERVICE_ID = "user-service";
  
  private final AuthService authService;
  private final PermissionJsonConfig permissionJsonConfig; // JSON 기반 권한 (기본)
  
  @Autowired(required = false)
  private DatabasePermissionService databasePermissionService; // DB 기반 권한 (선택적)

  /**
   * 🆕 Spring Security에서 호출되는 권한 검증 메서드 
   * SecurityFilterChain의 access() 메서드에서 사용하는 SpEL 표현식용
   *
   * @param authentication Spring Security Authentication 객체 (null일 수 있음)
   * @param requestPath 요청 경로
   * @param httpMethod HTTP 메서드
   * @param serviceId 서비스 ID (X-Service-ID 헤더)
   * @return 권한 여부
   */
  public boolean isAuthorization(
      Authentication authentication, String requestPath, String httpMethod, String serviceId) {

    // 모든 사용자의 권한 추출 (ANONYMOUS, USER, ADMIN 등)
    List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(this::cleanRole) // ROLE_ 접두사 제거
            .collect(Collectors.toList());

    return checkPermission(roles, serviceId, httpMethod, requestPath);
  }

//  /**
//   * 사용자 ID 기반 권한 검증 (DB에서 조회)
//   *
//   * @param userId 사용자 ID
//   * @param serviceId 서비스 ID
//   * @param httpMethod HTTP 메서드
//   * @param requestPath 요청 경로
//   * @return 권한 여부
//   */
//  public boolean hasPermissionByUserId(
//      String userId, String serviceId, String httpMethod, String requestPath) {
//    if (userId == null || userId.trim().isEmpty()) {
//      log.warn("사용자 ID가 비어있음");
//      return false;
//    }
//
//    // DB에서 사용자 정보 조회
//    User user = authService.getUserByUsername(userId);
//    if (user == null) {
//      log.warn("사용자를 찾을 수 없음: {}", userId);
//      return false;
//    }
//
//    // 서비스 ID 검증 및 기본값 설정
//    String validServiceId =
//        (serviceId != null && !serviceId.trim().isEmpty()) ? serviceId.trim() : DEFAULT_SERVICE_ID;
//
//    return checkPermission(
//        Collections.singletonList(user.getRole()), validServiceId, httpMethod, requestPath);
//  }

  /**
   * 🆕 핵심 권한 검증 로직 (DB 기반 우선, JSON 폴백)
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
      boolean hasPermission = false;
      
      // 1. DB 기반 권한 확인 (우선순위)
      if (databasePermissionService != null) {
        try {
          hasPermission = databasePermissionService.hasPermission(role, serviceId, httpMethod, normalizedPath);
          log.debug("DB 권한 검증: 역할[{}], 결과[{}]", role, hasPermission);
        } catch (Exception e) {
          log.warn("DB 권한 검증 실패, JSON 폴백 사용: {}", e.getMessage());
        }
      }
      
      // 2. JSON 기반 권한 확인 (폴백)
      if (!hasPermission) {
        hasPermission = permissionJsonConfig.hasPermission(role, serviceId, httpMethod, normalizedPath);
        log.debug("JSON 권한 검증: 역할[{}], 결과[{}]", role, hasPermission);
      }

      if (hasPermission) {
        log.debug("권한 검증 성공: 역할[{}], 서비스[{}], 메소드[{}], 경로[{}]",
                  role, serviceId, httpMethod, normalizedPath);
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
   * @param pattern Ant 패턴 (예: /api/auth/**)
   * @param path 실제 경로 (예: /api/auth/login)
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
