package com.tofumaker.controller;

import com.tofumaker.document.BoardDocument;
import com.tofumaker.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Elasticsearch 기반 검색 API")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/all")
    @Operation(summary = "전체 검색", description = "제목과 내용에서 키워드를 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> searchAll(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<BoardDocument.BoardSearchResult> results = searchService.searchAll(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/advanced")
    @Operation(summary = "고급 검색", description = "제목, 내용, 작성자에서 키워드를 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> advancedSearch(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BoardDocument.BoardSearchResult> results = searchService.advancedSearch(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/fuzzy")
    @Operation(summary = "퍼지 검색", description = "오타를 허용하여 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> fuzzySearch(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BoardDocument.BoardSearchResult> results = searchService.fuzzySearch(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/title")
    @Operation(summary = "제목 검색", description = "제목에서만 키워드를 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> searchByTitle(
            @Parameter(description = "검색할 제목") @RequestParam String title,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BoardDocument.BoardSearchResult> results = searchService.searchByTitle(title, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/content")
    @Operation(summary = "내용 검색", description = "내용에서만 키워드를 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> searchByContent(
            @Parameter(description = "검색할 내용") @RequestParam String content,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BoardDocument.BoardSearchResult> results = searchService.searchByContent(content, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/author")
    @Operation(summary = "작성자 검색", description = "특정 작성자의 게시글을 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> searchByAuthor(
            @Parameter(description = "작성자명") @RequestParam String author,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BoardDocument.BoardSearchResult> results = searchService.searchByAuthor(author, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/category")
    @Operation(summary = "카테고리 검색", description = "특정 카테고리의 게시글을 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> searchByCategory(
            @Parameter(description = "카테고리명") @RequestParam String category,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BoardDocument.BoardSearchResult> results = searchService.searchByCategory(category, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/date-range")
    @Operation(summary = "기간별 검색", description = "특정 기간 내의 게시글을 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> searchByDateRange(
            @Parameter(description = "시작 날짜 (yyyy-MM-dd'T'HH:mm:ss)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd'T'HH:mm:ss)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BoardDocument.BoardSearchResult> results = searchService.searchByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/popular")
    @Operation(summary = "인기 게시글", description = "조회수가 높은 게시글을 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> searchPopular(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BoardDocument.BoardSearchResult> results = searchService.searchPopular(pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/latest")
    @Operation(summary = "최신 게시글", description = "최근에 작성된 게시글을 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> searchLatest(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BoardDocument.BoardSearchResult> results = searchService.searchLatest(pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/highlight")
    @Operation(summary = "하이라이트 검색", description = "검색어가 강조된 결과를 반환합니다.")
    public ResponseEntity<SearchService.SearchResultWithHighlight> searchWithHighlight(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        SearchService.SearchResultWithHighlight results = searchService.searchWithHighlight(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "검색 제안", description = "입력한 접두사에 대한 자동완성 제안을 반환합니다.")
    public ResponseEntity<List<String>> getSuggestions(
            @Parameter(description = "검색 접두사") @RequestParam String prefix) {

        List<String> suggestions = searchService.getSuggestions(prefix);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/similar/{documentId}")
    @Operation(summary = "유사 문서 검색", description = "특정 문서와 유사한 문서들을 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> findSimilar(
            @Parameter(description = "기준 문서 ID") @PathVariable String documentId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BoardDocument.BoardSearchResult> results = searchService.findSimilar(documentId, pageable);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/complex")
    @Operation(summary = "복합 검색", description = "여러 조건을 조합하여 검색합니다.")
    public ResponseEntity<Page<BoardDocument.BoardSearchResult>> complexSearch(
            @RequestBody SearchService.SearchCriteria criteria,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<BoardDocument.BoardSearchResult> results = searchService.complexSearch(criteria, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/statistics")
    @Operation(summary = "검색 통계", description = "검색 관련 통계 정보를 반환합니다.")
    public ResponseEntity<SearchService.SearchStatistics> getSearchStatistics() {
        SearchService.SearchStatistics statistics = searchService.getSearchStatistics();
        return ResponseEntity.ok(statistics);
    }

    @PostMapping("/sync")
    @Operation(summary = "인덱스 동기화", description = "JPA 데이터를 Elasticsearch에 동기화합니다.")
    public ResponseEntity<Map<String, String>> syncIndex() {
        try {
            searchService.syncIndex();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Index synchronization completed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to sync index: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "검색 서비스 상태", description = "Elasticsearch 연결 상태를 확인합니다.")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            long totalDocuments = searchService.getSearchStatistics().getTotalDocuments();
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "totalDocuments", totalDocuments,
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "unhealthy",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }
} 