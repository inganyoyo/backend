package org.egovframe.cloud.userservice.config;

import java.util.*;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.Permission;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

/**
 * 권한 설정 관리 클래스
 * application.yml 파일에서 권한 정보를 로드하고 권한 상속 기능 제공
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "permissions")
public class PermissionConfig {
    
    /**
     * 🆕 권한 계층 구조 정의
     * 하위 권한은 상위 권한의 모든 권한을 상속받음
     */
    private static final Map<String, List<String>> ROLE_HIERARCHY;

    static {
        Map<String, List<String>> hierarchy = new HashMap<>();
        hierarchy.put("SYSTEM_ADMIN", Arrays.asList("ADMIN", "USER", "ANONYMOUS"));
        hierarchy.put("ADMIN", Arrays.asList("USER", "ANONYMOUS"));
        hierarchy.put("USER", Arrays.asList("ANONYMOUS"));
        hierarchy.put("ANONYMOUS", Collections.emptyList());
        ROLE_HIERARCHY = Collections.unmodifiableMap(hierarchy);
    }

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private List<Permission> ANONYMOUS = new ArrayList<>();
    private List<Permission> USER = new ArrayList<>();
    private List<Permission> ADMIN = new ArrayList<>();
    private List<Permission> SYSTEM_ADMIN = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        Map<String, List<Permission>> allPermissions = getAllPermissions();
        log.info("권한 설정 로드 완료. 총 역할 수: {}", allPermissions.size());
        
        // 권한 상속 관계 로깅
        ROLE_HIERARCHY.forEach((role, inheritedRoles) -> {
            if (!inheritedRoles.isEmpty()) {
                log.info("권한 상속: {} ← {}", role, inheritedRoles);
            }
        });
    }
    
    /**
     * 모든 권한 정보를 Map으로 반환
     */
    public Map<String, List<Permission>> getAllPermissions() {
        Map<String, List<Permission>> result = new HashMap<>();
        
        if (ANONYMOUS != null && !ANONYMOUS.isEmpty()) {
            result.put("ANONYMOUS", ANONYMOUS);
        }
        if (USER != null && !USER.isEmpty()) {
            result.put("USER", USER);
        }
        if (ADMIN != null && !ADMIN.isEmpty()) {
            result.put("ADMIN", ADMIN);
        }
        if (SYSTEM_ADMIN != null && !SYSTEM_ADMIN.isEmpty()) {
            result.put("SYSTEM_ADMIN", SYSTEM_ADMIN);
        }
        
        return result;
    }
    
    /**
     * 🆕 권한 상속을 고려한 권한 검증 메서드
     * 특정 역할의 사용자가 해당 서비스/경로/메소드에 접근 권한이 있는지 확인 (상속 권한 포함)
     */
    public boolean hasPermission(String role, String service, String method, String path) {
        // 1. 직접 권한 체크
        if (hasDirectPermission(role, service, method, path)) {
            return true;
        }
        
        // 2. 상속된 권한 체크
        List<String> inheritedRoles = ROLE_HIERARCHY.getOrDefault(role, Collections.emptyList());
        for (String inheritedRole : inheritedRoles) {
            if (hasDirectPermission(inheritedRole, service, method, path)) {
                log.debug("상속된 권한으로 접근 허용: 현재역할[{}] ← 상속역할[{}], 서비스[{}], 메소드[{}], 경로[{}]", 
                         role, inheritedRole, service, method, path);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 🆕 직접 권한만 체크하는 메서드 (상속 권한 제외)
     */
    private boolean hasDirectPermission(String role, String service, String method, String path) {
        Map<String, List<Permission>> permissions = getAllPermissions();

        if (permissions == null || !permissions.containsKey(role)) {
            return false;
        }
        
        List<Permission> rolePermissions = permissions.get(role);
        if (rolePermissions == null) {
            return false;
        }

        for (Permission permission : rolePermissions) {
            boolean serviceMatch = "*".equals(permission.getService()) ||
                                 service.equals(permission.getService());

            boolean methodMatch = "*".equals(permission.getMethod()) ||
                                method.equalsIgnoreCase(permission.getMethod());

            boolean pathMatch = pathMatcher.match(permission.getPath(), path);
            
            if (serviceMatch && methodMatch && pathMatch) {
                return true;
            }
        }

        return false;
    }

}
