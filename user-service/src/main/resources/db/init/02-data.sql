-- ==========================================
-- 전자정부 프레임워크 권한 초기 데이터
-- ==========================================

-- 1. 역할 데이터 삽입
INSERT INTO roles (role_name, description) VALUES 
('ANONYMOUS', '익명 사용자 (비로그인) 권한 - 기본 권한'),
('USER', '로그인 사용자 권한 - ANONYMOUS 권한을 자동 상속받음'),
('ADMIN', '관리자 권한 - USER 권한을 자동 상속받음'),
('SYSTEM_ADMIN', '시스템 관리자 권한 - ADMIN 권한을 자동 상속받음')
ON CONFLICT (role_name) DO NOTHING;

-- 2. 권한 데이터 삽입

-- ANONYMOUS 권한
INSERT INTO permissions (service_name, http_method, path_pattern, description) VALUES 
('auth-service', 'POST', '/api/auth/login', '로그인 API'),
('user-service', 'POST', '/api/auth/login', '로그인 API'),
('user-service', 'POST', '/api/auth/logout', '로그아웃 API'),
('auth-service', 'POST', '/api/auth/logout', '로그아웃 API'),
('auth-service', 'GET', '/api/auth/validate', '세션 검증 API (Gateway용)'),
('user-service', 'GET', '/api/auth/validate', '세션 검증 API (user-service 임시)'),
('user-service', 'GET', '/api/users/public-info', '공개 사용자 정보 조회'),
('board-service', 'GET', '/api/board/hello', '게시판 Hello API (익명 접근 가능)'),
('board-service', 'GET', '/api/boards/public/**', '공개 게시판 조회'),
('*', 'GET', '/api/*/health', '서비스 헬스체크'),

-- USER 권한
('auth-service', 'GET', '/api/auth/validate-and-authorize', '인증 및 권한 검증'),
('user-service', 'GET', '/api/users/profile', '사용자 프로필 조회'),
('user-service', 'POST', '/api/users/profile', '사용자 프로필 수정'),
('board-service', 'GET', '/api/boards/**', '모든 게시판 조회 (공개 + 비공개)'),
('board-service', 'POST', '/api/boards/*/posts', '게시글 작성'),
('user-service', 'GET', '/api/test', '테스트 API 호출'),

-- ADMIN 권한
('*', 'GET', '/api/**', '모든 GET 요청'),
('*', 'POST', '/api/**', '모든 POST 요청'),
('*', 'PUT', '/api/**', '모든 PUT 요청'),
('*', 'DELETE', '/api/**', '모든 DELETE 요청'),

-- SYSTEM_ADMIN 권한
('*', '*', '/**', '시스템 전체 권한')
ON CONFLICT DO NOTHING;

-- 3. 역할-권한 매핑

-- ANONYMOUS 역할 권한 매핑 (1~10번 권한)
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.role_id, p.permission_id 
FROM roles r, permissions p 
WHERE r.role_name = 'ANONYMOUS' 
AND p.permission_id BETWEEN 1 AND 10;

-- USER 역할 권한 매핑 (11~16번 권한)
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.role_id, p.permission_id 
FROM roles r, permissions p 
WHERE r.role_name = 'USER' 
AND p.permission_id BETWEEN 11 AND 16;

-- ADMIN 역할 권한 매핑 (17~20번 권한)
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.role_id, p.permission_id 
FROM roles r, permissions p 
WHERE r.role_name = 'ADMIN' 
AND p.permission_id BETWEEN 17 AND 20;

-- SYSTEM_ADMIN 역할 권한 매핑 (21번 권한)
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.role_id, p.permission_id 
FROM roles r, permissions p 
WHERE r.role_name = 'SYSTEM_ADMIN' 
AND p.permission_id = 21;

-- 4. 역할 상속 관계 설정
INSERT INTO role_hierarchy (parent_role_id, child_role_id)
SELECT parent.role_id, child.role_id
FROM roles parent, roles child
WHERE (parent.role_name = 'ANONYMOUS' AND child.role_name = 'USER')
   OR (parent.role_name = 'ANONYMOUS' AND child.role_name = 'ADMIN') 
   OR (parent.role_name = 'USER' AND child.role_name = 'ADMIN')
   OR (parent.role_name = 'ANONYMOUS' AND child.role_name = 'SYSTEM_ADMIN')
   OR (parent.role_name = 'USER' AND child.role_name = 'SYSTEM_ADMIN')
   OR (parent.role_name = 'ADMIN' AND child.role_name = 'SYSTEM_ADMIN');

-- 5. 테스트 사용자 (선택사항)
-- CREATE TABLE IF NOT EXISTS users (
--     user_id SERIAL PRIMARY KEY,
--     username VARCHAR(50) UNIQUE NOT NULL,
--     password VARCHAR(100) NOT NULL,
--     role_name VARCHAR(50) REFERENCES roles(role_name),
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- );

-- INSERT INTO users (username, password, role_name) VALUES 
-- ('admin', '$2a$10$example...', 'ADMIN'),
-- ('user', '$2a$10$example...', 'USER');

-- 확인 쿼리 (로그로 출력)
DO $$
DECLARE
    role_count INTEGER;
    permission_count INTEGER;
    mapping_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO role_count FROM roles;
    SELECT COUNT(*) INTO permission_count FROM permissions;
    SELECT COUNT(*) INTO mapping_count FROM role_permissions;
    
    RAISE NOTICE '=== 초기 데이터 삽입 완료 ===';
    RAISE NOTICE '역할 수: %', role_count;
    RAISE NOTICE '권한 수: %', permission_count;
    RAISE NOTICE '매핑 수: %', mapping_count;
    RAISE NOTICE '권한 해시: %', get_permission_data_hash();
END $$;
