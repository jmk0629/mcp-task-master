package com.example.boardstack.integration;

import com.example.boardstack.entity.User;
import com.example.boardstack.entity.Role;
import com.example.boardstack.entity.Permission;
import com.example.boardstack.entity.Board;
import com.example.boardstack.repository.UserRepository;
import com.example.boardstack.repository.RoleRepository;
import com.example.boardstack.repository.PermissionRepository;
import com.example.boardstack.repository.BoardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@Import(DatabaseIntegrationTest.TestJpaConfig.class)
public class DatabaseIntegrationTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class TestJpaConfig {
    }

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private BoardRepository boardRepository;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();
        boardRepository.deleteAll();
    }

    @Test
    void testPermissionCRUD() {
        // Given
        Permission permission = new Permission("TEST_READ", "테스트 읽기", "TEST", "READ");
        
        // When
        Permission savedPermission = permissionRepository.save(permission);
        
        // Then
        assertThat(savedPermission.getId()).isNotNull();
        assertThat(savedPermission.getName()).isEqualTo("TEST_READ");
        assertThat(savedPermission.getResource()).isEqualTo("TEST");
        assertThat(savedPermission.getAction()).isEqualTo("READ");
        
        // 조회 테스트
        Optional<Permission> foundPermission = permissionRepository.findByName("TEST_READ");
        assertThat(foundPermission).isPresent();
        assertThat(foundPermission.get().getDescription()).isEqualTo("테스트 읽기");
    }

    @Test
    void testRoleCRUD() {
        // Given
        Permission permission1 = new Permission("ROLE_READ", "역할 읽기", "ROLE", "READ");
        Permission permission2 = new Permission("ROLE_WRITE", "역할 쓰기", "ROLE", "WRITE");
        permissionRepository.save(permission1);
        permissionRepository.save(permission2);
        
        Role role = new Role("TEST_ROLE", "테스트 역할");
        role.setPermissions(Set.of(permission1, permission2));
        
        // When
        Role savedRole = roleRepository.save(role);
        
        // Then
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getName()).isEqualTo("TEST_ROLE");
        assertThat(savedRole.getPermissions()).hasSize(2);
        
        // 권한과 함께 조회 테스트
        Optional<Role> foundRole = roleRepository.findByNameWithPermissions("TEST_ROLE");
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getPermissions()).hasSize(2);
    }

    @Test
    void testUserCRUD() {
        // Given
        Permission permission = new Permission("USER_READ", "사용자 읽기", "USER", "READ");
        permissionRepository.save(permission);
        
        Role role = new Role("USER_ROLE", "사용자 역할");
        role.setPermissions(Set.of(permission));
        roleRepository.save(role);
        
        User user = new User("testuser", "password123", "test@example.com", "테스트 사용자");
        user.setRoles(Set.of(role));
        user.setEnabled(true);
        
        // When
        User savedUser = userRepository.save(user);
        
        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.getRoles()).hasSize(1);
        
        // 사용자명으로 조회 테스트
        Optional<User> foundUser = userRepository.findByUsername("testuser");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("테스트 사용자");
        
        // 역할과 권한과 함께 조회 테스트
        Optional<User> userWithRoles = userRepository.findByUsernameWithRolesAndPermissions("testuser");
        assertThat(userWithRoles).isPresent();
        assertThat(userWithRoles.get().getRoles()).hasSize(1);
        assertThat(userWithRoles.get().getRoles().iterator().next().getPermissions()).hasSize(1);
    }

    @Test
    void testBoardCRUD() {
        // Given
        Board board = Board.builder()
                .title("테스트 게시글")
                .content("테스트 내용입니다.")
                .writer("testwriter")
                .viewCount(0L)
                .build();
        
        // When
        Board savedBoard = boardRepository.save(board);
        
        // Then
        assertThat(savedBoard.getId()).isNotNull();
        assertThat(savedBoard.getTitle()).isEqualTo("테스트 게시글");
        assertThat(savedBoard.getContent()).isEqualTo("테스트 내용입니다.");
        assertThat(savedBoard.getWriter()).isEqualTo("testwriter");
        assertThat(savedBoard.getViewCount()).isEqualTo(0L);
        assertThat(savedBoard.getCreatedAt()).isNotNull();
        assertThat(savedBoard.getUpdatedAt()).isNotNull();
        
        // 조회수 증가 테스트
        savedBoard.increaseViewCount();
        Board updatedBoard = boardRepository.save(savedBoard);
        assertThat(updatedBoard.getViewCount()).isEqualTo(1L);
        
        // 게시글 수정 테스트
        savedBoard.update("수정된 제목", "수정된 내용");
        Board modifiedBoard = boardRepository.save(savedBoard);
        assertThat(modifiedBoard.getTitle()).isEqualTo("수정된 제목");
        assertThat(modifiedBoard.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    void testUserRolePermissionRelationship() {
        // Given
        Permission readPermission = new Permission("READ_PERM", "읽기 권한", "BOARD", "READ");
        Permission writePermission = new Permission("WRITE_PERM", "쓰기 권한", "BOARD", "WRITE");
        permissionRepository.save(readPermission);
        permissionRepository.save(writePermission);
        
        Role userRole = new Role("USER", "일반 사용자");
        userRole.setPermissions(Set.of(readPermission, writePermission));
        roleRepository.save(userRole);
        
        Role adminRole = new Role("ADMIN", "관리자");
        adminRole.setPermissions(Set.of(readPermission, writePermission));
        roleRepository.save(adminRole);
        
        User user = new User("testuser", "password", "user@test.com", "테스트 사용자");
        user.setRoles(Set.of(userRole, adminRole));
        user.setEnabled(true);
        
        // When
        User savedUser = userRepository.save(user);
        
        // Then
        Optional<User> foundUser = userRepository.findByUsernameWithRolesAndPermissions("testuser");
        assertThat(foundUser).isPresent();
        
        User retrievedUser = foundUser.get();
        assertThat(retrievedUser.getRoles()).hasSize(2);
        
        // 역할 확인
        assertThat(retrievedUser.hasRole("USER")).isTrue();
        assertThat(retrievedUser.hasRole("ADMIN")).isTrue();
        assertThat(retrievedUser.hasRole("GUEST")).isFalse();
        
        // 권한 확인 (모든 역할의 권한을 합쳐서 확인)
        boolean hasReadPermission = retrievedUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission -> permission.getName().equals("READ_PERM"));
        
        boolean hasWritePermission = retrievedUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission -> permission.getName().equals("WRITE_PERM"));
        
        assertThat(hasReadPermission).isTrue();
        assertThat(hasWritePermission).isTrue();
    }
} 