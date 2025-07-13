package org.egovframe.cloud.userservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.User;
import org.egovframe.cloud.userservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 관리자용 API Controller
 * 성능 테스트, 캐시 관리 등 관리 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AuthService authService;
    
    // ========== 성능 테스트 기능 ==========
    
    /**
     * 캐시 성능 테스트 - 연속 세션 조회
     */
    @GetMapping("/performance/cache")
    public ResponseEntity<?> testCachePerformance(@RequestHeader(value = "X-Session-ID", required = false) String sessionId,
                                                  @RequestParam(defaultValue = "10") int iterations,
                                                  @RequestParam(defaultValue = "false") boolean clearCache) {
        if (sessionId == null) {
            return ResponseEntity.badRequest().body("세션 ID가 필요합니다.");
        }
        
        // 🟢 캐시 초기화 옵션
        if (clearCache) {
            authService.clearCache();
            log.info("캐시 초기화 완료 - Redis 직접 조회 테스트");
        }
        
        long startTime = System.currentTimeMillis();
        
        User user = null;
        for (int i = 0; i < iterations; i++) {
            user = authService.getUser(sessionId);
            if (user == null) {
                return ResponseEntity.status(401).body("유효하지 않은 세션입니다.");
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        Map<String, Object> result = new HashMap<>();
        result.put("iterations", iterations);
        result.put("totalTimeMs", duration);
        result.put("averageTimeMs", (double) duration / iterations);
        result.put("user", user);
        result.put("cacheCleared", clearCache);
        result.put("cacheStats", authService.getCacheStats());
        
        log.info("캐시 성능 테스트 완료: {} 회 조회, 총 {}ms, 평균 {}ms (캐시 초기화: {})", 
                iterations, duration, (double) duration / iterations, clearCache);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Thread Pool 검증 테스트 - @Async 비동기 동작 확인
     */
    @GetMapping("/performance/thread-pool")
    public ResponseEntity<?> testThreadPool(@RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
        if (sessionId == null) {
            return ResponseEntity.badRequest().body("세션 ID가 필요합니다.");
        }
        
        String mainThreadName = Thread.currentThread().getName();
        long mainThreadId = Thread.currentThread().getId();
        
        log.info("🚀 메인 Thread 시작: {} (ID: {})", mainThreadName, mainThreadId);
        
        // 직접 Redis에서 조회하여 TTL 연장 유발
        User user = authService.getUserDirectFromRedis(sessionId);
        if (user == null) {
            return ResponseEntity.status(401).body("유효하지 않은 세션입니다.");
        }
        
        log.info("🏁 메인 Thread 완료: {} (ID: {})", mainThreadName, mainThreadId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Thread Pool 테스트 완료");
        response.put("mainThread", mainThreadName);
        response.put("mainThreadId", mainThreadId);
        response.put("user", user);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    // ========== 캐시 관리 기능 ==========
    
    /**
     * 캐시 통계 조회
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = authService.getCacheStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 전체 캐시 초기화
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<String> clearCache() {
        authService.clearCache();
        log.info("관리자에 의한 캐시 초기화 실행");
        return ResponseEntity.ok("캐시가 초기화되었습니다.");
    }
    
    /**
     * 특정 세션 캐시 무효화
     */
    @DeleteMapping("/cache/sessions/{sessionId}")
    public ResponseEntity<String> invalidateSessionCache(@PathVariable String sessionId) {
        authService.invalidateSessionCache(sessionId);
        log.info("세션 캐시 무효화: {}", sessionId);
        return ResponseEntity.ok("세션 캐시가 무효화되었습니다.");
    }
}
