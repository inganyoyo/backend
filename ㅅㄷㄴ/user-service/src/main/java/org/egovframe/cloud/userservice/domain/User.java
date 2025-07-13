package org.egovframe.cloud.userservice.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 사용자 도메인 모델 (Redis Session 저장용)
 */
@Data
@NoArgsConstructor
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String username;
    private String email;
    private String role;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;
    
    @Builder
    public User(String userId, String username, String email, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.loginTime = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
    }
    
    public void updateLastAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
    }
}
