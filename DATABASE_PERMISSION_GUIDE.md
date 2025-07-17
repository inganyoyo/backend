# 🗄️ Database 기반 권한 관리 시스템

기존 JSON 파일 기반 권한 관리를 PostgreSQL 데이터베이스 기반으로 개선한 시스템입니다.

## 🚀 **주요 특징**

- ✅ **PostgreSQL** 기반 권한 저장
- ✅ **10분 자동 갱신** (변경 감지 후 캐시 업데이트)
- ✅ **권한 상속** 지원 (ADMIN ← USER ← ANONYMOUS)
- ✅ **Docker** 기반 개발 환경
- ✅ **MyBatis** ORM 사용
- ✅ **간단한 관리 API** 제공

## 📋 **시스템 구조**

```
📦 Database Permission System
├── 🐘 PostgreSQL
│   ├── roles (역할 정보)
│   ├── permissions (권한 정보)
│   ├── role_permissions (역할-권한 매핑)
│   └── role_hierarchy (역할 상속 관계)
├── ⚡ 10분 스케줄러
│   └── DatabasePermissionService.refreshAllPermissions()
├── 🔧 캐시 시스템
│   └── ConcurrentHashMap 인메모리 캐시
└── 🌐 관리 API
    ├── POST /api/admin/permissions/cache/refresh
    └── GET /api/admin/permissions/cache/status
```

## 🛠️ **설치 및 실행**

### **1단계: 데이터베이스 시작**

```bash
# Docker로 PostgreSQL + Redis 시작
./start-database.sh
```

### **2단계: 애플리케이션 실행**

```bash
# user-service 디렉토리에서
cd user-service
./gradlew bootRun
```

### **3단계: 권한 확인**

```bash
# 권한 캐시 상태 확인
curl http://localhost:8001/api/admin/permissions/cache/status

# 수동 권한 갱신 (필요시)
curl -X POST http://localhost:8001/api/admin/permissions/cache/refresh
```

## 📊 **데이터베이스 스키마**

### **역할 테이블 (roles)**
```sql
role_id | role_name    | description
--------|--------------|----------------------------------
1       | ANONYMOUS    | 익명 사용자 (비로그인) 권한
2       | USER         | 로그인 사용자 권한
3       | ADMIN        | 관리자 권한
4       | SYSTEM_ADMIN | 시스템 관리자 권한
```

### **권한 상속 관계 (role_hierarchy)**
```
SYSTEM_ADMIN ← ADMIN ← USER ← ANONYMOUS
     🔑           🔑      🔑       🔑
   모든권한      관리권한  사용자권한  기본권한
```

## ⏰ **자동 갱신 시스템**

### **갱신 주기**
- **10분마다** 자동 권한 데이터 확인
- **변경 감지 시에만** 캐시 갱신 (해시 비교)
- **애플리케이션 시작 시** 초기 로드

### **갱신 로직**
```java
@Scheduled(fixedDelay = 600000) // 10분
public void refreshAllPermissions() {
    String currentHash = permissionMapper.getPermissionDataHash();
    
    if (!currentHash.equals(lastPermissionHash)) {
        // 권한 데이터 변경됨 → 캐시 갱신
        loadAllRolePermissions();
        lastPermissionHash = currentHash;
    }
}
```

## 🔧 **관리 방법**

### **1. 권한 데이터 확인**
```sql
-- 역할별 권한 수 확인
SELECT r.role_name, COUNT(p.permission_id) as permission_count
FROM roles r
LEFT JOIN role_permissions rp ON r.role_id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.permission_id
GROUP BY r.role_name;

-- 특정 역할의 모든 권한 조회 (상속 포함)
WITH RECURSIVE role_tree AS (
    SELECT role_id, role_name FROM roles WHERE role_name = 'ADMIN'
    UNION ALL
    SELECT r.role_id, r.role_name 
    FROM roles r
    INNER JOIN role_hierarchy rh ON r.role_id = rh.parent_role_id
    INNER JOIN role_tree rt ON rh.child_role_id = rt.role_id
)
SELECT DISTINCT p.service_name, p.http_method, p.path_pattern, p.description
FROM permissions p
INNER JOIN role_permissions rp ON p.permission_id = rp.permission_id
INNER JOIN role_tree rt ON rp.role_id = rt.role_id;
```

### **2. 권한 추가/수정**
```sql
-- 새 권한 추가
INSERT INTO permissions (service_name, http_method, path_pattern, description) 
VALUES ('user-service', 'GET', '/api/users/new-feature', '새 기능 API');

-- 역할에 권한 할당
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM roles r, permissions p 
WHERE r.role_name = 'USER' 
AND p.path_pattern = '/api/users/new-feature';
```

### **3. 캐시 갱신**
```bash
# 권한 변경 후 즉시 반영하려면
curl -X POST http://localhost:8001/api/admin/permissions/cache/refresh
```

## 📈 **성능 특징**

| 항목 | 이전 (JSON) | 현재 (DB+Cache) |
|------|-------------|----------------|
| 권한 조회 속도 | ~10ms | ~1ms (캐시) |
| 권한 변경 반영 | 재시작 필요 | 최대 10분 |
| 메모리 사용량 | 고정 | 동적 최적화 |
| 확장성 | 제한적 | 무제한 |
| 관리 편의성 | JSON 편집 | SQL + API |

## 🔍 **로그 모니터링**

### **정상 동작 로그**
```
2025-07-18 10:00:00 INFO  - 권한 캐시 갱신 시작...
2025-07-18 10:00:01 INFO  - 권한 데이터 변경 없음 - 캐시 갱신 생략
2025-07-18 10:10:00 INFO  - 권한 캐시 갱신 시작...
2025-07-18 10:10:01 INFO  - 권한 캐시 갱신 완료 - 총 4개 역할, 21개 권한
```

### **권한 검증 로그**
```
2025-07-18 10:05:30 DEBUG - 권한 검증 성공: 역할[USER], 서비스[user-service], 메소드[GET], 경로[/api/users/profile]
```

## 🚨 **문제 해결**

### **권한이 제대로 동작하지 않을 때**
1. 캐시 상태 확인: `GET /api/admin/permissions/cache/status`
2. 수동 갱신: `POST /api/admin/permissions/cache/refresh`
3. 로그 확인: `docker-compose logs -f`
4. DB 연결 확인: `docker-compose exec postgres pg_isready`

### **초기화 (데이터 리셋)**
```bash
# 모든 데이터 삭제 후 재생성
docker-compose down -v
docker-compose up -d
```

## 📝 **추가 개선 사항 (향후)**

- [ ] 권한 변경 이벤트 실시간 알림 (Redis Pub/Sub)
- [ ] 권한 변경 이력 관리
- [ ] 웹 기반 권한 관리 UI
- [ ] 권한 변경 승인 워크플로우
- [ ] 성능 메트릭 수집 및 모니터링

---

**📞 문의사항이나 개선 제안이 있으시면 언제든지 알려주세요!** 🚀
