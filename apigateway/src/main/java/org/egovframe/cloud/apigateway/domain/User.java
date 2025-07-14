package org.egovframe.cloud.apigateway.domain;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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
    

    
    /**
     * User 생성자
     *
     * @param userId 사용자 ID
     * @param username 사용자명
     * @param email 이메일
     * @param role 사용자 역할
     */
    @Builder
    public User(String userId, String username, String email, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
    }
    

}
