package com.tofumaker.service;

import com.tofumaker.document.BoardDocument;
import com.tofumaker.entity.Board;
import com.tofumaker.repository.BoardRepository;
import com.tofumaker.repository.elasticsearch.BoardSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private BoardSearchRepository boardSearchRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private SearchService searchService;

    private BoardDocument testDocument;
    private Board testBoard;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        testPageable = PageRequest.of(0, 10);
        
        // Test Board 생성
        testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setTitle("테스트 제목");
        testBoard.setContent("테스트 내용입니다.");
        testBoard.setAuthor("테스트 작성자");
        testBoard.setCreatedAt(LocalDateTime.now());
        testBoard.setUpdatedAt(LocalDateTime.now());
        testBoard.setViewCount(10);
        testBoard.setActive(true);

        // Test BoardDocument 생성
        testDocument = BoardDocument.fromEntity(testBoard);
    }

    @Test
    void searchAll_성공() {
        // Given
        String keyword = "테스트";
        List<BoardDocument> documents = Arrays.asList(testDocument);
        Page<BoardDocument> mockPage = new PageImpl<>(documents, testPageable, 1);
        
        when(boardSearchRepository.findByTitleOrContentContaining(keyword, testPageable))
            .thenReturn(mockPage);

        // When
        Page<BoardDocument.BoardSearchResult> result = searchService.searchAll(keyword, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("테스트 제목", result.getContent().get(0).getTitle());
        verify(boardSearchRepository).findByTitleOrContentContaining(keyword, testPageable);
    }

    @Test
    void searchAll_예외발생시_빈페이지반환() {
        // Given
        String keyword = "테스트";
        when(boardSearchRepository.findByTitleOrContentContaining(keyword, testPageable))
            .thenThrow(new RuntimeException("Elasticsearch 연결 오류"));

        // When
        Page<BoardDocument.BoardSearchResult> result = searchService.searchAll(keyword, testPageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void advancedSearch_성공() {
        // Given
        String keyword = "고급검색";
        List<BoardDocument> documents = Arrays.asList(testDocument);
        Page<BoardDocument> mockPage = new PageImpl<>(documents, testPageable, 1);
        
        when(boardSearchRepository.findByAdvancedSearch(keyword, testPageable))
            .thenReturn(mockPage);

        // When
        Page<BoardDocument.BoardSearchResult> result = searchService.advancedSearch(keyword, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(boardSearchRepository).findByAdvancedSearch(keyword, testPageable);
    }

    @Test
    void searchByTitle_성공() {
        // Given
        String title = "테스트 제목";
        List<BoardDocument> documents = Arrays.asList(testDocument);
        Page<BoardDocument> mockPage = new PageImpl<>(documents, testPageable, 1);
        
        when(boardSearchRepository.findByTitleContaining(title, testPageable))
            .thenReturn(mockPage);

        // When
        Page<BoardDocument.BoardSearchResult> result = searchService.searchByTitle(title, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(title, result.getContent().get(0).getTitle());
        verify(boardSearchRepository).findByTitleContaining(title, testPageable);
    }

    @Test
    void getSuggestions_성공() {
        // Given
        String prefix = "테스트";
        List<BoardDocument> documents = Arrays.asList(testDocument);
        
        when(boardSearchRepository.findByTitleStartingWith(prefix))
            .thenReturn(documents);

        // When
        List<String> suggestions = searchService.getSuggestions(prefix);

        // Then
        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        assertEquals("테스트 제목", suggestions.get(0));
        verify(boardSearchRepository).findByTitleStartingWith(prefix);
    }

    @Test
    void syncIndex_성공() {
        // Given
        List<Board> boards = Arrays.asList(testBoard);
        when(boardRepository.findByActiveTrue()).thenReturn(boards);

        // When
        assertDoesNotThrow(() -> searchService.syncIndex());

        // Then
        verify(boardSearchRepository).deleteAll();
        verify(boardRepository).findByActiveTrue();
        verify(boardSearchRepository).saveAll(anyList());
    }

    @Test
    void indexBoard_성공() {
        // When
        assertDoesNotThrow(() -> searchService.indexBoard(testBoard));

        // Then
        verify(boardSearchRepository).save(any(BoardDocument.class));
    }

    @Test
    void deleteFromIndex_성공() {
        // Given
        Long boardId = 1L;

        // When
        assertDoesNotThrow(() -> searchService.deleteFromIndex(boardId));

        // Then
        verify(boardSearchRepository).deleteById(String.valueOf(boardId));
    }

    @Test
    void getSearchStatistics_성공() {
        // Given
        when(boardSearchRepository.count()).thenReturn(100L);
        
        Page<BoardDocument> recentPage = new PageImpl<>(Arrays.asList(testDocument), testPageable, 10);
        when(boardSearchRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(recentPage);

        // When
        SearchService.SearchStatistics statistics = searchService.getSearchStatistics();

        // Then
        assertNotNull(statistics);
        assertEquals(100L, statistics.getTotalDocuments());
        assertEquals(10L, statistics.getRecentDocuments());
        assertNotNull(statistics.getTopAuthors());
    }
} 