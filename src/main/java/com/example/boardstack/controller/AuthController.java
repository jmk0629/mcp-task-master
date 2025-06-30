package com.example.boardstack.controller;

import com.example.boardstack.dto.LoginRequest;
import com.example.boardstack.dto.JwtResponse;
import com.example.boardstack.dto.ErrorResponse;
import com.example.boardstack.entity.User;
import com.example.boardstack.service.UserService;
import com.example.boardstack.security.JwtUtil;
import com.example.boardstack.security.SimplePasswordEncoder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@Tag(name = "인증 API", description = "사용자 로그인, 토큰 갱신, 사용자 정보 조회 기능을 제공하는 API")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SimplePasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 사용자 조회
            Optional<User> userOpt = userService.findByUsername(loginRequest.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("사용자를 찾을 수 없습니다"));
            }

            User user = userOpt.get();

            // 비밀번호 검증
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("비밀번호가 일치하지 않습니다"));
            }

            // 사용자 활성화 상태 확인
            if (!user.isEnabled()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("비활성화된 계정입니다"));
            }

            // 역할 정보 추출
            Set<String> userRoles = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());
            
            String roles = String.join(",", userRoles);

            // JWT 토큰 생성
            String accessToken = jwtUtil.generateTokenWithRoles(user.getUsername(), roles);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            // 로그인 시간 업데이트
            userService.updateLastLogin(user.getUsername());

            // 응답 생성
            JwtResponse response = new JwtResponse(
                    accessToken,
                    refreshToken,
                    86400000L, // 24시간
                    user.getUsername(),
                    user.getName(),
                    user.getEmail(),
                    userRoles
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("로그인 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("유효하지 않은 토큰 형식입니다"));
            }

            String refreshToken = authHeader.substring(7);

            // 리프레시 토큰 파싱 가능 여부 확인
            if (!jwtUtil.canTokenBeParsed(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("유효하지 않은 리프레시 토큰입니다"));
            }

            String username = jwtUtil.getUsernameFromToken(refreshToken);
            Optional<User> userOpt = userService.findByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("사용자를 찾을 수 없습니다"));
            }

            User user = userOpt.get();
            
            // 토큰 유효성 검증
            if (!jwtUtil.validateToken(refreshToken, username)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("만료된 리프레시 토큰입니다"));
            }
            
            // 역할 정보 추출
            Set<String> userRoles = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());
            
            String roles = String.join(",", userRoles);

            // 새로운 액세스 토큰 생성
            String newAccessToken = jwtUtil.generateTokenWithRoles(username, roles);

            JwtResponse response = new JwtResponse(
                    newAccessToken,
                    refreshToken,
                    86400000L,
                    user.getUsername(),
                    user.getName(),
                    user.getEmail(),
                    userRoles
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("토큰 갱신 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("유효하지 않은 토큰 형식입니다"));
            }

            String token = authHeader.substring(7);
            
            // 토큰 파싱 가능 여부 확인
            if (!jwtUtil.canTokenBeParsed(token)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("유효하지 않은 토큰입니다"));
            }

            String username = jwtUtil.getUsernameFromToken(token);
            
            // 토큰 유효성 검증
            if (!jwtUtil.validateToken(token, username)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("만료된 토큰입니다"));
            }

            Optional<User> userOpt = userService.findByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("사용자를 찾을 수 없습니다"));
            }

            User user = userOpt.get();
            
            // 역할과 권한 정보 추출
            Set<String> userRoles = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());
                    
            Set<String> userPermissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(permission -> permission.getName())
                    .collect(Collectors.toSet());

            // 사용자 정보 응답
            return ResponseEntity.ok(new UserInfoResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getEmail(),
                    userRoles,
                    userPermissions,
                    user.isEnabled(),
                    user.getLastLoginAt()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 사용자 정보 응답 DTO
    public static class UserInfoResponse {
        private Long id;
        private String username;
        private String name;
        private String email;
        private Set<String> roles;
        private Set<String> permissions;
        private boolean enabled;
        private java.time.LocalDateTime lastLoginAt;

        public UserInfoResponse(Long id, String username, String name, String email, 
                              Set<String> roles, Set<String> permissions, boolean enabled, 
                              java.time.LocalDateTime lastLoginAt) {
            this.id = id;
            this.username = username;
            this.name = name;
            this.email = email;
            this.roles = roles;
            this.permissions = permissions;
            this.enabled = enabled;
            this.lastLoginAt = lastLoginAt;
        }

        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public Set<String> getRoles() { return roles; }
        public Set<String> getPermissions() { return permissions; }
        public boolean isEnabled() { return enabled; }
        public java.time.LocalDateTime getLastLoginAt() { return lastLoginAt; }
    }
} 