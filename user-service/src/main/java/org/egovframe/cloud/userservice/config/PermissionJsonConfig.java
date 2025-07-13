package org.egovframe.cloud.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.Permission;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.AntPathMatcher;

/**
 * JSON 파일 기반 권한 설정 관리 클래스
 * permissions/ 폴더의 JSON 파일들에서 권한 정보를 로드하고 권한 상속 기능 제공
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PermissionJsonConfig {
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper;
    
    /**
     * JSON에서 읽어온 역할별 권한 정보
     */
    private final Map<String, RolePermission> rolePermissions = new HashMap<>();
    
    /**
     * 🆕 권한 계층 구조 정의 - JSON에서 동적으로 로드
     * 하위 권한은 상위 권한의 모든 권한을 상속받음
     */
    private final Map<String, List<String>> roleHierarchy = new HashMap<>();
    
    @PostConstruct
    public void init() {
        loadPermissionFiles();
        buildRoleHierarchy();
        logPermissionSummary();
    }
    
    /**
     * permissions/ 폴더의 모든 JSON 파일을 로드
     */
    private void loadPermissionFiles() {
        String[] permissionFiles = {
            "permissions/anonymous.json",
            "permissions/user.json", 
            "permissions/admin.json",
            "permissions/system-admin.json"
        };
        
        for (String filePath : permissionFiles) {
            try {
                ClassPathResource resource = new ClassPathResource(filePath);
                if (resource.exists()) {
                    RolePermission rolePermission = objectMapper.readValue(
                        resource.getInputStream(), 
                        RolePermission.class
                    );
                    rolePermissions.put(rolePermission.getRole(), rolePermission);
                    log.info("권한 파일 로드 완료: {} (권한 수: {})", 
                            filePath, rolePermission.getPermissions().size());
                } else {
                    log.warn("권한 파일을 찾을 수 없음: {}", filePath);
                }
            } catch (IOException e) {
                log.error("권한 파일 로드 실패: {}, 오류: {}", filePath, e.getMessage());
            }
        }
    }
    
    /**
     * JSON의 inherits 정보를 기반으로 권한 계층 구조 구축
     */
    private void buildRoleHierarchy() {
        for (RolePermission rolePermission : rolePermissions.values()) {
            String role = rolePermission.getRole();
            List<String> inherits = rolePermission.getInherits();
            
            if (inherits != null && !inherits.isEmpty()) {
                roleHierarchy.put(role, new ArrayList<>(inherits));
            } else {
                roleHierarchy.put(role, Collections.emptyList());
            }
        }
        
        log.info("권한 계층 구조 구축 완료");
        roleHierarchy.forEach((role, inheritedRoles) -> {
            if (!inheritedRoles.isEmpty()) {
                log.info("권한 상속: {} ← {}", role, inheritedRoles);
            }
        });
    }
    
    /**
     * 권한 로딩 완료 후 요약 정보 로깅
     */
    private void logPermissionSummary() {
        log.info("=== 권한 설정 로드 완료 ===");
        log.info("총 역할 수: {}", rolePermissions.size());
        
        int totalPermissions = rolePermissions.values().stream()
            .mapToInt(rp -> rp.getPermissions().size())
            .sum();
        log.info("총 권한 수: {}", totalPermissions);
        
        rolePermissions.forEach((role, rp) -> {
            log.info("- {}: {}개 권한 ({})", role, rp.getPermissions().size(), rp.getDescription());
        });
        
        // 🆕 각 역할별 최종 권한 내역 출력 (상속 포함)
        logDetailedPermissions();
    }
    
    /**
     * 🆕 각 역할별 상속을 포함한 최종 권한 내역을 상세히 로깅
     */
    private void logDetailedPermissions() {
        log.info("");
        log.info("=== 📋 역할별 최종 권한 내역 (상속 포함) ===");
        
        // 역할 순서 정의 (계층 순서대로)
        String[] roleOrder = {"ANONYMOUS", "USER", "ADMIN", "SYSTEM_ADMIN"};
        
        for (String role : roleOrder) {
            if (rolePermissions.containsKey(role)) {
                logRoleDetailedPermissions(role);
                log.info(""); // 역할 간 구분을 위한 빈 줄
            }
        }
    }
    
    /**
     * 🆕 특정 역할의 상세 권한 내역 로깅
     */
    private void logRoleDetailedPermissions(String role) {
        List<Permission> allPermissions = getAllPermissionsForRole(role);
        RolePermission rolePermission = rolePermissions.get(role);
        
        log.info("🔐 {} ({}) - 총 {}개 권한", 
                role, 
                rolePermission.getDescription(), 
                allPermissions.size());
        
        // 상속 정보 출력
        List<String> inheritedRoles = roleHierarchy.getOrDefault(role, Collections.emptyList());
        if (!inheritedRoles.isEmpty()) {
            log.info("   ↳ 상속: {}", inheritedRoles);
        }
        
        // 권한을 서비스별로 그룹화
        Map<String, List<Permission>> permissionsByService = new HashMap<>();
        for (Permission permission : allPermissions) {
            permissionsByService.computeIfAbsent(permission.getService(), k -> new ArrayList<>()).add(permission);
        }
        
        // 서비스별로 권한 출력
        permissionsByService.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String service = entry.getKey();
                List<Permission> permissions = entry.getValue();
                
                log.info("   📁 [{}] 서비스:", service);
                permissions.forEach(permission -> {
                    String source = getPermissionSource(role, permission);
                    log.info("      • {} {} - {} {}", 
                            permission.getMethod(), 
                            permission.getPath(), 
                            permission.getDescription() != null ? permission.getDescription() : "권한",
                            source);
                });
            });
    }
    
    /**
     * 🆕 권한의 출처를 확인 (직접 권한인지 상속된 권한인지)
     */
    private String getPermissionSource(String role, Permission targetPermission) {
        // 1. 직접 권한인지 확인
        RolePermission rolePermission = rolePermissions.get(role);
        if (rolePermission != null && rolePermission.getPermissions() != null) {
            for (Permission permission : rolePermission.getPermissions()) {
                if (isSamePermission(permission, targetPermission)) {
                    return "(직접)";
                }
            }
        }
        
        // 2. 상속된 권한인지 확인
        List<String> inheritedRoles = roleHierarchy.getOrDefault(role, Collections.emptyList());
        for (String inheritedRole : inheritedRoles) {
            RolePermission inheritedRolePermission = rolePermissions.get(inheritedRole);
            if (inheritedRolePermission != null && inheritedRolePermission.getPermissions() != null) {
                for (Permission permission : inheritedRolePermission.getPermissions()) {
                    if (isSamePermission(permission, targetPermission)) {
                        return "(← " + inheritedRole + ")";
                    }
                }
            }
        }
        
        return "(?)";
    }
    
    /**
     * 🆕 두 권한이 동일한지 비교
     */
    private boolean isSamePermission(Permission p1, Permission p2) {
        return Objects.equals(p1.getService(), p2.getService()) &&
               Objects.equals(p1.getMethod(), p2.getMethod()) &&
               Objects.equals(p1.getPath(), p2.getPath());
    }
    
    /**
     * 🆕 권한 상속을 고려한 권한 검증 메서드
     */
    public boolean hasPermission(String role, String service, String method, String path) {
        // 1. 직접 권한 체크
        if (hasDirectPermission(role, service, method, path)) {
            return true;
        }
        
        // 2. 상속된 권한 체크
        List<String> inheritedRoles = roleHierarchy.getOrDefault(role, Collections.emptyList());
        for (String inheritedRole : inheritedRoles) {
            if (hasDirectPermission(inheritedRole, service, method, path)) {
                log.info("상속된 권한으로 접근 허용: 현재역할[{}] ← 상속역할[{}], 서비스[{}], 메소드[{}], 경로[{}]",
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
        RolePermission rolePermission = rolePermissions.get(role);
        
        if (rolePermission == null || rolePermission.getPermissions() == null) {
            return false;
        }

        for (Permission permission : rolePermission.getPermissions()) {
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

    /**
     * 🆕 특정 역할이 상속받는 모든 권한 목록 반환 (디버깅/관리용)
     */
    public List<Permission> getAllPermissionsForRole(String role) {
        List<Permission> allPermissions = new ArrayList<>();

        // 1. 직접 권한 추가
        RolePermission rolePermission = rolePermissions.get(role);
        if (rolePermission != null && rolePermission.getPermissions() != null) {
            allPermissions.addAll(rolePermission.getPermissions());
        }

        // 2. 상속된 권한 추가
        List<String> inheritedRoles = roleHierarchy.getOrDefault(role, Collections.emptyList());
        for (String inheritedRole : inheritedRoles) {
            RolePermission inheritedRolePermission = rolePermissions.get(inheritedRole);
            if (inheritedRolePermission != null && inheritedRolePermission.getPermissions() != null) {
                allPermissions.addAll(inheritedRolePermission.getPermissions());
            }
        }

        return allPermissions;
    }

    /**
     * 모든 역할 목록 반환
     */
    public Set<String> getAllRoles() {
        return new HashSet<>(rolePermissions.keySet());
    }

    /**
     * 특정 역할 정보 반환
     */
    public RolePermission getRolePermission(String role) {
        return rolePermissions.get(role);
    }

    /**
     * JSON 파일 구조를 매핑하는 내부 클래스
     */
    @Data
    public static class RolePermission {
        private String role;
        private String description;
        private List<String> inherits;
        private List<Permission> permissions;
    }
}
