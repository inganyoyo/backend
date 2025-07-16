//// WebFluxSecurityConfig.java
//@Configuration
//@EnableWebFluxSecurity
//@RequiredArgsConstructor
//public class WebFluxSecurityConfig {
//
//    private final ReactiveAuthorizationManager<AuthorizationContext> authorizationManager;
//    private final ServerAccessDeniedHandler accessDeniedHandler;
//    private final ServerAuthenticationEntryPoint authenticationEntryPoint;
//
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        return http
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                .headers(headers -> headers.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
//                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
//                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
//                .authorizeExchange(exchange -> exchange
//                        .pathMatchers("/login", "/public/**").permitAll()
//                        .anyExchange().access(authorizationManager)
//                )
//                .exceptionHandling(exception -> exception
//                        .accessDeniedHandler(accessDeniedHandler)
//                        .authenticationEntryPoint(authenticationEntryPoint)
//                )
//                .build();
//    }
//}
//
//// ReactiveAuthorization.java
//@Component
//@RequiredArgsConstructor
//public class ReactiveAuthorization implements ReactiveAuthorizationManager<AuthorizationContext> {
//
//    private final AuthClient authClient; // WebClient로 user-service 호출
//
//    @Override
//    public Mono<AuthorizationDecision> check(Mono<Authentication> authenticationMono, AuthorizationContext context) {
//        ServerHttpRequest request = context.getExchange().getRequest();
//        String path = request.getURI().getPath();
//        String method = request.getMethodValue();
//
//        return authClient.checkAuthorization(request) // user-service로 인증 요청
//                .flatMap(authResponse -> {
//                    boolean granted = authResponse != null && authResponse.isGranted();
//
//                    if (!granted) {
//                        int code = authResponse.getCode();
//                        if (code == 401) {
//                            return Mono.error(new AuthenticationCredentialsNotFoundException("인증 필요"));
//                        } else if (code == 403) {
//                            return Mono.error(new AccessDeniedException("접근 권한 없음"));
//                        } else {
//                            return Mono.error(new RuntimeException("알 수 없는 인증 오류"));
//                        }
//                    }
//                    return Mono.just(new AuthorizationDecision(true));
//                })
//                .onErrorResume(e -> {
//                    if (e instanceof AuthenticationCredentialsNotFoundException || e instanceof AccessDeniedException) {
//                        return Mono.error(e);
//                    }
//                    return Mono.error(new RuntimeException("Authorization 처리 중 오류 발생", e));
//                });
//    }
//}
//
//// AuthResponse.java
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class AuthResponse {
//    private boolean granted;
//    private int code; // 200, 401, 403 등
//    private UserDto user;
//}
//
//// AuthClient.java (예시)
//@Component
//@RequiredArgsConstructor
//public class AuthClient {
//    private final WebClient webClient;
//
//    public Mono<AuthResponse> checkAuthorization(ServerHttpRequest request) {
//        return webClient.post()
//                .uri("http://user-service/internal/auth/check")
//                .headers(headers -> headers.addAll(request.getHeaders()))
//                .retrieve()
//                .bodyToMono(AuthResponse.class)
//                .onErrorResume(e -> Mono.just(new AuthResponse(false, 401, null))); // fallback 처리
//    }
//}
