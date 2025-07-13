package org.egovframe.cloud.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증 결과 DTO
 * Gateway와 User Service 간 인증/권한 결과 전달용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {
    private boolean authenticated;
    private boolean authorized;
    private String userId;
    private String role;
    private String message;
    
    public static AuthResult unauthenticated() {
        return AuthResult.builder()
            .authenticated(false)
            .authorized(false)
            .message("인증되지 않은 사용자")
            .build();
    }
    
    public static AuthResult unauthorized(String userId, String role) {
        return AuthResult.builder()
            .authenticated(true)
            .authorized(false)
            .userId(userId)
            .role(role)
            .message("권한이 없음")
            .build();
    }
    
    public static AuthResult authorized(String userId, String role) {
        return AuthResult.builder()
            .authenticated(true)
            .authorized(true)
            .userId(userId)
            .role(role)
            .message("인증 및 권한 확인 완료")
            .build();
    }
}
