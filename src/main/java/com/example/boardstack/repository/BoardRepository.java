package com.example.boardstack.repository;

import com.example.boardstack.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 제목으로 검색
    List<Board> findByTitleContaining(String title);

    // 작성자로 검색
    List<Board> findByWriter(String writer);

    // 제목 또는 내용으로 검색
    @Query("SELECT b FROM Board b WHERE b.title LIKE %:keyword% OR b.content LIKE %:keyword%")
    List<Board> findByTitleOrContentContaining(@Param("keyword") String keyword);

    // 조회수 기준 상위 게시글 조회
    List<Board> findTop10ByOrderByViewCountDesc();

    // 최신 게시글 조회
    List<Board> findTop10ByOrderByCreatedAtDesc();
} 