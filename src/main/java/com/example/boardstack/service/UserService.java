package com.example.boardstack.service;

import com.example.boardstack.entity.User;
import com.example.boardstack.entity.Role;
import com.example.boardstack.entity.Permission;
import com.example.boardstack.repository.UserRepository;
import com.example.boardstack.repository.RoleRepository;
import com.example.boardstack.repository.PermissionRepository;
import com.example.boardstack.security.SimplePasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final SimplePasswordEncoder passwordEncoder;

    public UserService(SimplePasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void initializeDefaultUsers() {
        // 기본 사용자가 없는 경우에만 초기화
        if (userRepository.count() == 0) {
            createDefaultUsersAndRoles();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createDefaultUsersAndRoles() {
        // 관리자 사용자 생성
        User admin = new User("admin", passwordEncoder.encode("admin123"), "admin@example.com", "관리자");
        admin.setEnabled(true);
        
        // 일반 사용자 생성
        User user = new User("user", passwordEncoder.encode("user123"), "user@example.com", "사용자");
        user.setEnabled(true);
        
        // 역할 할당
        Optional<Role> adminRole = roleRepository.findByName("ADMIN");
        Optional<Role> userRole = roleRepository.findByName("USER");
        
        if (adminRole.isPresent()) {
            admin.setRoles(Set.of(adminRole.get()));
        }
        if (userRole.isPresent()) {
            user.setRoles(Set.of(userRole.get()));
        }
        
        userRepository.save(admin);
        userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = false, isolation = Isolation.READ_COMMITTED)
    public User createUser(String username, String password, String email, String name) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        User user = new User(username, passwordEncoder.encode(password), email, name);
        user.setEnabled(true);
        
        // 먼저 사용자를 저장
        user = userRepository.saveAndFlush(user);
        
        // 기본 USER 역할 할당
        Optional<Role> userRole = roleRepository.findByName("USER");
        if (userRole.isPresent()) {
            // JPA 방식으로 역할 추가
            user.getRoles().add(userRole.get());
            
            // 명시적으로 사용자 업데이트
            user = userRepository.saveAndFlush(user);
            
            // 엔티티 매니저 클리어 및 새로 조회
            entityManager.clear();
        }
        
        // 사용자를 다시 조회하여 최신 상태 반환 (roles 포함)
        return userRepository.findByUsername(username).orElse(user);
    }

    @Transactional(readOnly = false)
    public User updateUser(Long userId, String email, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.setEmail(email);
        user.setName(name);
        user.updateTimestamp();
        
        return userRepository.save(user);
    }

    @Transactional(readOnly = false)
    public void updateLastLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        
        user.updateLastLogin();
        userRepository.save(user);
    }

    @Transactional(readOnly = false)
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid old password");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.updateTimestamp();
        userRepository.save(user);
    }

    public boolean hasPermission(String username, String permissionName) {
        Optional<User> userOpt = userRepository.findByUsernameWithRolesAndPermissions(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    public boolean hasRole(String username, String roleName) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        return userOpt.get().hasRole(roleName);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = false)
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        userRepository.deleteById(userId);
    }
} 