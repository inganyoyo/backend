package org.egovframe.cloud.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 최적화된 인증 서비스
 * Redis Session 기반 + Caffeine 로컬 캐시로 성능 향상
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SessionAsyncService sessionAsyncService;  // 🟢 별도 서비스 주입
    
    // 🟢 Thread Pool Executor 주입 (통계용)
    @Qualifier("sessionAsyncExecutor")
    private final Executor sessionAsyncExecutor;
    
    // 로컬 캐시로 빈번한 Redis 호출 줄이기
    private Cache<String, User> userCache;
    private Cache<String, Boolean> sessionExistsCache; // 세션 존재 여부 캐시
    
    // 테스트용 사용자 정보 (실제로는 외부 시스템이나 파일에서 로드)
    private static final Map<String, String[]> USERS = new HashMap<>();
    
    static {
        // {username, password, email, role}
        USERS.put("user1", new String[]{"user1", "user123", "user1@example.com", "USER"});
        USERS.put("admin", new String[]{"admin", "admin123", "admin@example.com", "ADMIN"});
        USERS.put("system", new String[]{"system", "system123", "system@example.com", "SYSTEM_ADMIN"});
    }
    
    @PostConstruct
    public void initCache() {
        // 사용자 정보 캐시 (5분)
        userCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats() // 캐시 통계 수집
            .build();
        
        // 세션 존재 여부 캐시 (1분) - 더 짧은 TTL로 데이터 일관성 보장
        sessionExistsCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .recordStats()
            .build();
        
        log.info("인증 서비스 캐시 초기화 완료");
    }
    
    /**
     * 사용자 로그인
     */
    public String login(String username, String password) {
        log.info("로그인 시도: {}", username);
        
        if (!USERS.containsKey(username)) {
            log.warn("존재하지 않는 사용자: {}", username);
            return null;
        }
        
        String[] userInfo = USERS.get(username);
        String storedPassword = userInfo[1];
        
        if (!password.equals(storedPassword)) {
            log.warn("비밀번호 불일치: {}", username);
            return null;
        }
        
        // 세션 ID 생성
        String sessionId = UUID.randomUUID().toString();
        
        // 사용자 정보 생성
        User user = User.builder()
                .userId(username)
                .username(userInfo[0])
                .email(userInfo[2])
                .role(userInfo[3])
                .build();
        
        // Redis에 세션 저장 (30분 TTL)
        String sessionKey = "session:" + sessionId;
        redisTemplate.opsForValue().set(sessionKey, user, 30, TimeUnit.MINUTES);
        
        // 캐시에 저장
        userCache.put(sessionId, user);
        sessionExistsCache.put(sessionId, true);
        
        log.info("로그인 성공: {} (세션: {})", username, sessionId);
        return sessionId;
    }
    
    /**
     * 세션 검증 및 사용자 정보 조회 (최적화된 버전)
     */
    public User getUser(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }

        // 1단계: 로컬 캐시에서 먼저 확인
        User cachedUser = userCache.getIfPresent(sessionId);
        if (cachedUser != null) {
            log.debug("캐시에서 사용자 정보 조회: {}", cachedUser.getUserId());
            
            // 비동기로 Redis TTL 연장 (블로킹하지 않음)
            sessionAsyncService.extendSessionTTLAsync(sessionId, cachedUser);
            return cachedUser;
        }

        // 2단계: 세션 존재 여부 캐시 확인
        Boolean sessionExists = sessionExistsCache.getIfPresent(sessionId);
        if (Boolean.FALSE.equals(sessionExists)) {
            log.debug("캐시에서 세션 없음 확인: {}", sessionId);
            return null;
        }

        // 3단계: Redis에서 조회
        String sessionKey = "session:" + sessionId;
        Object obj = redisTemplate.opsForValue().get(sessionKey);

        if (obj == null) {
            // 세션이 없음을 캐시에 저장
            sessionExistsCache.put(sessionId, false);
            log.debug("Redis에서 세션 없음: {}", sessionId);
            return null;
        }

        User user;
        try {
            if (obj instanceof User) {
                user = (User) obj;
            } else if (obj instanceof Map) {
                user = objectMapper.convertValue(obj, User.class);
            } else {
                log.warn("알 수 없는 세션 데이터 타입: {}", obj.getClass().getName());
                return null;
            }
            
            // 사용자 정보 업데이트
            user.updateLastAccessTime();
            
            // 캐시에 저장 (즉시)
            userCache.put(sessionId, user);
            sessionExistsCache.put(sessionId, true);
            
            // 🟢 개선: Redis TTL 연장도 비동기로 처리 (성능 향상)
            sessionAsyncService.extendSessionTTLAsync(sessionId, user);
            
            log.debug("Redis에서 사용자 정보 조회 및 캐시 저장: {}", user.getUserId());
            return user;
            
        } catch (Exception e) {
            log.error("사용자 정보 역직렬화 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 세션 연장 (기존 API 호환성 유지)
     */
    public boolean extendSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }

        // 캐시에서 먼저 확인
        User cachedUser = userCache.getIfPresent(sessionId);
        if (cachedUser != null) {
            // 비동기로 Redis TTL 연장
            sessionAsyncService.extendSessionTTLAsync(sessionId, cachedUser);
            return true;
        }

        // Redis에서 확인
        String sessionKey = "session:" + sessionId;
        Object obj = redisTemplate.opsForValue().get(sessionKey);

        if (obj == null) {
            sessionExistsCache.put(sessionId, false);
            return false;
        }

        User user;
        try {
            if (obj instanceof User) {
                user = (User) obj;
            } else if (obj instanceof Map) {
                user = objectMapper.convertValue(obj, User.class);
            } else {
                return false;
            }

            // 🟢 마지막 접근 시간 업데이트 (중요!)
            user.updateLastAccessTime();

            // 비동기로 Redis TTL 연장
            sessionAsyncService.extendSessionTTLAsync(sessionId, user);
            
            // 캐시 업데이트
            userCache.put(sessionId, user);
            sessionExistsCache.put(sessionId, true);

            return true;
        } catch (Exception e) {
            log.error("세션 연장 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 로그아웃 (캐시도 함께 정리)
     */
    public void logout(String sessionId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            String sessionKey = "session:" + sessionId;
            
            // Redis에서 삭제
            redisTemplate.delete(sessionKey);
            
            // 캐시에서도 삭제
            userCache.invalidate(sessionId);
            sessionExistsCache.invalidate(sessionId);
            
            log.info("로그아웃: 세션 [{}] 삭제 (Redis + 캐시)", sessionId);
        }
    }
    
    /**
     * 사용자 인증 (username/password)
     */
    public User authenticate(String username, String password) {
        log.info("사용자 인증 시도: {}", username);
        
        if (!USERS.containsKey(username)) {
            log.warn("존재하지 않는 사용자: {}", username);
            return null;
        }
        
        String[] userInfo = USERS.get(username);
        String storedPassword = userInfo[1];
        
        if (!password.equals(storedPassword)) {
            log.warn("비밀번호 불일치: {}", username);
            return null;
        }
        
        // 사용자 정보 생성
        User user = User.builder()
                .userId(username)
                .username(userInfo[0])
                .email(userInfo[2])
                .role(userInfo[3])
                .build();
        
        log.info("인증 성공: {}", username);
        return user;
    }
    
    /**
     * 사용자 정보 조회 (username으로)
     */
    public User getUserByUsername(String username) {
        if (!USERS.containsKey(username)) {
            return null;
        }
        
        String[] userInfo = USERS.get(username);
        return User.builder()
                .userId(username)
                .username(userInfo[0])
                .email(userInfo[2])
                .role(userInfo[3])
                .build();
    }
    
    /**
     * 캐시 통계 조회 (모니터링용)
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 사용자 캐시 통계
        CacheStats userStats = userCache.stats();
        Map<String, Object> userCacheStats = new HashMap<>();
        userCacheStats.put("hitCount", userStats.hitCount());
        userCacheStats.put("missCount", userStats.missCount());
        userCacheStats.put("hitRate", String.format("%.2f%%", userStats.hitRate() * 100));
        userCacheStats.put("size", userCache.estimatedSize());
        
        // 세션 존재 여부 캐시 통계
        CacheStats sessionStats = sessionExistsCache.stats();
        Map<String, Object> sessionCacheStats = new HashMap<>();
        sessionCacheStats.put("hitCount", sessionStats.hitCount());
        sessionCacheStats.put("missCount", sessionStats.missCount());
        sessionCacheStats.put("hitRate", String.format("%.2f%%", sessionStats.hitRate() * 100));
        sessionCacheStats.put("size", sessionExistsCache.estimatedSize());
        
        stats.put("userCache", userCacheStats);
        stats.put("sessionExistsCache", sessionCacheStats);
        
        // 🟢 Thread Pool 통계 추가
        try {
            if (sessionAsyncExecutor instanceof org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) {
                org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = 
                    (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) sessionAsyncExecutor;
                
                Map<String, Object> threadPoolStats = new HashMap<>();
                threadPoolStats.put("activeCount", executor.getActiveCount());
                threadPoolStats.put("poolSize", executor.getPoolSize());
                threadPoolStats.put("corePoolSize", executor.getCorePoolSize());
                threadPoolStats.put("maxPoolSize", executor.getMaxPoolSize());
                threadPoolStats.put("queueSize", executor.getQueueSize());
                threadPoolStats.put("queueCapacity", executor.getQueueCapacity());
                
                stats.put("threadPool", threadPoolStats);
            }
        } catch (Exception e) {
            log.debug("Thread Pool 통계 조회 실패: {}", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 캐시 초기화 (관리용)
     */
    public void clearCache() {
        userCache.invalidateAll();
        sessionExistsCache.invalidateAll();
        log.info("모든 캐시 초기화 완료");
    }
    
    /**
     * Redis에서 직접 사용자 조회 (캐시 우회) - 비동기 효과 테스트용
     */
    public User getUserDirectFromRedis(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }

        // 캐시 우회하고 직접 Redis에서 조회
        String sessionKey = "session:" + sessionId;
        Object obj = redisTemplate.opsForValue().get(sessionKey);

        if (obj == null) {
            log.debug("Redis에서 세션 없음: {}", sessionId);
            return null;
        }

        User user;
        try {
            if (obj instanceof User) {
                user = (User) obj;
            } else if (obj instanceof Map) {
                user = objectMapper.convertValue(obj, User.class);
            } else {
                log.warn("알 수 없는 세션 데이터 타입: {}", obj.getClass().getName());
                return null;
            }
            
            // 사용자 정보 업데이트
            user.updateLastAccessTime();
            
            // 🟢 여기서 TTL 연장 - @Async 효과 확인 가능
            sessionAsyncService.extendSessionTTLAsync(sessionId, user);
            
            log.debug("Redis에서 직접 사용자 정보 조회: {}", user.getUserId());
            return user;
            
        } catch (Exception e) {
            log.error("사용자 정보 역직렬화 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 특정 세션 캐시 무효화
     */
    public void invalidateSessionCache(String sessionId) {
        if (sessionId != null) {
            userCache.invalidate(sessionId);
            sessionExistsCache.invalidate(sessionId);
            log.debug("세션 캐시 무효화: {}", sessionId);
        }
    }
}
