package org.egovframe.cloud.apigateway.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * org.egovframe.cloud.common.config.Constants
 *
 * <p>공통 전역 상수 정의
 *
 * @author 표준프레임워크센터 jaeyeolkim
 * @version 1.0
 * @since 2021/07/19
 *     <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2021/07/19    jaeyeolkim  최초 생성
 *  2025/07/14    개발팀      서비스 목록 통일 추가
 * </pre>
 */
public interface GlobalConstant {
  final String AUTHORIZATION_URI = "/api/authorizations/check";
  final String SESSION_COOKIE_NAME = "GSNS-SESSION";
  final String SESSION_HEADER_NAME = "X-Session-ID";
  final String HEADER_SERVICE_NAME = "X-Service-ID";

  // 🆕 실제 사용하는 서비스 목록만 유지
  final Set<String> KNOWN_SERVICES = Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(
                  "user-service",    // ✅ 인증/사용자 관리
                  "board-service"    // ✅ 게시판 서비스
                  // 필요시 추가: portal-service 등
          ))
  );

  /**
   * 주어진 서비스명이 알려진 서비스인지 확인한다
   *
   * @param serviceName 확인할 서비스명
   * @return boolean 알려진 서비스 여부
   */
  static boolean isKnownService(String serviceName) {
    return KNOWN_SERVICES.contains(serviceName);
  }

  final String MESSAGES_URI = "/api/messages/**";
  final String LOGIN_URI = "/login";

  final String[]  PERMITALL_ANTPATTERNS = {
          AUTHORIZATION_URI, "/", "/csrf",
          "/auth-service/api/auth/login", // 로그인은 권한 확인 불필요
          "/auth-service/api/auth/logout", // 로그인아웃은 권한 확인 불필요
          "/auth-service/api/auth/validate", // 세션 검증 API 허용
          "/test2","/test", "/test/**", // 테스트 페이지 허용
  };

final String USER_SERVICE_URI = "/user-service";

}
