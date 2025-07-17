package org.egovframe.cloud.userservice.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.common.code.SuccessCode;
import org.egovframe.cloud.common.dto.ApiResponse;
import org.egovframe.cloud.common.exception.BusinessException;
import org.egovframe.cloud.common.util.SuccessResponseUtil;
import org.egovframe.cloud.userservice.code.UserServiceErrorCode;
import org.egovframe.cloud.userservice.domain.User;
import org.egovframe.cloud.userservice.dto.AuthCheckResponse;
import org.egovframe.cloud.userservice.service.AuthService;
import org.egovframe.cloud.userservice.service.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 간단한 인증 API Controller 헤더 기반 권한 검증 지원
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthorizationService authorizationService;
    private final SuccessResponseUtil successResponseUtil;

    /**
     * 권한 체크 (API Gateway용 - SecurityContext 기반) AuthenticationFilter에서 이미 SecurityContext가 설정되어 있음
     */
    @GetMapping("/api/auth/check")
    public ResponseEntity<AuthCheckResponse> checkAuthorization(
            @RequestHeader(value = "X-Service-ID", required = false) String serviceId,
            @RequestParam String httpMethod,
            @RequestParam String requestPath) {
        log.info("[checkAuthorization] path: {}, method: {}, serviceId: {}", requestPath, httpMethod, serviceId);
        
        // 인증/인가 로직 수행
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("[checkAuthorization] checking authentication: {}", authentication);

        boolean isAuth = authorizationService.isAuthorization(authentication, requestPath, httpMethod, serviceId);

        User user = null;
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                user = (User) principal;
            }
        }

        int status;

        if (user == null && !isAuth) {
            // 인증 실패 (User 객체 없음 = 로그인하지 않음)
            status = HttpStatus.UNAUTHORIZED.value(); // 401
            log.info("[checkAuthorization] Authentication failed: no user session");
        } else if (user != null && !isAuth) {
            // 인가 실패 (로그인했지만 권한 없음)
            status = HttpStatus.FORBIDDEN.value(); // 403
            log.info("[checkAuthorization] Authorization failed: user {} has no permission", user.getUsername());
        } else if (user != null && isAuth) {
            // 인증 + 인가 성공
            status = HttpStatus.OK.value(); // 200
            log.info("[checkAuthorization] Access granted: user {} to {}", user != null ? user.getUsername() : "system", requestPath);
        }else{
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(); // 200
            log.info("[checkAuthorization] Access granted: user {} to {}", user != null ? user.getUsername() : "system", requestPath);
        }
        
        return ResponseEntity.ok(AuthCheckResponse.builder()
                .isAuthorized(isAuth)
                .user(user)
                .status(status)
                .build());
    }

    /**
     * 로그인 (JSON 요청용)
     */
    @PostMapping(value = "/api/auth/login", consumes = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> loginRequest, HttpServletRequest request) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || password == null) {
            throw BusinessException.builder(UserServiceErrorCode.MISSING_CREDENTIALS).build();
        }

        // 1. 유저 정보조회
        User  user = authService.login(username, password);

        if (user == null) {
            throw BusinessException.builder(UserServiceErrorCode.INVALID_CREDENTIALS).build();
        }

        // 2. 권한 리스트 부여
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_"+user.getRole());

        // 3. 사용자 정보로 Authentication 객체 생성
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);

        // 4. SecurityContext 생성 및 저장
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 5. 세션에 SecurityContext 저장 (Spring Session Redis 지원됨)
        HttpSession session = request.getSession(true); // 세션 생성
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        Map<String, Object> loginData = new HashMap<>();
        loginData.put("sessionId", user.getUserId());
        loginData.put("username", username);

        return successResponseUtil.success(SuccessCode.ACTION_SUCCESS)
                .data(loginData)
                .args("로그인", "완료")
                .build();
    }

    /**
     * 로그인 (Form POST용 - 페이지 전체 이동)
     */
    @PostMapping(value = "/api/auth/login")
    public void loginWithForm(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletResponse response,
            HttpServletRequest request) throws Exception {
        
        log.info("Form POST 로그인 시도: {}", username);
        
        try {
            if (username == null || password == null) {
                response.sendRedirect("/test?error=missing_credentials");
                return;
            }

            User user = authService.login(username, password);
            
            if (user == null) {
                response.sendRedirect("/test?error=invalid_credentials");
                return;
            }

            // 2. 권한 리스트 부여
            List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_"+user.getRole());

            // 3. 사용자 정보로 Authentication 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            // 4. SecurityContext 생성 및 저장
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 5. 세션에 SecurityContext 저장 (Spring Session Redis 지원됨)
            HttpSession session = request.getSession(true); // 세션 생성
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

//
//            // 세션 쿠키 설정
//            Cookie sessionCookie = new Cookie("GSNS-SESSION", user);
//            sessionCookie.setPath("/");
//            sessionCookie.setMaxAge(-1); // 30분
//            sessionCookie.setHttpOnly(true);
//            response.addCookie(sessionCookie);
//            String cookieHeader = "GSNS-SESSION=" + sessionId + "; Path=/; HttpOnly; SameSite=None";
//            response.addHeader("Set-Cookie", cookieHeader);
//            log.info("Form POST 로그인 성공: {}, 세션: {}", username, sessionId);
//
            // 성공 페이지로 리다이렉트
            response.sendRedirect("http://localhost:8010/test?success=login_success&username=" + username);
            
        } catch (Exception e) {
            log.error("Form POST 로그인 처리 중 오류", e);
            response.sendRedirect("/test?error=processing_failed");
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/api/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId, HttpServletRequest request, HttpServletResponse response) {

        log.info("logout");
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            System.out.println("No cookies found.");

        }else {
            for (Cookie cookie : cookies) {
                System.out.println("Cookie Name : " + cookie.getName());
                System.out.println("Cookie Value: " + cookie.getValue());
                System.out.println("Domain      : " + cookie.getDomain());
                System.out.println("Path        : " + cookie.getPath());
                System.out.println("Max-Age     : " + cookie.getMaxAge());
                System.out.println("Secure      : " + cookie.getSecure());
                System.out.println("HttpOnly    : " + cookie.isHttpOnly());
                System.out.println("------------------------------------");
            }
        }

        if (sessionId != null) {
            authService.logout(sessionId);
        }

        return successResponseUtil.success(SuccessCode.ACTION_SUCCESS)
                .args("로그아웃", "완료")
                .build();
    }

    /**
     * 세션 검증 (Boolean 반환 - API Gateway 호환성 유지)
     */
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

    /**
     * 사용자 프로필 조회 (Spring Security로 인증 처리됨) - GET 방식
     */
    @GetMapping("/api/users/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(@AuthenticationPrincipal User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getUserId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("loginTime", user.getLoginTime());
        profile.put("lastAccessTime", user.getLastAccessTime());

        return successResponseUtil.success(SuccessCode.ITEM_RETRIEVED)
                .data(profile)
                .args("프로필")
                .build();
    }

    /**
     * 사용자 프로필 조회 (Spring Security로 인증 처리됨) - POST 방식
     */
    @PostMapping("/api/users/profile")
    public void getProfilePost(@AuthenticationPrincipal User user, HttpServletResponse response) throws Exception {
        log.info("getProfilePost");


        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getUserId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("loginTime", user.getLoginTime());
        profile.put("lastAccessTime", user.getLastAccessTime());
        profile.put("method", "POST"); // POST 방식임을 표시

        response.sendRedirect("http://localhost:8010/test?POST");
//        return successResponseUtil.success(SuccessCode.ITEM_RETRIEVED)
//                .data(profile)
//                .args("프로필 (POST)")
//                .build();
    }


}