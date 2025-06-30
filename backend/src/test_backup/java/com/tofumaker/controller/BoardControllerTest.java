package com.tofumaker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tofumaker.config.TestSecurityConfig;
import com.tofumaker.entity.Board;
import com.tofumaker.service.BoardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BoardController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @Autowired
    private ObjectMapper objectMapper;

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
    void getAllBoards_ShouldReturnBoardsList() throws Exception {
        // Given
        List<Board> boards = Arrays.asList(testBoard);
        when(boardService.getAllBoards()).thenReturn(boards);

        // When & Then
        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("테스트 제목"))
                .andExpect(jsonPath("$[0].content").value("테스트 내용"))
                .andExpect(jsonPath("$[0].author").value("테스트 작성자"));

        verify(boardService, times(1)).getAllBoards();
    }

    @Test
    void getBoardById_WhenBoardExists_ShouldReturnBoard() throws Exception {
        // Given
        Long boardId = 1L;
        when(boardService.getBoardById(boardId)).thenReturn(testBoard);

        // When & Then
        mockMvc.perform(get("/api/boards/{id}", boardId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("테스트 제목"))
                .andExpect(jsonPath("$.content").value("테스트 내용"))
                .andExpect(jsonPath("$.author").value("테스트 작성자"));

        verify(boardService, times(1)).getBoardById(boardId);
    }

    @Test
    void getBoardById_WhenBoardNotExists_ShouldReturnNotFound() throws Exception {
        // Given
        Long boardId = 999L;
        when(boardService.getBoardById(boardId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/boards/{id}", boardId))
                .andExpect(status().isNotFound());

        verify(boardService, times(1)).getBoardById(boardId);
    }

    @Test
    void createBoard_ShouldCreateAndReturnBoard() throws Exception {
        // Given
        Board newBoard = new Board("새 제목", "새 내용", "새 작성자");
        when(boardService.createBoard(any(Board.class))).thenReturn(testBoard);

        // When & Then
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBoard)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("테스트 제목"));

        verify(boardService, times(1)).createBoard(any(Board.class));
    }

    @Test
    void updateBoard_WhenBoardExists_ShouldUpdateAndReturnBoard() throws Exception {
        // Given
        Long boardId = 1L;
        Board updateBoard = new Board("수정된 제목", "수정된 내용", "수정된 작성자");
        Board updatedBoard = new Board();
        updatedBoard.setId(boardId);
        updatedBoard.setTitle("수정된 제목");
        updatedBoard.setContent("수정된 내용");
        updatedBoard.setAuthor("수정된 작성자");

        when(boardService.updateBoard(eq(boardId), any(Board.class))).thenReturn(updatedBoard);

        // When & Then
        mockMvc.perform(put("/api/boards/{id}", boardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBoard)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(boardId))
                .andExpect(jsonPath("$.title").value("수정된 제목"));

        verify(boardService, times(1)).updateBoard(eq(boardId), any(Board.class));
    }

    @Test
    void updateBoard_WhenBoardNotExists_ShouldReturnNotFound() throws Exception {
        // Given
        Long boardId = 999L;
        Board updateBoard = new Board("수정된 제목", "수정된 내용", "수정된 작성자");
        when(boardService.updateBoard(eq(boardId), any(Board.class))).thenReturn(null);

        // When & Then
        mockMvc.perform(put("/api/boards/{id}", boardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBoard)))
                .andExpect(status().isNotFound());

        verify(boardService, times(1)).updateBoard(eq(boardId), any(Board.class));
    }

    @Test
    void deleteBoard_WhenBoardExists_ShouldReturnNoContent() throws Exception {
        // Given
        Long boardId = 1L;
        when(boardService.deleteBoard(boardId)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/boards/{id}", boardId))
                .andExpect(status().isNoContent());

        verify(boardService, times(1)).deleteBoard(boardId);
    }

    @Test
    void deleteBoard_WhenBoardNotExists_ShouldReturnNotFound() throws Exception {
        // Given
        Long boardId = 999L;
        when(boardService.deleteBoard(boardId)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/boards/{id}", boardId))
                .andExpect(status().isNotFound());

        verify(boardService, times(1)).deleteBoard(boardId);
    }

    @Test
    void searchByTitle_ShouldReturnMatchingBoards() throws Exception {
        // Given
        String title = "테스트";
        List<Board> boards = Arrays.asList(testBoard);
        when(boardService.searchByTitle(title)).thenReturn(boards);

        // When & Then
        mockMvc.perform(get("/api/boards/search/title")
                        .param("title", title))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("테스트 제목"));

        verify(boardService, times(1)).searchByTitle(title);
    }

    @Test
    void searchByAuthor_ShouldReturnMatchingBoards() throws Exception {
        // Given
        String author = "테스트";
        List<Board> boards = Arrays.asList(testBoard);
        when(boardService.searchByAuthor(author)).thenReturn(boards);

        // When & Then
        mockMvc.perform(get("/api/boards/search/author")
                        .param("author", author))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].author").value("테스트 작성자"));

        verify(boardService, times(1)).searchByAuthor(author);
    }

    @Test
    void searchByKeyword_ShouldReturnMatchingBoards() throws Exception {
        // Given
        String keyword = "테스트";
        List<Board> boards = Arrays.asList(testBoard);
        when(boardService.searchByKeyword(keyword)).thenReturn(boards);

        // When & Then
        mockMvc.perform(get("/api/boards/search")
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("테스트 제목"));

        verify(boardService, times(1)).searchByKeyword(keyword);
    }

    @Test
    void getRecentBoards_ShouldReturnRecentBoards() throws Exception {
        // Given
        List<Board> boards = Arrays.asList(testBoard);
        when(boardService.getRecentBoards()).thenReturn(boards);

        // When & Then
        mockMvc.perform(get("/api/boards/recent"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("테스트 제목"));

        verify(boardService, times(1)).getRecentBoards();
    }
} 