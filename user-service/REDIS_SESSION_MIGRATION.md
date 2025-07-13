# 🔄 User Service - Redis Session 기반 인증 시스템

## 📋 변경 사항

### ❌ 제거된 기능
- **JWT 토큰 기반 인증** → Redis Session 기반 인증
- **JPA/Database 연동** → 파일 기반 권한 관리
- **복잡한 권한 체계** → 간단한 YAML 설정

### ✅ 새로운 기능
- **Redis Session 관리** (30분 TTL)
- **파일 기반 권한 설정** (application.yml)
- **간단한 사용자 관리** (메모리 기반)

## 🏗️ 아키텍처

```
API Gateway → User Service (Redis Session) → 권한 검증
```

## 📁 프로젝트 구조

```
src/main/java/org/egovframe/cloud/userservice/
├── UserServiceApplication.java          # 메인 애플리케이션
├── domain/
│   ├── User.java                       # 사용자 도메인 모델
│   └── Permission.java                 # 권한 도메인 모델
├── config/
│   ├── RedisConfig.java                # Redis 설정
│   ├── SecurityConfig.java             # Spring Security 설정
│   └── PermissionConfig.java           # 권한 설정 로더
├── service/
│   ├── AuthService.java                # 인증 서비스
│   └── AuthorizationService.java       # 권한 검증 서비스
└── api/
    ├── AuthController.java             # 인증 API
    ├── AuthorizationController.java    # 권한 검증 API (Gateway용)
    ├── UserController.java             # 사용자 API
    └── TestController.java             # 테스트 API
```

## 🔑 테스트 계정

| 사용자명 | 비밀번호 | 역할 | 권한 |
|---------|----------|------|------|
| user1 | user123 | USER | 기본 사용자 권한 |
| admin | admin123 | ADMIN | 관리자 권한 |
| system | system123 | SYSTEM_ADMIN | 최고 권한 |

## 🚀 사용법

### 1. Redis 서버 실행
```bash
# Docker로 Redis 실행
docker run -d -p 6379:6379 redis:latest

# 또는 로컬 Redis 설치 후 실행
redis-server
```

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 3. API 테스트

#### 로그인
```bash
curl -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "password": "user123"}'
```

#### 사용자 정보 조회
```bash
curl -X GET http://localhost:8001/api/v1/users/profile \
  -H "X-Session-ID: {세션ID}"
```

#### 테스트 API 호출
```bash
curl -X GET http://localhost:8001/api/v1/test \
  -H "X-Session-ID: {세션ID}"
```

## 🔒 권한 체계

### USER 권한
- `GET /api/v1/users/profile` - 프로필 조회
- `PUT /api/v1/users/profile` - 프로필 수정
- `GET /api/v1/boards/**` - 게시판 조회
- `POST /api/v1/boards/*/posts` - 게시글 작성
- `GET /api/v1/test` - 테스트 API

### ADMIN 권한
- `GET/POST/PUT/DELETE /api/v1/**` - 모든 API 접근

### SYSTEM_ADMIN 권한
- `* /**` - 시스템 전체 권한

## 🔧 API Gateway 연동

API Gateway의 ReactiveAuthorization.java에서 다음과 같이 수정 필요:

```java
// JWT 토큰 검증 → Session ID 검증으로 변경
String sessionId = request.getHeaders().getFirst("X-Session-ID");

// User Service 권한 체크 호출
String baseUrl = APIGATEWAY_HOST + AUTHORIZATION_URI + 
    "?httpMethod=" + httpMethod + "&requestPath=" + requestPath;

WebClient.create(baseUrl)
    .get()
    .headers(httpHeaders -> {
        httpHeaders.add("X-Session-ID", sessionId);
    })
    .retrieve()
    .bodyToMono(Boolean.class);
```

## 📝 설정 파일

### application.yml 주요 설정
```yaml
spring:
  redis:
    host: localhost
    port: 6379
  session:
    store-type: redis
    timeout: 1800 # 30분

permissions:
  USER:
    - service: "user-service"
      method: "GET"
      path: "/api/v1/users/profile"
      description: "사용자 프로필 조회"
```

## 🔄 세션 관리

- **세션 생성**: 로그인 시 UUID로 세션 ID 생성
- **세션 저장**: Redis에 User 객체 직렬화하여 저장
- **세션 연장**: API 호출 시마다 TTL 갱신 (30분)
- **세션 삭제**: 로그아웃 시 Redis에서 삭제

## 🎯 장점

1. **간단한 구조**: JWT 검증 복잡성 제거
2. **중앙화된 세션 관리**: Redis를 통한 세션 공유
3. **파일 기반 권한**: DB 없이 YAML 설정으로 권한 관리
4. **확장성**: 필요시 점진적으로 기능 추가 가능

## 🚧 향후 개선사항

1. **사용자 정보 외부화**: 파일 또는 외부 시스템 연동
2. **권한 실시간 갱신**: 설정 변경 시 재시작 없이 반영
3. **세션 클러스터링**: Redis Cluster 적용
4. **로그인 실패 제한**: 보안 강화
