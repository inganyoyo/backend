# 🔐 User Service

**eGovFrame Cloud 마이크로서비스 아키텍처**의 사용자 인증 및 권한 관리 서비스입니다.

## 📋 목차
- [개요](#개요)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [설치 및 실행](#설치-및-실행)
- [API 문서](#api-문서)
- [설정 가이드](#설정-가이드)
- [보안 구성](#보안-구성)
- [모니터링](#모니터링)
- [개발 가이드](#개발-가이드)

---

## 🎯 개요

User Service는 eGovFrame Cloud MSA의 핵심 인증 서비스로, 사용자 관리, 세션 관리, 권한 검증을 담당합니다.

### 🏗️ 아키텍처 특징
- **Redis 기반 세션 관리**: 분산 환경에서 세션 공유
- **역할 기반 접근 제어 (RBAC)**: JSON 기반 권한 설정
- **API Gateway 통합**: 헤더 기반 인증 검증
- **비동기 세션 처리**: 성능 최적화를 위한 비동기 로직

---

## ⚡ 주요 기능

### 🔑 인증 관리
- **사용자 로그인/로그아웃**: 세션 기반 인증
- **세션 검증**: API Gateway를 위한 실시간 세션 확인
- **권한 체크**: 서비스별 API 접근 권한 검증

### 👥 사용자 관리
- **사용자 프로필 조회**: 인증된 사용자 정보 조회
- **권한 기반 접근 제어**: 4단계 권한 체계
  - `anonymous`: 익명 사용자
  - `user`: 일반 사용자
  - `admin`: 관리자
  - `system-admin`: 시스템 관리자

### 🚀 성능 최적화
- **Redis 연결 풀링**: 최적화된 Redis 설정
- **Caffeine 캐싱**: 권한 정보 메모리 캐싱
- **비동기 세션 처리**: 논블로킹 세션 관리

---

## 🛠️ 기술 스택

| 분류 | 기술 | 버전 | 용도 |
|------|------|------|------|
| **Framework** | Spring Boot | 2.7.18 | 메인 프레임워크 |
| **Security** | Spring Security | 5.7.x | 보안 및 인증 |
| **Cache** | Redis | - | 세션 저장소 |
| **Local Cache** | Caffeine | - | 메모리 캐싱 |
| **Documentation** | SpringDoc OpenAPI | 1.7.0 | API 문서화 |
| **Monitoring** | Spring Actuator | - | 헬스 체크 |
| **Utility** | Lombok | - | 코드 간소화 |

---

## 📁 프로젝트 구조

```
src/main/java/org/egovframe/cloud/
├── userservice/
│   ├── UserServiceApplication.java    # 메인 애플리케이션
│   ├── api/
│   │   └── AuthController.java        # 인증 API 컨트롤러
│   ├── config/
│   │   ├── SecurityConfig.java        # Spring Security 설정
│   │   ├── RedisConfig.java          # Redis 설정
│   │   ├── PermissionJsonConfig.java  # 권한 JSON 로더
│   │   └── AsyncConfig.java          # 비동기 설정
│   ├── domain/
│   │   ├── User.java                 # 사용자 엔티티
│   │   └── Permission.java           # 권한 엔티티
│   ├── dto/
│   │   ├── AuthCheckResponse.java    # 권한 체크 응답
│   │   └── AuthResult.java           # 인증 결과
│   ├── filter/
│   │   ├── AuthenticationFilter.java # 인증 필터
│   │   └── SessionValidationFilter.java # 세션 검증 필터
│   └── service/
│       ├── AuthService.java          # 인증 서비스
│       ├── AuthorizationService.java # 권한 서비스
│       └── SessionAsyncService.java  # 비동기 세션 서비스
├── common/
│   ├── config/                       # 공통 설정
│   ├── exception/                    # 예외 처리
│   └── util/                        # 유틸리티
└── servlet/
    └── exception/                    # 서블릿 예외 처리

resources/
├── application.yml                   # 메인 설정
├── permissions/                      # 권한 설정 JSON
│   ├── anonymous.json               # 익명 사용자 권한
│   ├── user.json                    # 일반 사용자 권한
│   ├── admin.json                   # 관리자 권한
│   └── system-admin.json            # 시스템 관리자 권한
└── logback-spring.xml               # 로깅 설정
```

---

## 🚀 설치 및 실행

### 📋 사전 요구사항
- **Java**: 8+ (권장: 11+)
- **Redis**: 6.0+
- **Gradle**: 7.4+

### ⚙️ 환경 설정

1. **Redis 서버 실행**
   ```bash
   # Docker로 Redis 실행
   docker run -d --name redis -p 6379:6379 redis:alpine
   
   # 또는 로컬 Redis 설치 후 실행
   redis-server
   ```

2. **환경변수 설정** (선택사항)
   ```bash
   export SPRING_PROFILES_ACTIVE=local
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   ```

### 🏃‍♂️ 실행 방법

#### Gradle로 실행
```bash
# 프로젝트 루트에서
./gradlew :user-service:bootRun

# 또는 user-service 디렉토리에서
cd user-service
./gradlew bootRun
```

#### JAR 파일로 실행
```bash
# 빌드
./gradlew :user-service:build

# 실행
java -jar user-service/build/libs/user-service-1.0.0.jar
```

#### IDE에서 실행
`UserServiceApplication.java`의 main 메서드 실행

### ✅ 실행 확인
```bash
# 헬스 체크
curl http://localhost:8001/actuator/health

# 응답 예시
{
  "status": "UP",
  "components": {
    "redis": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

---

## 📚 API 문서

### 🔑 인증 API

#### 로그인
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

**응답:**
```json
{
  "success": true,
  "message": "로그인 성공",
  "sessionId": "abc123-def456-ghi789"
}
```

#### 로그아웃
```http
POST /api/auth/logout
X-Session-ID: abc123-def456-ghi789
```

#### 세션 검증
```http
GET /api/auth/validate
X-Session-ID: abc123-def456-ghi789
```

**응답:** `true` 또는 `false`

#### 권한 체크 (API Gateway용)
```http
GET /api/auth/check?httpMethod=GET&requestPath=/api/users/profile
X-Session-ID: abc123-def456-ghi789
X-Service-ID: user-service
```

**응답:**
```json
{
  "isAuthorized": true,
  "user": {
    "userId": "admin",
    "username": "admin",
    "email": "admin@example.com",
    "role": "admin"
  }
}
```

### 👤 사용자 API

#### 프로필 조회
```http
GET /api/users/profile
X-Session-ID: abc123-def456-ghi789
```

### 📊 모니터링 API

#### 헬스 체크
```http
GET /actuator/health
```

#### 애플리케이션 정보
```http
GET /actuator/info
```

---

## ⚙️ 설정 가이드

### 🔧 application.yml 주요 설정

```yaml
server:
  port: 8001                          # 서비스 포트

spring:
  application:
    name: user-service                # 서비스 이름
  
  # Redis 설정 (성능 최적화)
  redis:
    host: localhost
    port: 6379
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 20                # 최대 연결 수
        max-wait: 2000ms             # 대기 시간
        max-idle: 10                 # 최대 유휴 연결
        min-idle: 2                  # 최소 유휴 연결
  
  # 세션 설정
  session:
    store-type: redis
    timeout: 1800                    # 30분
  
  # 캐시 설정
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=5000,expireAfterWrite=10m

# 모니터링 설정
management:
  endpoints:
    web:
      exposure:
        include: health, info, refresh
```

### 🔒 권한 설정 (JSON)

권한은 `src/main/resources/permissions/` 디렉토리의 JSON 파일로 관리됩니다.

#### 예시: admin.json
```json
{
  "role": "admin",
  "description": "관리자 권한",
  "inherits": ["user"],
  "permissions": [
    {
      "service": "user-service",
      "method": "*",
      "path": "/api/admin/**",
      "description": "관리자 API 접근"
    },
    {
      "service": "*",
      "method": "GET",
      "path": "/api/*/admin/**",
      "description": "모든 서비스 관리자 API 조회"
    }
  ]
}
```

### 🌐 환경별 설정

#### 개발 환경 (application-dev.yml)
```yaml
spring:
  redis:
    host: localhost
logging:
  level:
    org.egovframe.cloud.userservice: DEBUG
```

#### 운영 환경 (application-prod.yml)
```yaml
spring:
  redis:
    host: redis-cluster.internal
    password: ${REDIS_PASSWORD}
logging:
  level:
    root: WARN
```

---

## 🛡️ 보안 구성

### 🔐 Spring Security 설정

```java
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .build();
    }
}
```

### 🔒 세션 보안

- **세션 ID 보안**: UUID 기반 안전한 세션 ID
- **세션 타임아웃**: 30분 비활성 시 자동 만료
- **Redis 암호화**: 민감정보 Redis 저장 시 암호화
- **HTTPS 강제**: 운영 환경에서 HTTPS 필수

### 🚫 보안 헤더

```yaml
# application.yml
server:
  servlet:
    session:
      cookie:
        secure: true        # HTTPS 필수
        http-only: true     # XSS 방지
        same-site: strict   # CSRF 방지
```

---

## 📊 모니터링

### 🔍 로깅

#### 로그 레벨 설정
```yaml
logging:
  level:
    org.egovframe.cloud.userservice: debug
    org.egovframe.cloud.userservice.filter: debug
    org.springframework.security: info
    org.springframework.scheduling: info
```

#### 주요 로그 포인트
- **인증 요청**: 로그인/로그아웃 시도
- **권한 체크**: API 접근 권한 검증
- **세션 관리**: 세션 생성/갱신/만료
- **에러 처리**: 예외 발생 및 처리

### 📈 메트릭

#### Actuator 엔드포인트
```http
GET /actuator/health     # 서비스 상태
GET /actuator/info       # 애플리케이션 정보
GET /actuator/metrics    # 성능 메트릭
```

#### 커스텀 메트릭
- **로그인 성공/실패 횟수**
- **세션 생성/만료 횟수**
- **권한 체크 응답 시간**
- **Redis 연결 상태**

### 🚨 알람 설정

#### Redis 연결 모니터링
```bash
# Redis 연결 상태 확인
curl -s http://localhost:8001/actuator/health | jq '.components.redis.status'
```

#### 응답 시간 모니터링
```bash
# 권한 체크 API 응답 시간
time curl "http://localhost:8001/api/auth/check?httpMethod=GET&requestPath=/test"
```

---

## 🔧 개발 가이드

### 🏗️ 새로운 권한 추가

1. **권한 JSON 파일 수정**
   ```json
   {
     "permissions": [
       {
         "service": "new-service",
         "method": "POST",
         "path": "/api/new/**",
         "description": "새로운 API 접근"
       }
     ]
   }
   ```

2. **권한 검증 로직 확인**
   ```java
   @Service
   public class AuthorizationService {
       public boolean isAuthorization(Authentication auth, String path, String method, String service) {
           // 권한 검증 로직
       }
   }
   ```

### 🔌 API Gateway 연동

1. **헤더 설정 확인**
   ```http
   X-Session-ID: 세션ID
   X-Service-ID: 서비스명 (선택)
   ```

2. **권한 체크 API 호출**
   ```java
   // API Gateway에서 사용
   GET /api/auth/check?httpMethod={METHOD}&requestPath={PATH}
   ```

### 🧪 테스트

#### 단위 테스트
```bash
./gradlew :user-service:test
```

#### 통합 테스트
```bash
# Redis 의존성 포함
./gradlew :user-service:integrationTest
```

#### API 테스트
```bash
# 로그인 테스트
curl -X POST http://localhost:8001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

### 🔄 배포

#### Docker 빌드
```dockerfile
FROM openjdk:11-jre-slim
COPY build/libs/user-service-1.0.0.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

#### 환경 변수
```bash
# 운영 환경 변수
SPRING_PROFILES_ACTIVE=prod
REDIS_HOST=redis.internal
REDIS_PASSWORD=secure_password
```

---

## 🔗 관련 링크

- **API Gateway**: [../apigateway/README.md](../apigateway/README.md)
- **Board Service**: [../board-service/README.md](../board-service/README.md)
- **Module Common**: [../module-common/README.md](../module-common/README.md)
- **eGovFrame**: [https://www.egovframe.go.kr/](https://www.egovframe.go.kr/)

---

## 📞 문의

- **이슈 신고**: GitHub Issues
- **기술 지원**: 개발팀 연락처
- **문서 개선**: Pull Request 환영

---

*마지막 업데이트: 2025년 7월 14일*