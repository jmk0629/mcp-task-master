package com.tofumaker.service;

import com.tofumaker.entity.Board;
import com.tofumaker.repository.BoardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private BoardService boardService;

    private Board testBoard;

    @BeforeEach
    void setUp() {
        testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setTitle("테스트 제목");
        testBoard.setContent("테스트 내용");
        testBoard.setAuthor("테스트 작성자");
        testBoard.setCreatedAt(LocalDateTime.now());
        testBoard.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAllBoards_ShouldReturnAllBoards() {
        // Given
        List<Board> expectedBoards = Arrays.asList(testBoard);
        when(boardRepository.findAll()).thenReturn(expectedBoards);

        // When
        List<Board> actualBoards = boardService.getAllBoards();

        // Then
        assertEquals(expectedBoards.size(), actualBoards.size());
        assertEquals(expectedBoards.get(0).getTitle(), actualBoards.get(0).getTitle());
        verify(boardRepository, times(1)).findAll();
    }

    @Test
    void getBoardById_WhenBoardExists_ShouldReturnBoard() {
        // Given
        Long boardId = 1L;
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(testBoard));

        // When
        Board actualBoard = boardService.getBoardById(boardId);

        // Then
        assertNotNull(actualBoard);
        assertEquals(testBoard.getId(), actualBoard.getId());
        assertEquals(testBoard.getTitle(), actualBoard.getTitle());
        verify(boardRepository, times(1)).findById(boardId);
    }

    @Test
    void getBoardById_WhenBoardNotExists_ShouldReturnNull() {
        // Given
        Long boardId = 999L;
        when(boardRepository.findById(boardId)).thenReturn(Optional.empty());

        // When
        Board actualBoard = boardService.getBoardById(boardId);

        // Then
        assertNull(actualBoard);
        verify(boardRepository, times(1)).findById(boardId);
    }

    @Test
    void createBoard_ShouldSaveAndReturnBoard() {
        // Given
        Board newBoard = new Board("새 제목", "새 내용", "새 작성자");
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

        // When
        Board createdBoard = boardService.createBoard(newBoard);

        // Then
        assertNotNull(createdBoard);
        assertEquals(testBoard.getId(), createdBoard.getId());
        verify(boardRepository, times(1)).save(newBoard);
    }

    @Test
    void updateBoard_WhenBoardExists_ShouldUpdateAndReturnBoard() {
        // Given
        Long boardId = 1L;
        Board updateDetails = new Board("수정된 제목", "수정된 내용", "수정된 작성자");
        
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(testBoard));
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

        // When
        Board updatedBoard = boardService.updateBoard(boardId, updateDetails);

        // Then
        assertNotNull(updatedBoard);
        verify(boardRepository, times(1)).findById(boardId);
        verify(boardRepository, times(1)).save(testBoard);
    }

    @Test
    void updateBoard_WhenBoardNotExists_ShouldReturnNull() {
        // Given
        Long boardId = 999L;
        Board updateDetails = new Board("수정된 제목", "수정된 내용", "수정된 작성자");
        
        when(boardRepository.findById(boardId)).thenReturn(Optional.empty());

        // When
        Board updatedBoard = boardService.updateBoard(boardId, updateDetails);

        // Then
        assertNull(updatedBoard);
        verify(boardRepository, times(1)).findById(boardId);
        verify(boardRepository, never()).save(any(Board.class));
    }

    @Test
    void deleteBoard_WhenBoardExists_ShouldReturnTrue() {
        // Given
        Long boardId = 1L;
        when(boardRepository.existsById(boardId)).thenReturn(true);

        // When
        boolean result = boardService.deleteBoard(boardId);

        // Then
        assertTrue(result);
        verify(boardRepository, times(1)).existsById(boardId);
        verify(boardRepository, times(1)).deleteById(boardId);
    }

    @Test
    void deleteBoard_WhenBoardNotExists_ShouldReturnFalse() {
        // Given
        Long boardId = 999L;
        when(boardRepository.existsById(boardId)).thenReturn(false);

        // When
        boolean result = boardService.deleteBoard(boardId);

        // Then
        assertFalse(result);
        verify(boardRepository, times(1)).existsById(boardId);
        verify(boardRepository, never()).deleteById(boardId);
    }

    @Test
    void searchByTitle_ShouldReturnMatchingBoards() {
        // Given
        String title = "테스트";
        List<Board> expectedBoards = Arrays.asList(testBoard);
        when(boardRepository.findByTitleContainingIgnoreCase(title)).thenReturn(expectedBoards);

        // When
        List<Board> actualBoards = boardService.searchByTitle(title);

        // Then
        assertEquals(expectedBoards.size(), actualBoards.size());
        assertEquals(expectedBoards.get(0).getTitle(), actualBoards.get(0).getTitle());
        verify(boardRepository, times(1)).findByTitleContainingIgnoreCase(title);
    }

    @Test
    void searchByAuthor_ShouldReturnMatchingBoards() {
        // Given
        String author = "테스트";
        List<Board> expectedBoards = Arrays.asList(testBoard);
        when(boardRepository.findByAuthorContainingIgnoreCase(author)).thenReturn(expectedBoards);

        // When
        List<Board> actualBoards = boardService.searchByAuthor(author);

        // Then
        assertEquals(expectedBoards.size(), actualBoards.size());
        assertEquals(expectedBoards.get(0).getAuthor(), actualBoards.get(0).getAuthor());
        verify(boardRepository, times(1)).findByAuthorContainingIgnoreCase(author);
    }

    @Test
    void searchByKeyword_ShouldReturnMatchingBoards() {
        // Given
        String keyword = "테스트";
        List<Board> expectedBoards = Arrays.asList(testBoard);
        when(boardRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(keyword, keyword))
                .thenReturn(expectedBoards);

        // When
        List<Board> actualBoards = boardService.searchByKeyword(keyword);

        // Then
        assertEquals(expectedBoards.size(), actualBoards.size());
        verify(boardRepository, times(1))
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(keyword, keyword);
    }

    @Test
    void getRecentBoards_ShouldReturnRecentBoards() {
        // Given
        List<Board> expectedBoards = Arrays.asList(testBoard);
        when(boardRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(expectedBoards);

        // When
        List<Board> actualBoards = boardService.getRecentBoards();

        // Then
        assertEquals(expectedBoards.size(), actualBoards.size());
        verify(boardRepository, times(1)).findTop10ByOrderByCreatedAtDesc();
    }
} 