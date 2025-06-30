package com.example.boardstack.integration;

import com.example.boardstack.dto.BoardRequestDto;
import com.example.boardstack.dto.BoardResponseDto;
import com.example.boardstack.entity.User;
import com.example.boardstack.service.BoardService;
import com.example.boardstack.service.UserService;
import com.example.boardstack.repository.UserRepository;
import com.example.boardstack.repository.RoleRepository;
import com.example.boardstack.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ServiceIntegrationTest {

    @Autowired
    private BoardService boardService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();
    }

    @Test
    void testBoardServiceCRUD() {
        // Given
        BoardRequestDto requestDto = new BoardRequestDto();
        requestDto.setTitle("통합 테스트 게시글");
        requestDto.setContent("통합 테스트 내용입니다.");
        requestDto.setWriter("testwriter");
        
        // When - 게시글 생성
        BoardResponseDto createdBoard = boardService.createBoard(requestDto);
        
        // Then
        assertThat(createdBoard.getId()).isNotNull();
        assertThat(createdBoard.getTitle()).isEqualTo("통합 테스트 게시글");
        assertThat(createdBoard.getContent()).isEqualTo("통합 테스트 내용입니다.");
        assertThat(createdBoard.getWriter()).isEqualTo("testwriter");
        assertThat(createdBoard.getViewCount()).isEqualTo(0L);
        
        // When - 전체 게시글 조회
        List<BoardResponseDto> allBoards = boardService.getAllBoards();
        
        // Then
        assertThat(allBoards).hasSize(1);
        assertThat(allBoards.get(0).getTitle()).isEqualTo("통합 테스트 게시글");
        
        // When - 게시글 상세 조회 (조회수 증가)
        BoardResponseDto detailBoard = boardService.getBoardById(createdBoard.getId());
        
        // Then
        assertThat(detailBoard.getViewCount()).isEqualTo(1L);
        
        // When - 게시글 수정
        BoardRequestDto updateDto = new BoardRequestDto();
        updateDto.setTitle("수정된 제목");
        updateDto.setContent("수정된 내용");
        updateDto.setWriter("testwriter");
        
        BoardResponseDto updatedBoard = boardService.updateBoard(createdBoard.getId(), updateDto);
        
        // Then
        assertThat(updatedBoard.getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedBoard.getContent()).isEqualTo("수정된 내용");
        
        // When - 게시글 삭제
        boardService.deleteBoard(createdBoard.getId());
        
        // Then
        List<BoardResponseDto> boardsAfterDelete = boardService.getAllBoards();
        assertThat(boardsAfterDelete).isEmpty();
    }

    @Test
    void testUserServiceCRUD() {
        // When - 사용자 생성
        User createdUser = userService.createUser("testuser", "password123", "test@example.com", "테스트 사용자");
        
        // Then
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo("testuser");
        assertThat(createdUser.getEmail()).isEqualTo("test@example.com");
        assertThat(createdUser.getName()).isEqualTo("테스트 사용자");
        assertThat(createdUser.isEnabled()).isTrue();
        
        // When - 사용자명으로 조회
        Optional<User> foundUser = userService.findByUsername("testuser");
        
        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("테스트 사용자");
        
        // When - 이메일로 조회
        Optional<User> foundByEmail = userService.findByEmail("test@example.com");
        
        // Then
        assertThat(foundByEmail).isPresent();
        assertThat(foundByEmail.get().getUsername()).isEqualTo("testuser");
        
        // When - 사용자 정보 수정
        User updatedUser = userService.updateUser(createdUser.getId(), "updated@example.com", "수정된 이름");
        
        // Then
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getName()).isEqualTo("수정된 이름");
        
        // When - 마지막 로그인 시간 업데이트
        userService.updateLastLogin("testuser");
        
        // Then
        Optional<User> userAfterLogin = userService.findByUsername("testuser");
        assertThat(userAfterLogin).isPresent();
        assertThat(userAfterLogin.get().getLastLoginAt()).isNotNull();
        
        // When - 사용자 삭제
        userService.deleteUser(createdUser.getId());
        
        // Then
        Optional<User> deletedUser = userService.findByUsername("testuser");
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void testUserServiceValidation() {
        // Given
        userService.createUser("existinguser", "password", "existing@example.com", "기존 사용자");
        
        // When & Then - 중복 사용자명 검증
        assertThatThrownBy(() -> 
            userService.createUser("existinguser", "password", "new@example.com", "새 사용자")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Username already exists");
        
        // When & Then - 중복 이메일 검증
        assertThatThrownBy(() -> 
            userService.createUser("newuser", "password", "existing@example.com", "새 사용자")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Email already exists");
    }

    @Test
    @Rollback(false)
    public void testUserPermissionAndRole() {
        // 기존 사용자 삭제
        userRepository.deleteAll();
        
        // 새 사용자 생성
        User createdUser = userService.createUser("permissionuser", "password123", "perm@example.com", "권한 테스트 사용자");
        
        // 사용자가 생성되었는지 확인
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo("permissionuser");
        
        // 사용자를 다시 조회
        Optional<User> foundUser = userService.findByUsername("permissionuser");
        assertThat(foundUser).isPresent();
        
        // 역할 확인
        boolean hasUserRole = userService.hasRole("permissionuser", "USER");
        assertThat(hasUserRole).isTrue();
        
        // 권한 확인
        boolean hasReadPermission = userService.hasPermission("permissionuser", "READ_BOARD");
        assertThat(hasReadPermission).isTrue();
    }

    @Test
    void testTransactionRollback() {
        // Given
        String username = "rollbackuser";
        
        // When & Then - 존재하지 않는 사용자 업데이트 시 예외 발생
        assertThatThrownBy(() -> 
            userService.updateUser(999L, "new@example.com", "새 이름")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("User not found");
        
        // When & Then - 존재하지 않는 사용자 비밀번호 변경 시 예외 발생
        assertThatThrownBy(() -> 
            userService.changePassword(username, "oldpass", "newpass")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("User not found");
    }

    @Test
    void testPasswordChange() {
        // Given
        User user = userService.createUser("passworduser", "oldpassword", "pass@example.com", "비밀번호 테스트");
        
        // When - 비밀번호 변경
        userService.changePassword("passworduser", "oldpassword", "newpassword");
        
        // Then - 새 비밀번호로 검증 가능해야 함
        Optional<User> updatedUser = userService.findByUsername("passworduser");
        assertThat(updatedUser).isPresent();
        // 실제 비밀번호 검증은 SimplePasswordEncoder를 통해 이루어짐
        
        // When & Then - 잘못된 기존 비밀번호로 변경 시도 시 예외 발생
        assertThatThrownBy(() -> 
            userService.changePassword("passworduser", "wrongoldpassword", "anotherpassword")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Invalid old password");
    }
} 