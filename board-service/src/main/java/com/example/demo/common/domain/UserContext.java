package com.example.demo.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.servlet.http.HttpServletRequest;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private String userId;
    private String role;
    private String email;
    private String username;
    private String sessionId;

    public static UserContext fromHeaders(HttpServletRequest request) {
        return UserContext.builder()
                .userId(request.getHeader("X-User-ID"))
                .role(request.getHeader("X-User-Role"))
                .email(request.getHeader("X-User-Email"))
                .username(request.getHeader("X-Username"))
                .sessionId(request.getHeader("X-Session-ID"))
                .build();
    }

    public boolean isAuthenticated() {
        return userId != null && !userId.trim().isEmpty();
    }
}
