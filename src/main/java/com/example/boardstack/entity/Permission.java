package com.example.boardstack.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "permissions")
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String name;
    
    @Column(length = 255)
    private String description;
    
    @Column(nullable = false, length = 50)
    private String resource;
    
    @Column(nullable = false, length = 50)
    private String action;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 기본 생성자
    public Permission() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 생성자
    public Permission(String name, String description, String resource, String action) {
        this();
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.action = action;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 비즈니스 메서드
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getFullPermission() {
        return resource + ":" + action;
    }

    // 권한 상수 정의
    public static final String BOARD_READ = "BOARD_READ";
    public static final String BOARD_WRITE = "BOARD_WRITE";
    public static final String BOARD_DELETE = "BOARD_DELETE";
    public static final String OPENSTACK_READ = "OPENSTACK_READ";
    public static final String OPENSTACK_WRITE = "OPENSTACK_WRITE";
    public static final String OPENSTACK_DELETE = "OPENSTACK_DELETE";
    public static final String ADMIN_ACCESS = "ADMIN_ACCESS";
} 