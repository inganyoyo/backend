package org.egovframe.cloud.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.User;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 최적화된 인증 서비스
 * Redis Session 기반 + Caffeine 로컬 캐시로 성능 향상
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    // 테스트용 사용자 정보 (실제로는 외부 시스템이나 파일에서 로드)
    private static final Map<String, String[]> USERS = new HashMap<>();

    static {
        // {username, password, email, role}
        USERS.put("user1", new String[]{"user1", "user123", "user1@example.com", "USER"});
        USERS.put("admin", new String[]{"admin", "admin123", "admin@example.com", "ADMIN"});
        USERS.put("system", new String[]{"system", "system123", "system@example.com", "SYSTEM_ADMIN"});
    }

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 사용자 로그인
     */
    public User login(String username, String password) {
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
                .sessionId(sessionId)
                .build();
        
        // Redis에 세션 저장 (30분 TTL)
//        String sessionKey = "session:" + sessionId;
//        redisTemplate.opsForValue().set(sessionKey, user, 30, TimeUnit.MINUTES);
        
        // 캐시에 저장
//        userCache.put(sessionId, user);
//        sessionExistsCache.put(sessionId, true);
//
        log.info("로그인 성공: {} (세션: {})", username, sessionId);
        return user;
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
    
}
