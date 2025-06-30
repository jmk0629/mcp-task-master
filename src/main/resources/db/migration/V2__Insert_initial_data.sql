-- V2__Insert_initial_data.sql
-- 초기 데이터 삽입

-- 권한 데이터 삽입
INSERT INTO permissions (name, description, resource, action) VALUES
('BOARD_READ', '게시판 읽기', 'BOARD', 'READ'),
('BOARD_WRITE', '게시판 쓰기', 'BOARD', 'WRITE'),
('BOARD_DELETE', '게시판 삭제', 'BOARD', 'DELETE'),
('OPENSTACK_READ', 'OpenStack 읽기', 'OPENSTACK', 'READ'),
('OPENSTACK_WRITE', 'OpenStack 쓰기', 'OPENSTACK', 'WRITE'),
('OPENSTACK_DELETE', 'OpenStack 삭제', 'OPENSTACK', 'DELETE'),
('ADMIN_ACCESS', '관리자 접근', 'SYSTEM', 'ADMIN');

-- 역할 데이터 삽입
INSERT INTO roles (name, description) VALUES
('ADMIN', '시스템 관리자'),
('USER', '일반 사용자'),
('MODERATOR', '중간 관리자');

-- 역할-권한 매핑 (ADMIN 역할)
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'ADMIN';

-- 역할-권한 매핑 (USER 역할)
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'USER' 
AND p.name IN ('BOARD_READ', 'BOARD_WRITE', 'OPENSTACK_READ', 'OPENSTACK_WRITE');

-- 역할-권한 매핑 (MODERATOR 역할)
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'MODERATOR' 
AND p.name IN ('BOARD_READ', 'BOARD_WRITE', 'BOARD_DELETE', 'OPENSTACK_READ', 'OPENSTACK_WRITE');

-- 기본 사용자 삽입 (비밀번호는 해시된 값)
-- admin123 -> SHA-256 + Salt 해시값 (실제 구현에서는 SimplePasswordEncoder 사용)
INSERT INTO users (username, password, email, name, enabled) VALUES
('admin', 'hashed_admin_password_placeholder', 'admin@example.com', '관리자', true),
('user', 'hashed_user_password_placeholder', 'user@example.com', '사용자', true);

-- 사용자-역할 매핑
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.username = 'admin' AND r.name = 'ADMIN';

INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.username = 'user' AND r.name = 'USER';

-- 샘플 게시글 데이터
INSERT INTO board (title, content, writer, view_count) VALUES
('환영합니다!', '게시판 시스템에 오신 것을 환영합니다.', 'admin', 0),
('OpenStack 연동 테스트', 'OpenStack VM 생성 기능을 테스트해보세요.', 'admin', 0),
('사용자 가이드', '시스템 사용법에 대한 안내입니다.', 'user', 0); 