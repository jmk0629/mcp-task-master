package com.tofumaker.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tofumaker.entity.Board;
import com.tofumaker.repository.BoardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
class BoardIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        boardRepository.deleteAll();
    }

    @Test
    void createAndRetrieveBoard_ShouldWorkEndToEnd() throws Exception {
        // Given
        Board newBoard = new Board("통합 테스트 제목", "통합 테스트 내용", "테스트 작성자");

        // When - Create board
        String response = mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBoard)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("통합 테스트 제목"))
                .andExpect(jsonPath("$.content").value("통합 테스트 내용"))
                .andExpect(jsonPath("$.author").value("테스트 작성자"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Board createdBoard = objectMapper.readValue(response, Board.class);
        Long boardId = createdBoard.getId();

        // Then - Retrieve the created board
        mockMvc.perform(get("/api/boards/{id}", boardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(boardId))
                .andExpect(jsonPath("$.title").value("통합 테스트 제목"))
                .andExpect(jsonPath("$.content").value("통합 테스트 내용"))
                .andExpect(jsonPath("$.author").value("테스트 작성자"));
    }

    @Test
    void updateBoard_ShouldModifyExistingBoard() throws Exception {
        // Given - Create a board first
        Board originalBoard = new Board("원본 제목", "원본 내용", "원본 작성자");
        Board savedBoard = boardRepository.save(originalBoard);

        Board updateBoard = new Board("수정된 제목", "수정된 내용", "수정된 작성자");

        // When - Update the board
        mockMvc.perform(put("/api/boards/{id}", savedBoard.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBoard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedBoard.getId()))
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.content").value("수정된 내용"))
                .andExpect(jsonPath("$.author").value("수정된 작성자"));

        // Then - Verify the board was updated
        mockMvc.perform(get("/api/boards/{id}", savedBoard.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.content").value("수정된 내용"));
    }

    @Test
    void deleteBoard_ShouldRemoveBoardFromDatabase() throws Exception {
        // Given - Create a board first
        Board board = new Board("삭제될 제목", "삭제될 내용", "삭제될 작성자");
        Board savedBoard = boardRepository.save(board);

        // When - Delete the board
        mockMvc.perform(delete("/api/boards/{id}", savedBoard.getId()))
                .andExpect(status().isNoContent());

        // Then - Verify the board was deleted
        mockMvc.perform(get("/api/boards/{id}", savedBoard.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchFunctionality_ShouldReturnMatchingBoards() throws Exception {
        // Given - Create multiple boards
        Board board1 = new Board("Java 프로그래밍", "Java 관련 내용", "개발자1");
        Board board2 = new Board("Python 기초", "Python 학습 내용", "개발자2");
        Board board3 = new Board("Spring Boot", "Spring Boot 튜토리얼", "개발자1");

        boardRepository.save(board1);
        boardRepository.save(board2);
        boardRepository.save(board3);

        // When & Then - Search by title
        mockMvc.perform(get("/api/boards/search/title")
                        .param("title", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Java 프로그래밍"));

        // When & Then - Search by author
        mockMvc.perform(get("/api/boards/search/author")
                        .param("author", "개발자1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].author", everyItem(is("개발자1"))));

        // When & Then - Search by keyword
        mockMvc.perform(get("/api/boards/search")
                        .param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Spring Boot"));
    }

    @Test
    void getAllBoards_ShouldReturnAllBoardsInDatabase() throws Exception {
        // Given - Create multiple boards
        Board board1 = new Board("제목1", "내용1", "작성자1");
        Board board2 = new Board("제목2", "내용2", "작성자2");
        Board board3 = new Board("제목3", "내용3", "작성자3");

        boardRepository.save(board1);
        boardRepository.save(board2);
        boardRepository.save(board3);

        // When & Then
        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("제목1", "제목2", "제목3")));
    }

    @Test
    void getRecentBoards_ShouldReturnBoardsOrderedByCreationDate() throws Exception {
        // Given - Create boards with different creation times
        Board oldBoard = new Board("오래된 게시글", "오래된 내용", "작성자1");
        Board recentBoard = new Board("최신 게시글", "최신 내용", "작성자2");

        boardRepository.save(oldBoard);
        Thread.sleep(1000); // Ensure different creation times
        boardRepository.save(recentBoard);

        // When & Then
        mockMvc.perform(get("/api/boards/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("최신 게시글")) // Most recent first
                .andExpect(jsonPath("$[1].title").value("오래된 게시글"));
    }

    @Test
    void boardNotFound_ShouldReturn404() throws Exception {
        // When & Then - Try to get non-existent board
        mockMvc.perform(get("/api/boards/999"))
                .andExpect(status().isNotFound());

        // When & Then - Try to update non-existent board
        Board updateBoard = new Board("수정 제목", "수정 내용", "수정 작성자");
        mockMvc.perform(put("/api/boards/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBoard)))
                .andExpect(status().isNotFound());

        // When & Then - Try to delete non-existent board
        mockMvc.perform(delete("/api/boards/999"))
                .andExpect(status().isNotFound());
    }
} 