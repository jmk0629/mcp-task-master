package com.tofumaker.repository.elasticsearch;

import com.tofumaker.document.BoardDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoardSearchRepository extends ElasticsearchRepository<BoardDocument, String> {

    /**
     * 제목으로 검색
     */
    Page<BoardDocument> findByTitleContaining(String title, Pageable pageable);

    /**
     * 내용으로 검색
     */
    Page<BoardDocument> findByContentContaining(String content, Pageable pageable);

    /**
     * 작성자로 검색
     */
    Page<BoardDocument> findByAuthor(String author, Pageable pageable);

    /**
     * 활성 상태인 게시글 검색
     */
    Page<BoardDocument> findByActiveTrue(Pageable pageable);

    /**
     * 카테고리별 검색
     */
    Page<BoardDocument> findByCategory(String category, Pageable pageable);

    /**
     * 기간별 검색
     */
    Page<BoardDocument> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * 복합 검색 - 제목 또는 내용에서 키워드 검색
     */
    @Query("{\"bool\": {\"must\": [{\"bool\": {\"should\": [{\"match\": {\"title\": \"?0\"}}, {\"match\": {\"content\": \"?0\"}}]}}], \"filter\": [{\"term\": {\"active\": true}}]}}")
    Page<BoardDocument> findByTitleOrContentContaining(String keyword, Pageable pageable);

    /**
     * 고급 검색 - 제목, 내용, 작성자에서 키워드 검색
     */
    @Query("{\"bool\": {\"must\": [{\"bool\": {\"should\": [{\"match\": {\"title\": {\"query\": \"?0\", \"boost\": 2}}}, {\"match\": {\"content\": \"?0\"}}, {\"match\": {\"author\": \"?0\"}}]}}], \"filter\": [{\"term\": {\"active\": true}}]}}")
    Page<BoardDocument> findByAdvancedSearch(String keyword, Pageable pageable);

    /**
     * 퍼지 검색 - 오타 허용 검색
     */
    @Query("{\"bool\": {\"must\": [{\"bool\": {\"should\": [{\"fuzzy\": {\"title\": {\"value\": \"?0\", \"fuzziness\": \"AUTO\"}}}, {\"fuzzy\": {\"content\": {\"value\": \"?0\", \"fuzziness\": \"AUTO\"}}}]}}], \"filter\": [{\"term\": {\"active\": true}}]}}")
    Page<BoardDocument> findByFuzzySearch(String keyword, Pageable pageable);

    /**
     * 하이라이트 검색 - 검색어 강조
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title^2\", \"content\"], \"type\": \"best_fields\"}}], \"filter\": [{\"term\": {\"active\": true}}]}}")
    Page<BoardDocument> findByHighlightSearch(String keyword, Pageable pageable);

    /**
     * 인기 게시글 검색 (조회수 기준)
     */
    Page<BoardDocument> findByActiveTrueOrderByViewCountDesc(Pageable pageable);

    /**
     * 최신 게시글 검색
     */
    Page<BoardDocument> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 태그로 검색
     */
    Page<BoardDocument> findByTagsContaining(String tag, Pageable pageable);

    /**
     * 작성자와 기간으로 검색
     */
    Page<BoardDocument> findByAuthorAndCreatedAtBetween(String author, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * 조회수 범위로 검색
     */
    Page<BoardDocument> findByViewCountBetween(Integer minViews, Integer maxViews, Pageable pageable);

    /**
     * 검색 제안 - 자동완성용
     */
    @Query("{\"bool\": {\"must\": [{\"prefix\": {\"title\": \"?0\"}}], \"filter\": [{\"term\": {\"active\": true}}]}}")
    List<BoardDocument> findByTitleStartingWith(String prefix);

    /**
     * 유사 문서 검색
     */
    @Query("{\"more_like_this\": {\"fields\": [\"title\", \"content\"], \"like\": [{\"_index\": \"boards\", \"_id\": \"?0\"}], \"min_term_freq\": 1, \"max_query_terms\": 12}}")
    Page<BoardDocument> findSimilarDocuments(String documentId, Pageable pageable);
} 