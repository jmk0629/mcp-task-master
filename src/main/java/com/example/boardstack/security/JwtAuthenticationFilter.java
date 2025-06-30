package com.example.boardstack.security;

import com.example.boardstack.service.UserService;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // CORS 헤더 설정
        httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");

        // OPTIONS 요청 처리
        if ("OPTIONS".equals(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String requestURI = httpRequest.getRequestURI();

        // 인증이 필요하지 않은 경로
        if (isPublicPath(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // Authorization 헤더 확인
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(httpResponse, "인증 토큰이 필요합니다");
            return;
        }

        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            
            // 토큰 유효성 검증
            if (token == null || !jwtUtil.canTokenBeParsed(token)) {
                sendUnauthorizedResponse(httpResponse, "유효하지 않은 토큰입니다");
                return;
            }

            String username = jwtUtil.getUsernameFromToken(token);
            
            // 사용자 존재 여부 확인
            if (!userService.findByUsername(username).isPresent()) {
                sendUnauthorizedResponse(httpResponse, "사용자를 찾을 수 없습니다");
                return;
            }

            // 토큰 유효성 재검증
            if (!jwtUtil.validateToken(token, username)) {
                sendUnauthorizedResponse(httpResponse, "만료되거나 유효하지 않은 토큰입니다");
                return;
            }

            // 요청에 사용자 정보 추가
            httpRequest.setAttribute("username", username);
            httpRequest.setAttribute("token", token);

            chain.doFilter(request, response);

        } catch (Exception e) {
            sendUnauthorizedResponse(httpResponse, "토큰 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private boolean isPublicPath(String requestURI) {
        return requestURI.startsWith("/api/auth/") ||
               requestURI.startsWith("/api/boards/") || // 임시로 허용
               requestURI.startsWith("/api/openstack/") || // 임시로 허용
               requestURI.startsWith("/api/integrated/") || // 임시로 허용
               requestURI.equals("/") ||
               requestURI.startsWith("/static/") ||
               requestURI.startsWith("/public/");
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\": \"Unauthorized\", \"message\": \"%s\", \"timestamp\": %d}",
            message, System.currentTimeMillis()
        );
        
        response.getWriter().write(jsonResponse);
    }
} 