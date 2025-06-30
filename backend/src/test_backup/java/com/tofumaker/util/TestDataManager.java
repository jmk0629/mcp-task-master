package com.tofumaker.util;

import com.tofumaker.entity.Board;
import com.tofumaker.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class TestDataManager {

    @Autowired
    private BoardRepository boardRepository;

    private List<Long> createdBoardIds = new ArrayList<>();

    /**
     * 테스트용 게시글 데이터 생성
     */
    public Board createTestBoard(String title, String content, String author) {
        Board board = new Board(title, content, author);
        Board savedBoard = boardRepository.save(board);
        createdBoardIds.add(savedBoard.getId());
        return savedBoard;
    }

    /**
     * 기본 테스트 게시글 생성
     */
    public Board createDefaultTestBoard() {
        return createTestBoard("테스트 제목", "테스트 내용", "테스트 작성자");
    }

    /**
     * 여러 개의 테스트 게시글 생성
     */
    public List<Board> createMultipleTestBoards(int count) {
        List<Board> boards = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Board board = createTestBoard(
                    "테스트 제목 " + i,
                    "테스트 내용 " + i,
                    "테스트 작성자 " + i
            );
            boards.add(board);
        }
        return boards;
    }

    /**
     * 검색 테스트용 게시글 생성
     */
    public List<Board> createSearchTestData() {
        List<Board> boards = new ArrayList<>();
        
        boards.add(createTestBoard("Java 프로그래밍 기초", "Java 언어 학습 내용", "개발자A"));
        boards.add(createTestBoard("Python 데이터 분석", "Python을 이용한 데이터 분석", "개발자B"));
        boards.add(createTestBoard("Spring Boot 튜토리얼", "Spring Boot 프레임워크 가이드", "개발자A"));
        boards.add(createTestBoard("React 컴포넌트", "React 컴포넌트 개발 방법", "개발자C"));
        boards.add(createTestBoard("데이터베이스 설계", "효율적인 데이터베이스 설계 방법", "개발자B"));
        
        return boards;
    }

    /**
     * 시간 순서가 있는 테스트 데이터 생성
     */
    public List<Board> createTimeOrderedTestData() throws InterruptedException {
        List<Board> boards = new ArrayList<>();
        
        Board oldBoard = createTestBoard("오래된 게시글", "오래된 내용", "작성자1");
        boards.add(oldBoard);
        
        Thread.sleep(1000); // 시간 차이를 위한 대기
        
        Board recentBoard = createTestBoard("최신 게시글", "최신 내용", "작성자2");
        boards.add(recentBoard);
        
        return boards;
    }

    /**
     * 특정 작성자의 게시글들 생성
     */
    public List<Board> createBoardsByAuthor(String author, int count) {
        List<Board> boards = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Board board = createTestBoard(
                    author + "의 게시글 " + i,
                    author + "가 작성한 내용 " + i,
                    author
            );
            boards.add(board);
        }
        return boards;
    }

    /**
     * 특정 키워드가 포함된 게시글들 생성
     */
    public List<Board> createBoardsWithKeyword(String keyword, int count) {
        List<Board> boards = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Board board = createTestBoard(
                    keyword + " 관련 제목 " + i,
                    keyword + "에 대한 상세한 내용입니다. " + i,
                    "작성자" + i
            );
            boards.add(board);
        }
        return boards;
    }

    /**
     * 테스트 중에 생성된 모든 데이터 정리
     */
    public void cleanupTestData() {
        if (!createdBoardIds.isEmpty()) {
            boardRepository.deleteAllById(createdBoardIds);
            createdBoardIds.clear();
        }
    }

    /**
     * 모든 게시글 데이터 정리
     */
    public void cleanupAllBoardData() {
        boardRepository.deleteAll();
        createdBoardIds.clear();
    }

    /**
     * 특정 게시글 삭제
     */
    public void deleteBoard(Long boardId) {
        boardRepository.deleteById(boardId);
        createdBoardIds.remove(boardId);
    }

    /**
     * 생성된 게시글 ID 목록 반환
     */
    public List<Long> getCreatedBoardIds() {
        return new ArrayList<>(createdBoardIds);
    }

    /**
     * 게시글 존재 여부 확인
     */
    public boolean boardExists(Long boardId) {
        return boardRepository.existsById(boardId);
    }

    /**
     * 게시글 수 반환
     */
    public long getBoardCount() {
        return boardRepository.count();
    }

    /**
     * 특정 작성자의 게시글 수 반환
     */
    public long getBoardCountByAuthor(String author) {
        return boardRepository.findByAuthorContainingIgnoreCase(author).size();
    }

    /**
     * 제목에 특정 키워드가 포함된 게시글 수 반환
     */
    public long getBoardCountByTitleKeyword(String keyword) {
        return boardRepository.findByTitleContainingIgnoreCase(keyword).size();
    }
} 