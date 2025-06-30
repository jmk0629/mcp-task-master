package com.tofumaker.controller;

import com.tofumaker.entity.Board;
import com.tofumaker.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@CrossOrigin(origins = "*")
@Tag(name = "Board", description = "게시판 관리 API")
public class BoardController {
    
    @Autowired
    private BoardService boardService;
    
    @Operation(summary = "모든 게시글 조회 (페이징)", description = "등록된 모든 게시글을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping
    public ResponseEntity<Page<Board>> getAllBoards(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Board> boards = boardService.getAllBoards(pageable);
        return ResponseEntity.ok(boards);
    }

    @Operation(summary = "모든 게시글 조회 (리스트)", description = "등록된 모든 게시글을 리스트로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping("/list")
    public ResponseEntity<List<Board>> getAllBoardsList() {
        List<Board> boards = boardService.getAllBoards();
        return ResponseEntity.ok(boards);
    }
    
    @Operation(summary = "게시글 상세 조회", description = "ID로 특정 게시글을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Board.class))),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Board> getBoardById(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long id) {
        Board board = boardService.getBoardById(id);
        if (board != null) {
            return ResponseEntity.ok(board);
        }
        return ResponseEntity.notFound().build();
    }
    
    @Operation(summary = "게시글 생성", description = "새로운 게시글을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = Board.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    public ResponseEntity<Board> createBoard(
            @Parameter(description = "생성할 게시글 정보", required = true) @RequestBody Board board) {
        Board createdBoard = boardService.createBoard(board);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBoard);
    }
    
    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = Board.class))),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Board> updateBoard(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long id,
            @Parameter(description = "수정할 게시글 정보", required = true) @RequestBody Board boardDetails) {
        Board updatedBoard = boardService.updateBoard(id, boardDetails);
        if (updatedBoard != null) {
            return ResponseEntity.ok(updatedBoard);
        }
        return ResponseEntity.notFound().build();
    }
    
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long id) {
        boolean deleted = boardService.deleteBoard(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @Operation(summary = "제목으로 검색 (페이징)", description = "제목에 포함된 키워드로 게시글을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping("/search/title")
    public ResponseEntity<Page<Board>> searchByTitle(
            @Parameter(description = "검색할 제목 키워드", required = true) @RequestParam String title,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Board> boards = boardService.searchByTitle(title, pageable);
        return ResponseEntity.ok(boards);
    }
    
    @Operation(summary = "작성자로 검색 (페이징)", description = "작성자명으로 게시글을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping("/search/author")
    public ResponseEntity<Page<Board>> searchByAuthor(
            @Parameter(description = "검색할 작성자명", required = true) @RequestParam String author,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Board> boards = boardService.searchByAuthor(author, pageable);
        return ResponseEntity.ok(boards);
    }
    
    @Operation(summary = "키워드로 검색 (페이징)", description = "제목과 내용에서 키워드로 게시글을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<Page<Board>> searchByKeyword(
            @Parameter(description = "검색할 키워드", required = true) @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Board> boards = boardService.searchByKeyword(keyword, pageable);
        return ResponseEntity.ok(boards);
    }
    
    @Operation(summary = "최신 게시글 조회 (페이징)", description = "최근에 작성된 게시글을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping("/recent")
    public ResponseEntity<Page<Board>> getRecentBoards(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardService.getRecentBoards(pageable);
        return ResponseEntity.ok(boards);
    }

    @Operation(summary = "최신 게시글 조회 (리스트)", description = "최근에 작성된 게시글을 리스트로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping("/recent/list")
    public ResponseEntity<List<Board>> getRecentBoardsList() {
        List<Board> boards = boardService.getRecentBoards();
        return ResponseEntity.ok(boards);
    }

    @Operation(summary = "활성 게시글 조회 (페이징)", description = "활성 상태인 게시글을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping("/active")
    public ResponseEntity<Page<Board>> getActiveBoards(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Board> boards = boardService.getActiveBoards(pageable);
        return ResponseEntity.ok(boards);
    }

    @Operation(summary = "활성 게시글 조회 (리스트)", description = "활성 상태인 게시글을 리스트로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping("/active/list")
    public ResponseEntity<List<Board>> getActiveBoardsList() {
        List<Board> boards = boardService.getActiveBoards();
        return ResponseEntity.ok(boards);
    }

    @Operation(summary = "인기 게시글 조회 (페이징)", description = "조회수 기준으로 인기 게시글을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping("/popular")
    public ResponseEntity<Page<Board>> getPopularBoards(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "viewCount"));
        Page<Board> boards = boardService.getPopularBoards(pageable);
        return ResponseEntity.ok(boards);
    }

    @Operation(summary = "인기 게시글 조회 (리스트)", description = "조회수 기준으로 인기 게시글을 리스트로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Board.class)))
    })
    @GetMapping("/popular/list")
    public ResponseEntity<List<Board>> getPopularBoardsList() {
        List<Board> boards = boardService.getPopularBoards();
        return ResponseEntity.ok(boards);
    }
} 