package org.egovframe.cloud.apigateway.config;

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
 * </pre>
 */
public interface GlobalConstant {
  final String AUTHORIZATION_URI = "/api/v1/authorizations/check";
  final String SESSION_COOKIE_NAME = "GSNS-SESSION";
  final String SESSION_HEADER_NAME = "X-Session-ID";
  final String HEADER_SERVICE_NAME = "X-Service-ID";

  final String MESSAGES_URI = "/api/v1/messages/**";
  final String LOGIN_URI = "/login";

  final String[]  PERMITALL_ANTPATTERNS = {
          AUTHORIZATION_URI, "/", "/csrf",
          "/auth-service/api/v1/auth/login", // 로그인은 권한 확인 불필요
          "/auth-service/api/v1/auth/logout", // 로그인아웃은 권한 확인 불필요
          "/auth-service/api/v1/auth/validate", // 세션 검증 API 허용
          "/test", "/test/**", // 테스트 페이지 허용
  };

final String USER_SERVICE_URI = "/user-service";

}
