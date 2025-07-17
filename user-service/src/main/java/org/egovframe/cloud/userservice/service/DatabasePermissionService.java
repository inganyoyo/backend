package org.egovframe.cloud.userservice.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.Permission;
import org.egovframe.cloud.userservice.mapper.PermissionMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

/**
 * 데이터베이스 기반 권한 관리 서비스
 * 10분마다 자동으로 권한 정보를 갱신합니다.
 * 🔄 PostgreSQL 연결 시에만 활성화
 *
 * @version 1.0
 * @since 2025/07/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "permission.database.enabled", havingValue = "true", matchIfMissing = false)
public class DatabasePermissionService {

    private final PermissionMapper permissionMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 간단한 인메모리 캐시 (Caffeine 캐시와 별도)
    private final ConcurrentHashMap<String, List<Permission>> rolePermissionCache = new ConcurrentHashMap<>();
    private String lastPermissionHash = "";

    /**
     * 🚀 애플리케이션 시작 시 초기 권한 로드
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialLoad() {
        log.info("=== 애플리케이션 시작 - 초기 권한 로드 ===");
        refreshAllPermissions();
    }

    /**
     * ⏰ 10분마다 권한 정보 갱신
     */
    @Scheduled(fixedDelay = 600000) // 10분 = 600,000ms
    public void refreshAllPermissions() {
        log.info("권한 캐시 갱신 시작...");

        try {
            //String currentHash = permissionMapper.getPermissionDataHash();

//            if (currentHash != null && currentHash.equals(lastPermissionHash)) {
//                log.info("권한 데이터 변경 없음 - 캐시 갱신 생략");
//                return;
//            }

            List<String> allRoles = permissionMapper.findAllRoleNames();
            int totalPermissionCount = 0;

            for (String roleName : allRoles) {
                List<Permission> permissions = permissionMapper.findAllPermissionsByRole(roleName);
                rolePermissionCache.put(roleName, permissions);
                totalPermissionCount += permissions.size();

                log.debug("역할 [{}] 권한 {}개 로드됨", roleName, permissions.size());
            }

//            lastPermissionHash = currentHash;

            log.info("권한 캐시 갱신 완료 - 총 {}개 역할, {}개 권한 ",
                    allRoles.size(), totalPermissionCount);

        } catch (Exception e) {
            log.error("권한 캐시 갱신 실패", e);
        }
    }

    /**
     * 🔍 특정 역할의 권한 조회 (캐시된 데이터 사용)
     *
     * @param roleName 역할명
     * @return 권한 목록
     */
    public List<Permission> getAllPermissionsByRole(String roleName) {
        List<Permission> permissions = rolePermissionCache.get(roleName);

        if (permissions == null) {
            log.warn("캐시에 역할 [{}] 권한이 없음 - DB에서 직접 조회", roleName);
            try {
                permissions = permissionMapper.findAllPermissionsByRole(roleName);
                if (permissions != null) {
                    rolePermissionCache.put(roleName, permissions);
                }
            } catch (Exception e) {
                log.error("역할 [{}] 권한 조회 실패", roleName, e);
                return Collections.emptyList(); // 빈 리스트 반환
            }
        }

        return permissions != null ? permissions : Collections.emptyList();
    }

    /**
     * 🎯 권한 검증 메서드
     *
     * @param roleName 역할명
     * @param service 서비스명
     * @param method HTTP 메서드
     * @param path 요청 경로
     * @return 권한 여부
     */
    public boolean hasPermission(String roleName, String service, String method, String path) {
        List<Permission> permissions = getAllPermissionsByRole(roleName);

        return permissions.stream().anyMatch(permission -> {
            boolean serviceMatch = "*".equals(permission.getService()) ||
                    service.equals(permission.getService());
            boolean methodMatch = "*".equals(permission.getMethod()) ||
                    method.equalsIgnoreCase(permission.getMethod());
            boolean pathMatch = pathMatcher.match(permission.getPath(), path);

            if (serviceMatch && methodMatch && pathMatch) {
                log.debug("권한 매칭 성공: 역할[{}], 서비스[{}], 메소드[{}], 경로[{}] -> 권한[{}/{}/{}]",
                        roleName, service, method, path,
                        permission.getService(), permission.getMethod(), permission.getPath());
                return true;
            }
            return false;
        });
    }

    /**
     * 🛠️ 수동 캐시 갱신 (관리용)
     */
    public void manualRefresh() {
        log.info("수동 권한 캐시 갱신 요청");
        refreshAllPermissions();
    }

    /**
     * 📊 캐시 상태 정보
     */
    public String getCacheStatus() {
        return String.format("캐시된 역할 수: %d, 마지막 해시: %s",
                rolePermissionCache.size(), lastPermissionHash);
    }
}
