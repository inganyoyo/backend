package org.egovframe.cloud.userservice.api;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.service.DatabasePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 권한 관리 API (관리자용)
 * DB 기반과 JSON 기반을 조건부로 지원
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
public class PermissionAdminController {

    @Autowired(required = false)
    private DatabasePermissionService databasePermissionService;

    /**
     * 수동 권한 캐시 갱신
     */
    @PostMapping("/cache/refresh")
    public ResponseEntity<Map<String, Object>> refreshCache() {
        log.info("수동 권한 캐시 갱신 요청 받음");

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", System.currentTimeMillis());

            if (databasePermissionService != null) {
                databasePermissionService.manualRefresh();

                response.put("success", true);
                response.put("mode", "DB 기반 권한 관리");
                response.put("message", "권한 캐시가 성공적으로 갱신되었습니다.");
            } else {
                response.put("success", false);
                response.put("mode", "JSON 기반 권한 관리");
                response.put("message", "DB 기반 권한 관리가 비활성화되어 있습니다. JSON 파일 기반으로 동작 중입니다.");
                response.put("note", "DB 기반 권한 갱신은 지원되지 않습니다.");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("권한 캐시 갱신 실패", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "권한 캐시 갱신에 실패했습니다: " + e.getMessage());
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 권한 캐시 상태 조회
     */
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", System.currentTimeMillis());

            if (databasePermissionService != null) {
                String status = databasePermissionService.getCacheStatus();

                response.put("success", true);
                response.put("mode", "DB 기반 권한 관리");
                response.put("status", status);
                response.put("autoRefreshInterval", "10분");
                response.put("nextRefresh", "최대 10분 후");
            } else {
                response.put("success", true);
                response.put("mode", "JSON 기반 권한 관리");
                response.put("status", "정상 동작 중");
                response.put("note", "DB 연결 후 10분 자동 갱신 모드로 전환 가능");
                response.put("configLocation", "src/main/resources/permissions/*.json");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("캐시 상태 조회 실패", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "캐시 상태 조회에 실패했습니다: " + e.getMessage());
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.internalServerError().body(error);
        }
    }
}
