package com.tofumaker.repository;

import com.tofumaker.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    
    // 제목으로 검색 (페이징)
    Page<Board> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    // 제목으로 검색 (리스트)
    List<Board> findByTitleContainingIgnoreCase(String title);
    
    // 작성자로 검색 (페이징)
    Page<Board> findByAuthorContainingIgnoreCase(String author, Pageable pageable);
    
    // 작성자로 검색 (리스트)
    List<Board> findByAuthorContainingIgnoreCase(String author);
    
    // 제목 또는 내용으로 검색 (페이징)
    @Query("SELECT b FROM Board b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Board> findByTitleOrContentContaining(@Param("keyword") String keyword, Pageable pageable);
    
    // 제목 또는 내용으로 검색 (리스트)
    @Query("SELECT b FROM Board b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Board> findByTitleOrContentContaining(@Param("keyword") String keyword);
    
    // 최신 게시글 조회 (페이징)
    Page<Board> findByOrderByCreatedAtDesc(Pageable pageable);
    
    // 최신 게시글 조회 (리스트)
    List<Board> findTop10ByOrderByCreatedAtDesc();
    
    // 활성 게시글 조회 (페이징)
    Page<Board> findByActiveTrue(Pageable pageable);
    
    // 활성 게시글 조회 (리스트)
    List<Board> findByActiveTrue();
    
    // 조회수 기준 정렬 (페이징)
    Page<Board> findByActiveTrueOrderByViewCountDesc(Pageable pageable);
    
    // 인기 게시글 조회 (리스트)
    List<Board> findTop10ByActiveTrueOrderByViewCountDesc();
} 