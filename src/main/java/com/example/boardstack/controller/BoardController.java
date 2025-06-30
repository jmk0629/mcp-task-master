package com.example.boardstack.controller;

import com.example.boardstack.dto.BoardRequestDto;
import com.example.boardstack.dto.BoardResponseDto;
import com.example.boardstack.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "게시판 API", description = "게시글 CRUD 및 검색 기능을 제공하는 API")
public class BoardController {

    private final BoardService boardService;

    /**
     * 게시글 목록 조회
     * GET /api/boards
     */
    @Operation(summary = "게시글 목록 조회", description = "모든 게시글의 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponseDto.class)))
    })
    @GetMapping
    public ResponseEntity<List<BoardResponseDto>> getAllBoards() {
        log.info("게시글 목록 조회 요청");
        List<BoardResponseDto> boards = boardService.getAllBoards();
        return ResponseEntity.ok(boards);
    }

    /**
     * 게시글 상세 조회
     * GET /api/boards/{id}
     */
    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 조회 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BoardResponseDto> getBoardById(
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long id) {
        log.info("게시글 상세 조회 요청: ID={}", id);
        try {
            BoardResponseDto board = boardService.getBoardById(id);
            return ResponseEntity.ok(board);
        } catch (IllegalArgumentException e) {
            log.error("게시글 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 게시글 등록
     * POST /api/boards
     */
    @Operation(summary = "게시글 등록", description = "새로운 게시글을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "게시글 등록 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping
    public ResponseEntity<BoardResponseDto> createBoard(
            @Parameter(description = "게시글 등록 정보", required = true)
            @Valid @RequestBody BoardRequestDto requestDto) {
        log.info("게시글 등록 요청: {}", requestDto.getTitle());
        try {
            BoardResponseDto board = boardService.createBoard(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(board);
        } catch (Exception e) {
            log.error("게시글 등록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 게시글 수정
     * PUT /api/boards/{id}
     */
    @Operation(summary = "게시글 수정", description = "기존 게시글의 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 수정 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BoardResponseDto> updateBoard(
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "게시글 수정 정보", required = true)
            @Valid @RequestBody BoardRequestDto requestDto) {
        log.info("게시글 수정 요청: ID={}", id);
        try {
            BoardResponseDto board = boardService.updateBoard(id, requestDto);
            return ResponseEntity.ok(board);
        } catch (IllegalArgumentException e) {
            log.error("게시글 수정 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("게시글 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 게시글 삭제
     * DELETE /api/boards/{id}
     */
    @Operation(summary = "게시글 삭제", description = "특정 게시글을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "게시글 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long id) {
        log.info("게시글 삭제 요청: ID={}", id);
        try {
            boardService.deleteBoard(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("게시글 삭제 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 제목으로 게시글 검색
     * GET /api/boards/search/title?q={title}
     */
    @Operation(summary = "제목으로 게시글 검색", description = "제목에 특정 키워드가 포함된 게시글을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponseDto.class)))
    })
    @GetMapping("/search/title")
    public ResponseEntity<List<BoardResponseDto>> searchByTitle(
            @Parameter(description = "검색할 제목 키워드", required = true, example = "Spring")
            @RequestParam("q") String title) {
        log.info("제목으로 게시글 검색: {}", title);
        List<BoardResponseDto> boards = boardService.searchByTitle(title);
        return ResponseEntity.ok(boards);
    }

    /**
     * 작성자로 게시글 검색
     * GET /api/boards/search/writer?q={writer}
     */
    @Operation(summary = "작성자로 게시글 검색", description = "특정 작성자가 작성한 게시글을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponseDto.class)))
    })
    @GetMapping("/search/writer")
    public ResponseEntity<List<BoardResponseDto>> searchByWriter(
            @Parameter(description = "검색할 작성자명", required = true, example = "admin")
            @RequestParam("q") String writer) {
        log.info("작성자로 게시글 검색: {}", writer);
        List<BoardResponseDto> boards = boardService.searchByWriter(writer);
        return ResponseEntity.ok(boards);
    }

    /**
     * 키워드로 게시글 검색 (제목 또는 내용)
     * GET /api/boards/search?q={keyword}
     */
    @Operation(summary = "키워드로 게시글 검색", description = "제목 또는 내용에 특정 키워드가 포함된 게시글을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponseDto.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<List<BoardResponseDto>> searchByKeyword(
            @Parameter(description = "검색할 키워드", required = true, example = "OpenStack")
            @RequestParam("q") String keyword) {
        log.info("키워드로 게시글 검색: {}", keyword);
        List<BoardResponseDto> boards = boardService.searchByKeyword(keyword);
        return ResponseEntity.ok(boards);
    }

    /**
     * 인기 게시글 조회 (조회수 기준)
     * GET /api/boards/popular
     */
    @Operation(summary = "인기 게시글 조회", description = "조회수가 높은 순서로 게시글을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인기 게시글 조회 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponseDto.class)))
    })
    @GetMapping("/popular")
    public ResponseEntity<List<BoardResponseDto>> getPopularBoards() {
        log.info("인기 게시글 조회");
        List<BoardResponseDto> boards = boardService.getPopularBoards();
        return ResponseEntity.ok(boards);
    }

    /**
     * 최신 게시글 조회
     * GET /api/boards/recent
     */
    @Operation(summary = "최신 게시글 조회", description = "최근에 작성된 순서로 게시글을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "최신 게시글 조회 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponseDto.class)))
    })
    @GetMapping("/recent")
    public ResponseEntity<List<BoardResponseDto>> getRecentBoards() {
        log.info("최신 게시글 조회");
        List<BoardResponseDto> boards = boardService.getRecentBoards();
        return ResponseEntity.ok(boards);
    }
} 