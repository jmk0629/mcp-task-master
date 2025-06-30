package com.tofumaker.service;

import com.tofumaker.config.CacheConfig;
import com.tofumaker.entity.Board;
import com.tofumaker.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BoardService {
    
    @Autowired
    private BoardRepository boardRepository;
    
    // 모든 게시글 조회 (페이징)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'all_boards_page_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<Board> getAllBoards(Pageable pageable) {
        return boardRepository.findAll(pageable);
    }

    // 모든 게시글 조회 (리스트)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'all_boards'")
    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }
    
    // ID로 게시글 조회
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'board_' + #id")
    public Board getBoardById(Long id) {
        Optional<Board> board = boardRepository.findById(id);
        return board.orElse(null);
    }
    
    // 게시글 생성
    @CacheEvict(value = CacheConfig.CacheNames.API_RESPONSES, allEntries = true)
    public Board createBoard(Board board) {
        return boardRepository.save(board);
    }
    
    // 게시글 수정
    @CachePut(value = CacheConfig.CacheNames.API_RESPONSES, key = "'board_' + #id")
    @CacheEvict(value = CacheConfig.CacheNames.API_RESPONSES, key = "'all_boards'")
    public Board updateBoard(Long id, Board boardDetails) {
        Optional<Board> optionalBoard = boardRepository.findById(id);
        if (optionalBoard.isPresent()) {
            Board board = optionalBoard.get();
            board.setTitle(boardDetails.getTitle());
            board.setContent(boardDetails.getContent());
            board.setAuthor(boardDetails.getAuthor());
            return boardRepository.save(board);
        }
        return null;
    }
    
    // 게시글 삭제
    @CacheEvict(value = CacheConfig.CacheNames.API_RESPONSES, allEntries = true)
    public boolean deleteBoard(Long id) {
        if (boardRepository.existsById(id)) {
            boardRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    // 제목으로 검색 (페이징)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'search_title_page_' + #title + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<Board> searchByTitle(String title, Pageable pageable) {
        return boardRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    // 제목으로 검색 (리스트)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'search_title_' + #title")
    public List<Board> searchByTitle(String title) {
        return boardRepository.findByTitleContainingIgnoreCase(title);
    }
    
    // 작성자로 검색 (페이징)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'search_author_page_' + #author + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<Board> searchByAuthor(String author, Pageable pageable) {
        return boardRepository.findByAuthorContainingIgnoreCase(author, pageable);
    }

    // 작성자로 검색 (리스트)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'search_author_' + #author")
    public List<Board> searchByAuthor(String author) {
        return boardRepository.findByAuthorContainingIgnoreCase(author);
    }
    
    // 키워드로 검색 (제목 또는 내용) - 페이징
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'search_keyword_page_' + #keyword + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<Board> searchByKeyword(String keyword, Pageable pageable) {
        return boardRepository.findByTitleOrContentContaining(keyword, pageable);
    }

    // 키워드로 검색 (제목 또는 내용) - 리스트
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'search_keyword_' + #keyword")
    public List<Board> searchByKeyword(String keyword) {
        return boardRepository.findByTitleOrContentContaining(keyword);
    }
    
    // 최신 게시글 조회 (페이징)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'recent_boards_page_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<Board> getRecentBoards(Pageable pageable) {
        return boardRepository.findByOrderByCreatedAtDesc(pageable);
    }

    // 최신 게시글 조회 (리스트)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'recent_boards'")
    public List<Board> getRecentBoards() {
        return boardRepository.findTop10ByOrderByCreatedAtDesc();
    }
    
    // 활성 게시글 조회 (페이징)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'active_boards_page_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<Board> getActiveBoards(Pageable pageable) {
        return boardRepository.findByActiveTrue(pageable);
    }

    // 활성 게시글 조회 (리스트)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'active_boards'")
    public List<Board> getActiveBoards() {
        return boardRepository.findByActiveTrue();
    }
    
    // 인기 게시글 조회 (페이징)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'popular_boards_page_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<Board> getPopularBoards(Pageable pageable) {
        return boardRepository.findByActiveTrueOrderByViewCountDesc(pageable);
    }

    // 인기 게시글 조회 (리스트)
    @Cacheable(value = CacheConfig.CacheNames.API_RESPONSES, key = "'popular_boards'")
    public List<Board> getPopularBoards() {
        return boardRepository.findTop10ByActiveTrueOrderByViewCountDesc();
    }
} 