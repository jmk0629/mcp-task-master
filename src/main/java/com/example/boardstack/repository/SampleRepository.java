package com.example.boardstack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.boardstack.entity.SampleEntity;

@Repository
public interface SampleRepository extends JpaRepository<SampleEntity, Long> {
    
    // 추가적인 쿼리 메서드들을 여기에 정의할 수 있습니다
    // 예: List<SampleEntity> findByName(String name);
} 