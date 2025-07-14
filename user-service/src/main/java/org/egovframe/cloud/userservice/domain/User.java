package org.egovframe.cloud.userservice.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egovframe.cloud.userservice.config.CustomLocalDateTimeSerializer;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * org.egovframe.cloud.userservice.domain.User
 * <p>
 * 사용자 도메인 모델 (Redis Session 저장용)
 *
 * @version 1.0
 * @since 2025/07/14
 */
@Data
@NoArgsConstructor
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String username;
    private String email;
    private String role;
    private String sessionId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime loginTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastAccessTime;
    
    /**
     * User 생성자
     *
     * @param userId 사용자 ID
     * @param username 사용자명
     * @param email 이메일
     * @param role 사용자 역할
     */
    @Builder
    public User(String userId, String username, String email, String role, String sessionId) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.sessionId = sessionId;
        this.loginTime = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
    }
    
    /**
     * 마지막 접근 시간을 현재 시간으로 업데이트한다
     */
    public void updateLastAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
    }
}
