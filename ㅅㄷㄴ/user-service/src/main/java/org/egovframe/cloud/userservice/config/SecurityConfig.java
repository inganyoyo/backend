package org.egovframe.cloud.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * API 기반이므로 기본 인증은 비활성화하고 세션 관리는 Redis에서 처리
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .formLogin().disable()
            .httpBasic().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(authz -> authz
                // 🟢 핵심 인증 API만 허용
                .antMatchers("/api/v1/auth/login").permitAll()
                .antMatchers("/api/v1/auth/logout").permitAll()
                .antMatchers("/api/v1/auth/validate").permitAll()
                .antMatchers("/api/v1/auth/validate-and-authorize").permitAll()
                .antMatchers("/api/v1/users/profile").permitAll()
                .antMatchers("/api/v1/authorizations/check").permitAll()
                .antMatchers("/api/v1/admin/cache/**").permitAll() // 캐시 모니터링 API
                .antMatchers("/api/v1/test/**").permitAll() // 테스트 API
                // 🟢 나중에 필요시 추가할 경로들은 제거
                // .antMatchers("/actuator/**").permitAll()
                // .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()
                .anyRequest().authenticated()
            );
            
        return http.build();
    }
    
    /**
     * 🟢 기본 UserDetailsService 비활성화 (경고 메시지 제거)
     * 우리는 Redis 세션 기반 인증을 사용하므로 UserDetailsService 불필요
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("UserDetailsService is disabled. Use Redis session-based authentication.");
        };
    }
}
