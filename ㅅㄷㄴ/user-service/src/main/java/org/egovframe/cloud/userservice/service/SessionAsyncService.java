package org.egovframe.cloud.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.User;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 세션 관련 비동기 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionAsyncService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 비동기로 세션 TTL 연장
     */
    @Async("sessionAsyncExecutor")
    public void extendSessionTTLAsync(String sessionId, User user) {
        try {
            String asyncThreadName = Thread.currentThread().getName();
            long asyncThreadId = Thread.currentThread().getId();
            
            log.debug("🚀 비동기 TTL 연장 시작: {} (Thread: {} - ID: {})", 
                    sessionId, asyncThreadName, asyncThreadId);
            
            String sessionKey = "session:" + sessionId;
            
            // Redis에서 TTL 연장 (30분)
            redisTemplate.opsForValue().set(sessionKey, user, 30, TimeUnit.MINUTES);
            
            log.debug("✅ 비동기 TTL 연장 완료: {} (Thread: {} - ID: {})", 
                    sessionId, asyncThreadName, asyncThreadId);
            
        } catch (Exception e) {
            log.error("❌ 비동기 TTL 연장 실패: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }
}
