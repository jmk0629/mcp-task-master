package com.example.boardstack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;

/**
 * 트랜잭션 관리 설정
 * JPA 트랜잭션 매니저 구성 및 트랜잭션 관리 활성화
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * JPA 트랜잭션 매니저 설정
     * @param entityManagerFactory JPA EntityManagerFactory
     * @return 설정된 JpaTransactionManager
     */
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        
        // 트랜잭션 타임아웃 설정 (30초)
        transactionManager.setDefaultTimeout(30);
        
        // 커밋 실패 시 롤백 활성화
        transactionManager.setRollbackOnCommitFailure(true);
        
        return transactionManager;
    }
} 