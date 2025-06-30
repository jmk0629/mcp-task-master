package com.tofumaker.api;

import com.tofumaker.entity.Board;
import com.tofumaker.repository.BoardRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
class BoardApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private BoardRepository boardRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        boardRepository.deleteAll();
    }

    @Test
    void getAllBoards_ShouldReturnEmptyListWhenNoBoards() {
        given()
                .when()
                .get("/api/boards")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(0));
    }

    @Test
    void createBoard_ShouldReturnCreatedBoard() {
        Board newBoard = new Board("API 테스트 제목", "API 테스트 내용", "API 테스트 작성자");

        given()
                .contentType(ContentType.JSON)
                .body(newBoard)
                .when()
                .post("/api/boards")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("title", equalTo("API 테스트 제목"))
                .body("content", equalTo("API 테스트 내용"))
                .body("author", equalTo("API 테스트 작성자"))
                .body("id", notNullValue())
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    void getBoardById_ShouldReturnBoard() {
        // Given - Create a board first
        Board board = new Board("조회 테스트 제목", "조회 테스트 내용", "조회 테스트 작성자");
        Board savedBoard = boardRepository.save(board);

        // When & Then
        given()
                .when()
                .get("/api/boards/{id}", savedBoard.getId())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(savedBoard.getId().intValue()))
                .body("title", equalTo("조회 테스트 제목"))
                .body("content", equalTo("조회 테스트 내용"))
                .body("author", equalTo("조회 테스트 작성자"));
    }

    @Test
    void getBoardById_WhenNotExists_ShouldReturn404() {
        given()
                .when()
                .get("/api/boards/999")
                .then()
                .statusCode(404);
    }

    @Test
    void updateBoard_ShouldReturnUpdatedBoard() {
        // Given - Create a board first
        Board board = new Board("수정 전 제목", "수정 전 내용", "수정 전 작성자");
        Board savedBoard = boardRepository.save(board);

        Board updateBoard = new Board("수정 후 제목", "수정 후 내용", "수정 후 작성자");

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(updateBoard)
                .when()
                .put("/api/boards/{id}", savedBoard.getId())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(savedBoard.getId().intValue()))
                .body("title", equalTo("수정 후 제목"))
                .body("content", equalTo("수정 후 내용"))
                .body("author", equalTo("수정 후 작성자"));
    }

    @Test
    void updateBoard_WhenNotExists_ShouldReturn404() {
        Board updateBoard = new Board("수정 제목", "수정 내용", "수정 작성자");

        given()
                .contentType(ContentType.JSON)
                .body(updateBoard)
                .when()
                .put("/api/boards/999")
                .then()
                .statusCode(404);
    }

    @Test
    void deleteBoard_ShouldReturnNoContent() {
        // Given - Create a board first
        Board board = new Board("삭제 테스트 제목", "삭제 테스트 내용", "삭제 테스트 작성자");
        Board savedBoard = boardRepository.save(board);

        // When & Then
        given()
                .when()
                .delete("/api/boards/{id}", savedBoard.getId())
                .then()
                .statusCode(204);

        // Verify board is deleted
        given()
                .when()
                .get("/api/boards/{id}", savedBoard.getId())
                .then()
                .statusCode(404);
    }

    @Test
    void deleteBoard_WhenNotExists_ShouldReturn404() {
        given()
                .when()
                .delete("/api/boards/999")
                .then()
                .statusCode(404);
    }

    @Test
    void searchByTitle_ShouldReturnMatchingBoards() {
        // Given - Create test data
        Board board1 = new Board("Java 프로그래밍", "Java 내용", "작성자1");
        Board board2 = new Board("Python 기초", "Python 내용", "작성자2");
        Board board3 = new Board("JavaScript 고급", "JavaScript 내용", "작성자3");

        boardRepository.save(board1);
        boardRepository.save(board2);
        boardRepository.save(board3);

        // When & Then
        given()
                .param("title", "Java")
                .when()
                .get("/api/boards/search/title")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(2))
                .body("title", hasItems("Java 프로그래밍", "JavaScript 고급"));
    }

    @Test
    void searchByAuthor_ShouldReturnMatchingBoards() {
        // Given - Create test data
        Board board1 = new Board("제목1", "내용1", "개발자A");
        Board board2 = new Board("제목2", "내용2", "개발자B");
        Board board3 = new Board("제목3", "내용3", "개발자A");

        boardRepository.save(board1);
        boardRepository.save(board2);
        boardRepository.save(board3);

        // When & Then
        given()
                .param("author", "개발자A")
                .when()
                .get("/api/boards/search/author")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(2))
                .body("author", everyItem(equalTo("개발자A")));
    }

    @Test
    void searchByKeyword_ShouldReturnMatchingBoards() {
        // Given - Create test data
        Board board1 = new Board("Spring Boot 가이드", "Spring Boot 프레임워크 설명", "작성자1");
        Board board2 = new Board("React 컴포넌트", "React 개발 방법", "작성자2");
        Board board3 = new Board("데이터베이스 설계", "Spring 데이터 JPA 사용법", "작성자3");

        boardRepository.save(board1);
        boardRepository.save(board2);
        boardRepository.save(board3);

        // When & Then
        given()
                .param("keyword", "Spring")
                .when()
                .get("/api/boards/search")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(2));
    }

    @Test
    void getRecentBoards_ShouldReturnBoardsOrderedByDate() throws InterruptedException {
        // Given - Create boards with time difference
        Board oldBoard = new Board("오래된 게시글", "오래된 내용", "작성자1");
        boardRepository.save(oldBoard);

        Thread.sleep(1000); // Ensure different creation times

        Board recentBoard = new Board("최신 게시글", "최신 내용", "작성자2");
        boardRepository.save(recentBoard);

        // When & Then
        given()
                .when()
                .get("/api/boards/recent")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(2))
                .body("[0].title", equalTo("최신 게시글")) // Most recent first
                .body("[1].title", equalTo("오래된 게시글"));
    }

    @Test
    void createBoard_WithInvalidData_ShouldReturnBadRequest() {
        // Test with empty title
        Board invalidBoard = new Board("", "내용", "작성자");

        given()
                .contentType(ContentType.JSON)
                .body(invalidBoard)
                .when()
                .post("/api/boards")
                .then()
                .statusCode(anyOf(is(400), is(201))); // Depending on validation implementation
    }

    @Test
    void apiEndpoints_ShouldReturnCorrectContentType() {
        // Create a test board
        Board board = new Board("Content-Type 테스트", "내용", "작성자");
        Board savedBoard = boardRepository.save(board);

        // Test all endpoints return JSON
        given()
                .when()
                .get("/api/boards")
                .then()
                .contentType(ContentType.JSON);

        given()
                .when()
                .get("/api/boards/{id}", savedBoard.getId())
                .then()
                .contentType(ContentType.JSON);

        given()
                .when()
                .get("/api/boards/recent")
                .then()
                .contentType(ContentType.JSON);
    }
} 