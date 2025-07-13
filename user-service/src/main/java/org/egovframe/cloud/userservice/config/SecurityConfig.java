package org.egovframe.cloud.userservice.config;

import static org.egovframe.cloud.userservice.config.SecurityConstants.PERMIT_ALL_PATTERNS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.userservice.filter.AuthenticationFilter;
import org.egovframe.cloud.userservice.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * User Service Spring Security 설정
 * 세션 기반 인증과 동적 권한 검증
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthService authService;

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authConfiguration) throws Exception {
        return authConfiguration.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        AuthenticationManager authenticationManager = authenticationManager(http.getSharedObject(AuthenticationConfiguration.class));
        
        /**
         * 세션 기반 인증정보를 받아 Authentication 객체를 설정할 수 있도록 필터를 등록해준다.
         */
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(authenticationManager, authService);
        
        http
                .csrf().disable()
                .headers().frameOptions().disable()
            .and()
                .formLogin().disable() // 폼 로그인 완전 비활성화
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
                .anonymous() // 🆕 익명 사용자 지원 명시적 활성화
            .and()
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class) // 🆕 addFilterBefore 사용
                .authorizeRequests()
                .antMatchers(PERMIT_ALL_PATTERNS).permitAll() // 🆕 상수 사용
                .anyRequest().access("@authorizationService.isAuthorization(authentication, request.requestURI, request.method, request.getHeader('X-Service-ID'))");


        return http.build();
    }

}
