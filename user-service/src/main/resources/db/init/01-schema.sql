-- ==========================================
-- 전자정부 프레임워크 권한 관리 스키마
-- ==========================================

-- 1. 역할(Role) 테이블
CREATE TABLE IF NOT EXISTS roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 권한(Permission) 테이블  
CREATE TABLE IF NOT EXISTS permissions (
    permission_id SERIAL PRIMARY KEY,
    service_name VARCHAR(50) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    path_pattern VARCHAR(200) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 역할-권한 매핑 테이블
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INTEGER REFERENCES roles(role_id) ON DELETE CASCADE,
    permission_id INTEGER REFERENCES permissions(permission_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

-- 4. 역할 상속 관계 테이블 (ADMIN이 USER 권한 상속 등)
CREATE TABLE IF NOT EXISTS role_hierarchy (
    parent_role_id INTEGER REFERENCES roles(role_id) ON DELETE CASCADE,
    child_role_id INTEGER REFERENCES roles(role_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (parent_role_id, child_role_id)
);

-- 인덱스 생성 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_permissions_service_method ON permissions(service_name, http_method);
CREATE INDEX IF NOT EXISTS idx_role_hierarchy_child ON role_hierarchy(child_role_id);

-- 권한 데이터 해시 함수 (캐시 무효화용)
CREATE OR REPLACE FUNCTION get_permission_data_hash()
RETURNS TEXT AS $$
BEGIN
    RETURN md5(
        COALESCE(
            (SELECT string_agg(
                CONCAT(r.role_name, ':', p.service_name, ':', p.http_method, ':', p.path_pattern),
                '|' ORDER BY r.role_name, p.service_name, p.http_method, p.path_pattern
            )
            FROM roles r
            JOIN role_permissions rp ON r.role_id = rp.role_id  
            JOIN permissions p ON rp.permission_id = p.permission_id),
            'empty'
        )
    );
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE roles IS '사용자 역할 정보';
COMMENT ON TABLE permissions IS '시스템 권한 정보';
COMMENT ON TABLE role_permissions IS '역할별 권한 매핑';
COMMENT ON TABLE role_hierarchy IS '역할 상속 관계';
