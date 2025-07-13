package org.egovframe.cloud.userservice.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.domain.User;
import org.egovframe.cloud.userservice.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * 세션 기반 인증 필터
 * X-Session-ID 헤더를 통해 사용자 인증 처리
 */
@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        // 1. X-Session-ID 헤더에서 세션 ID 추출
        String sessionId = request.getHeader("X-Session-ID");

        if (sessionId != null && !sessionId.trim().isEmpty()) {
            try {
                // 2. 세션 검증 (Redis/Cache에서 사용자 정보 조회)
                User user = authService.getUser(sessionId);

                if (user != null) {
                    // 3. Spring Security Authentication 객체 생성
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                    );

                    // 4. SecurityContext에 인증 정보 설정
                    SecurityContextHolder.getContext().setAuthentication(auth);


                }
            } catch (Exception e) {
                log.warn("세션 검증 중 오류 발생: sessionId={}, error={}",
                        sessionId.substring(0, Math.min(8, sessionId.length())) + "...", e.getMessage());
            }
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
