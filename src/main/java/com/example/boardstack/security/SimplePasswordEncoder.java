package com.example.boardstack.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SimplePasswordEncoder {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    public String encode(String rawPassword) {
        try {
            // 솔트 생성
            byte[] salt = generateSalt();
            
            // 패스워드와 솔트를 결합하여 해시
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(rawPassword.getBytes());
            
            // 솔트와 해시를 Base64로 인코딩하여 결합
            String saltString = Base64.getEncoder().encodeToString(salt);
            String hashString = Base64.getEncoder().encodeToString(hashedPassword);
            
            return saltString + ":" + hashString;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("패스워드 인코딩 실패", e);
        }
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        try {
            // 인코딩된 패스워드에서 솔트와 해시 분리
            String[] parts = encodedPassword.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String storedHash = parts[1];
            
            // 입력된 패스워드를 같은 솔트로 해시
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(rawPassword.getBytes());
            String hashString = Base64.getEncoder().encodeToString(hashedPassword);
            
            return storedHash.equals(hashString);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
} 