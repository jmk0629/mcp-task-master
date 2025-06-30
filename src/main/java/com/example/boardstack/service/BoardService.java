package com.example.boardstack.service;

import com.example.boardstack.dto.BoardRequestDto;
import com.example.boardstack.dto.BoardResponseDto;
import com.example.boardstack.entity.Board;
import com.example.boardstack.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;

    /**
     * 게시글 등록
     */
    @Transactional
    public BoardResponseDto createBoard(BoardRequestDto requestDto) {
        log.info("게시글 등록 요청: {}", requestDto.getTitle());
        
        Board board = requestDto.toEntity();
        Board savedBoard = boardRepository.save(board);
        
        log.info("게시글 등록 완료: ID={}", savedBoard.getId());
        return BoardResponseDto.from(savedBoard);
    }

    /**
     * 전체 게시글 목록 조회
     */
    public List<BoardResponseDto> getAllBoards() {
        log.info("전체 게시글 목록 조회");
        
        return boardRepository.findAll()
                .stream()
                .map(BoardResponseDto::fromForList)
                .collect(Collectors.toList());
    }

    /**
     * 게시글 상세 조회 (조회수 증가)
     */
    @Transactional
    public BoardResponseDto getBoardById(Long id) {
        log.info("게시글 상세 조회: ID={}", id);
        
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + id));
        
        // 조회수 증가
        board.increaseViewCount();
        
        return BoardResponseDto.from(board);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public BoardResponseDto updateBoard(Long id, BoardRequestDto requestDto) {
        log.info("게시글 수정 요청: ID={}", id);
        
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + id));
        
        // 게시글 수정
        board.update(requestDto.getTitle(), requestDto.getContent());
        
        log.info("게시글 수정 완료: ID={}", id);
        return BoardResponseDto.from(board);
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deleteBoard(Long id) {
        log.info("게시글 삭제 요청: ID={}", id);
        
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + id));
        
        boardRepository.delete(board);
        log.info("게시글 삭제 완료: ID={}", id);
    }

    /**
     * 제목으로 게시글 검색
     */
    public List<BoardResponseDto> searchByTitle(String title) {
        log.info("제목으로 게시글 검색: {}", title);
        
        return boardRepository.findByTitleContaining(title)
                .stream()
                .map(BoardResponseDto::fromForList)
                .collect(Collectors.toList());
    }

    /**
     * 작성자로 게시글 검색
     */
    public List<BoardResponseDto> searchByWriter(String writer) {
        log.info("작성자로 게시글 검색: {}", writer);
        
        return boardRepository.findByWriter(writer)
                .stream()
                .map(BoardResponseDto::fromForList)
                .collect(Collectors.toList());
    }

    /**
     * 키워드로 게시글 검색 (제목 또는 내용)
     */
    public List<BoardResponseDto> searchByKeyword(String keyword) {
        log.info("키워드로 게시글 검색: {}", keyword);
        
        return boardRepository.findByTitleOrContentContaining(keyword)
                .stream()
                .map(BoardResponseDto::fromForList)
                .collect(Collectors.toList());
    }

    /**
     * 인기 게시글 조회 (조회수 기준)
     */
    public List<BoardResponseDto> getPopularBoards() {
        log.info("인기 게시글 조회");
        
        return boardRepository.findTop10ByOrderByViewCountDesc()
                .stream()
                .map(BoardResponseDto::fromForList)
                .collect(Collectors.toList());
    }

    /**
     * 최신 게시글 조회
     */
    public List<BoardResponseDto> getRecentBoards() {
        log.info("최신 게시글 조회");
        
        return boardRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(BoardResponseDto::fromForList)
                .collect(Collectors.toList());
    }
} 