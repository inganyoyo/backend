package org.egovframe.cloud.userservice.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.Permission;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 권한 설정 관리 클래스
 * application.yml 파일에서 권한 정보를 로드
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "permissions")
public class PermissionConfig {
    
    private List<Permission> USER = new ArrayList<>();
    private List<Permission> ADMIN = new ArrayList<>();
    private List<Permission> SYSTEM_ADMIN = new ArrayList<>();
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    @PostConstruct
    public void init() {
        Map<String, List<Permission>> allPermissions = getAllPermissions();
        log.info("권한 설정 로드 완료. 총 역할 수: {}", allPermissions.size());
    }
    
    /**
     * 모든 권한 정보를 Map으로 반환
     */
    public Map<String, List<Permission>> getAllPermissions() {
        Map<String, List<Permission>> result = new HashMap<>();
        
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
     * 특정 역할의 사용자가 해당 서비스/경로/메소드에 접근 권한이 있는지 확인
     */
    public boolean hasPermission(String role, String service, String method, String path) {
        Map<String, List<Permission>> permissions = getAllPermissions();
        
        if (permissions == null || !permissions.containsKey(role)) {
            return false;
        }
        
        List<Permission> rolePermissions = permissions.get(role);
        
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
