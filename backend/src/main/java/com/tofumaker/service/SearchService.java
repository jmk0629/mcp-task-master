package com.tofumaker.service;

import com.tofumaker.document.BoardDocument;
import com.tofumaker.entity.Board;
import com.tofumaker.repository.BoardRepository;
import com.tofumaker.repository.elasticsearch.BoardSearchRepository;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private static final String BOARD_INDEX = "boards";

    @Autowired
    private BoardSearchRepository boardSearchRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private RestHighLevelClient elasticsearchClient;

    /**
     * 전체 텍스트 검색 (제목 + 내용)
     */
    public Page<BoardDocument.BoardSearchResult> searchAll(String keyword, Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findByTitleOrContentContaining(keyword, pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in searchAll", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 고급 검색 (제목, 내용, 작성자)
     */
    public Page<BoardDocument.BoardSearchResult> advancedSearch(String keyword, Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findByAdvancedSearch(keyword, pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in advancedSearch", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 퍼지 검색 (오타 허용)
     */
    public Page<BoardDocument.BoardSearchResult> fuzzySearch(String keyword, Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findByFuzzySearch(keyword, pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in fuzzySearch", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 제목으로 검색
     */
    public Page<BoardDocument.BoardSearchResult> searchByTitle(String title, Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findByTitleContaining(title, pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in searchByTitle", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 내용으로 검색
     */
    public Page<BoardDocument.BoardSearchResult> searchByContent(String content, Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findByContentContaining(content, pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in searchByContent", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 작성자로 검색
     */
    public Page<BoardDocument.BoardSearchResult> searchByAuthor(String author, Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findByAuthor(author, pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in searchByAuthor", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 카테고리로 검색
     */
    public Page<BoardDocument.BoardSearchResult> searchByCategory(String category, Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findByCategory(category, pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in searchByCategory", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 기간별 검색
     */
    public Page<BoardDocument.BoardSearchResult> searchByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findByCreatedAtBetween(startDate, endDate, pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in searchByDateRange", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 인기 게시글 검색
     */
    public Page<BoardDocument.BoardSearchResult> searchPopular(Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findByActiveTrueOrderByViewCountDesc(pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in searchPopular", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 최신 게시글 검색
     */
    public Page<BoardDocument.BoardSearchResult> searchLatest(Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findByActiveTrueOrderByCreatedAtDesc(pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in searchLatest", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 하이라이트 검색 (검색어 강조)
     */
    public SearchResultWithHighlight searchWithHighlight(String keyword, Pageable pageable) {
        try {
            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(multiMatchQuery(keyword, "title", "content"))
                .withPageable(pageable)
                .build();

            SearchHits<BoardDocument> searchHits = elasticsearchOperations.search(
                searchQuery, BoardDocument.class, IndexCoordinates.of(BOARD_INDEX));

            List<BoardDocument.BoardSearchResult> results = searchHits.stream()
                .map(hit -> {
                    BoardDocument.BoardSearchResult result = hit.getContent().toSearchResult();
                    result.setScore(hit.getScore());
                    return result;
                })
                .collect(Collectors.toList());

            Map<String, List<String>> highlights = searchHits.stream()
                .collect(Collectors.toMap(
                    hit -> hit.getContent().getId(),
                    hit -> hit.getHighlightFields().values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toList())
                ));

            Page<BoardDocument.BoardSearchResult> page = new PageImpl<>(results, pageable, searchHits.getTotalHits());
            return new SearchResultWithHighlight(page, highlights);

        } catch (Exception e) {
            logger.error("Error in searchWithHighlight", e);
            return new SearchResultWithHighlight(Page.empty(pageable), new HashMap<>());
        }
    }

    /**
     * 자동완성 제안
     */
    public List<String> getSuggestions(String prefix) {
        try {
            List<BoardDocument> documents = boardSearchRepository.findByTitleStartingWith(prefix);
            return documents.stream()
                .map(BoardDocument::getTitle)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error in getSuggestions", e);
            return new ArrayList<>();
        }
    }

    /**
     * 유사 문서 검색
     */
    public Page<BoardDocument.BoardSearchResult> findSimilar(String documentId, Pageable pageable) {
        try {
            Page<BoardDocument> documents = boardSearchRepository.findSimilarDocuments(documentId, pageable);
            return convertToSearchResults(documents, pageable);
        } catch (Exception e) {
            logger.error("Error in findSimilar", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 복합 검색 (여러 조건)
     */
    public Page<BoardDocument.BoardSearchResult> complexSearch(SearchCriteria criteria, Pageable pageable) {
        try {
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

            // 키워드 검색
            if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
                queryBuilder.withQuery(multiMatchQuery(criteria.getKeyword(), "title", "content"));
            }

            // 필터 조건들
            if (criteria.getAuthor() != null) {
                queryBuilder.withFilter(termQuery("author", criteria.getAuthor()));
            }

            if (criteria.getCategory() != null) {
                queryBuilder.withFilter(termQuery("category", criteria.getCategory()));
            }

            if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
                queryBuilder.withFilter(rangeQuery("createdAt")
                    .gte(criteria.getStartDate())
                    .lte(criteria.getEndDate()));
            }

            if (criteria.getMinViews() != null) {
                queryBuilder.withFilter(rangeQuery("viewCount").gte(criteria.getMinViews()));
            }

            // 활성 상태 필터
            queryBuilder.withFilter(termQuery("active", true));

            queryBuilder.withPageable(pageable);

            SearchHits<BoardDocument> searchHits = elasticsearchOperations.search(
                queryBuilder.build(), BoardDocument.class, IndexCoordinates.of(BOARD_INDEX));

            List<BoardDocument.BoardSearchResult> results = searchHits.stream()
                .map(hit -> {
                    BoardDocument.BoardSearchResult result = hit.getContent().toSearchResult();
                    result.setScore(hit.getScore());
                    return result;
                })
                .collect(Collectors.toList());

            return new PageImpl<>(results, pageable, searchHits.getTotalHits());

        } catch (Exception e) {
            logger.error("Error in complexSearch", e);
            return Page.empty(pageable);
        }
    }

    /**
     * 검색 통계
     */
    public SearchStatistics getSearchStatistics() {
        try {
            long totalDocuments = boardSearchRepository.count();
            
            // 최근 7일간 생성된 문서 수
            LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
            Page<BoardDocument> recentDocs = boardSearchRepository.findByCreatedAtBetween(
                weekAgo, LocalDateTime.now(), Pageable.ofSize(1));
            long recentDocuments = recentDocs.getTotalElements();

            // 인기 작성자 (상위 5명)
            List<String> topAuthors = new ArrayList<>();

            return new SearchStatistics(totalDocuments, recentDocuments, topAuthors);

        } catch (Exception e) {
            logger.error("Error in getSearchStatistics", e);
            return new SearchStatistics(0, 0, new ArrayList<>());
        }
    }

    /**
     * 인덱스 동기화 - JPA 데이터를 Elasticsearch에 동기화
     */
    @Transactional
    public void syncIndex() {
        try {
            logger.info("Starting index synchronization...");
            
            // 기존 인덱스 삭제 후 재생성
            boardSearchRepository.deleteAll();
            
            // JPA에서 모든 활성 게시글 조회
            List<Board> boards = boardRepository.findByActiveTrue();
            
            // Elasticsearch 문서로 변환 후 저장
            List<BoardDocument> documents = boards.stream()
                .map(BoardDocument::fromEntity)
                .collect(Collectors.toList());
            
            boardSearchRepository.saveAll(documents);
            
            logger.info("Index synchronization completed. Synced {} documents", documents.size());
            
        } catch (Exception e) {
            logger.error("Error in syncIndex", e);
            throw new RuntimeException("Failed to sync index", e);
        }
    }

    /**
     * 단일 문서 인덱싱
     */
    @Transactional
    public void indexBoard(Board board) {
        try {
            BoardDocument document = BoardDocument.fromEntity(board);
            boardSearchRepository.save(document);
            logger.debug("Indexed board: {}", board.getId());
        } catch (Exception e) {
            logger.error("Error indexing board: {}", board.getId(), e);
        }
    }

    /**
     * 문서 삭제
     */
    @Transactional
    public void deleteFromIndex(Long boardId) {
        try {
            boardSearchRepository.deleteById(String.valueOf(boardId));
            logger.debug("Deleted board from index: {}", boardId);
        } catch (Exception e) {
            logger.error("Error deleting board from index: {}", boardId, e);
        }
    }

    // Helper methods
    private Page<BoardDocument.BoardSearchResult> convertToSearchResults(Page<BoardDocument> documents, Pageable pageable) {
        List<BoardDocument.BoardSearchResult> results = documents.getContent().stream()
            .map(BoardDocument::toSearchResult)
            .collect(Collectors.toList());
        
        return new PageImpl<>(results, pageable, documents.getTotalElements());
    }

    // DTOs
    public static class SearchCriteria {
        private String keyword;
        private String author;
        private String category;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer minViews;
        private Integer maxViews;

        // Getters and Setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public Integer getMinViews() { return minViews; }
        public void setMinViews(Integer minViews) { this.minViews = minViews; }
        public Integer getMaxViews() { return maxViews; }
        public void setMaxViews(Integer maxViews) { this.maxViews = maxViews; }
    }

    public static class SearchResultWithHighlight {
        private final Page<BoardDocument.BoardSearchResult> results;
        private final Map<String, List<String>> highlights;

        public SearchResultWithHighlight(Page<BoardDocument.BoardSearchResult> results, Map<String, List<String>> highlights) {
            this.results = results;
            this.highlights = highlights;
        }

        public Page<BoardDocument.BoardSearchResult> getResults() { return results; }
        public Map<String, List<String>> getHighlights() { return highlights; }
    }

    public static class SearchStatistics {
        private final long totalDocuments;
        private final long recentDocuments;
        private final List<String> topAuthors;

        public SearchStatistics(long totalDocuments, long recentDocuments, List<String> topAuthors) {
            this.totalDocuments = totalDocuments;
            this.recentDocuments = recentDocuments;
            this.topAuthors = topAuthors;
        }

        public long getTotalDocuments() { return totalDocuments; }
        public long getRecentDocuments() { return recentDocuments; }
        public List<String> getTopAuthors() { return topAuthors; }
    }
} 