package org.egovframe.cloud.userservice.api;

import java.util.HashMap;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 로그인
     */
    @PostMapping("/api/auth/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || password == null) {
            throw BusinessException.builder(UserServiceErrorCode.MISSING_CREDENTIALS).build();
        }

        String sessionId = authService.login(username, password);
        log.info("sessionId " + sessionId);
        if (sessionId == null) {
            throw BusinessException.builder(UserServiceErrorCode.INVALID_CREDENTIALS).build();
        }

        Map<String, Object> loginData = new HashMap<>();
        loginData.put("sessionId", sessionId);
        loginData.put("username", username);

        return successResponseUtil.success(SuccessCode.ACTION_SUCCESS)
                .data(loginData)
                .args("로그인", "완료")
                .build();
    }

    /**
     * 로그아웃
     */
    @PostMapping("/api/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

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
     * 사용자 프로필 조회 (Spring Security로 인증 처리됨)
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


}