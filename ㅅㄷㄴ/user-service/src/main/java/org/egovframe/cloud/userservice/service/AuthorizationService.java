package org.egovframe.cloud.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.config.PermissionConfig;
import org.egovframe.cloud.userservice.domain.User;
import org.springframework.stereotype.Service;

/**
 * 권한 검증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {
    
    private final AuthService authService;
    private final PermissionConfig permissionConfig;
    
    /**
     * 요청에 대한 권한 검증 (세션 ID로)
     */
    public boolean checkPermission(String sessionId, String requestPath, String httpMethod) {
        // 세션에서 사용자 정보 조회
        User user = authService.getUser(sessionId);
        if (user == null) {
            log.warn("유효하지 않은 세션: {}", sessionId);
            return false;
        }
        
        return checkPermission(user, requestPath, httpMethod);
    }
    
    /**
     * 요청에 대한 권한 검증 (User 객체로) - 🟢 최적화된 버전
     */
    public boolean checkPermission(User user, String requestPath, String httpMethod) {
        // 서비스 이름 추출 (requestPath에서 첫 번째 세그먼트)
        String serviceName = extractServiceName(requestPath);
        
        // 권한 검증
        boolean hasPermission = permissionConfig.hasPermission(
            user.getRole(), 
            serviceName, 
            httpMethod, 
            requestPath
        );
        
        log.info("권한 검증 결과: 사용자[{}], 역할[{}], 서비스[{}], 메소드[{}], 경로[{}] => {}", 
                user.getUsername(), user.getRole(), serviceName, httpMethod, requestPath, 
                hasPermission ? "허용" : "거부");
        
        return hasPermission;
    }
    
    /**
     * 요청 경로에서 서비스 이름 추출
     * 예: /api/v1/users/profile -> user-service (API Gateway에서 라우팅되기 때문)
     */
    private String extractServiceName(String requestPath) {
        // API Gateway를 통해 들어오는 요청은 이미 서비스별로 라우팅된 상태
        // 따라서 현재 서비스는 user-service
        
        if (requestPath.startsWith("/api/v1/users") || requestPath.startsWith("/api/v1/test")) {
            return "user-service";
        } else if (requestPath.startsWith("/api/v1/boards")) {
            return "board-service";
        } else if (requestPath.startsWith("/api/v1/portal")) {
            return "portal-service";
        } else if (requestPath.startsWith("/api/v1/reserve")) {
            return "reserve-service";
        }
        
        // 기본값
        return "user-service";
    }
}
