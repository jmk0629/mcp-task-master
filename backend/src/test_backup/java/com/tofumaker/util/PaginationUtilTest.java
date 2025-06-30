package com.tofumaker.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginationUtilTest {

    @Test
    void testCreatePageable_ValidParameters() {
        // Given
        int page = 1;
        int size = 20;
        String sort = "title";
        String direction = "asc";

        // When
        Pageable pageable = PaginationUtil.createPageable(page, size, sort, direction);

        // Then
        assertEquals(1, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "title"), pageable.getSort());
    }

    @Test
    void testCreatePageable_NegativePageNumber() {
        // Given
        int page = -1;
        int size = 10;
        String sort = "createdAt";
        String direction = "desc";

        // When
        Pageable pageable = PaginationUtil.createPageable(page, size, sort, direction);

        // Then
        assertEquals(0, pageable.getPageNumber()); // 음수는 0으로 보정
        assertEquals(10, pageable.getPageSize());
    }

    @Test
    void testCreatePageable_InvalidPageSize() {
        // Given - 너무 작은 크기
        Pageable pageable1 = PaginationUtil.createPageable(0, 0, "id", "asc");
        assertEquals(1, pageable1.getPageSize()); // 최소 1로 보정

        // Given - 너무 큰 크기
        Pageable pageable2 = PaginationUtil.createPageable(0, 200, "id", "asc");
        assertEquals(100, pageable2.getPageSize()); // 최대 100으로 보정
    }

    @Test
    void testCreatePageable_InvalidSortField() {
        // Given
        String invalidSort = "invalidField";

        // When
        Pageable pageable = PaginationUtil.createPageable(0, 10, invalidSort, "desc");

        // Then
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getSort());
    }

    @Test
    void testCreatePageable_InvalidSortDirection() {
        // Given
        String invalidDirection = "invalid";

        // When
        Pageable pageable = PaginationUtil.createPageable(0, 10, "id", invalidDirection);

        // Then
        assertEquals(Sort.by(Sort.Direction.DESC, "id"), pageable.getSort());
    }

    @Test
    void testCreatePageable_NullParameters() {
        // When
        Pageable pageable = PaginationUtil.createPageable(0, 10, null, null);

        // Then
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getSort());
    }

    @Test
    void testCreatePageable_EmptyStringParameters() {
        // When
        Pageable pageable = PaginationUtil.createPageable(0, 10, "", "");

        // Then
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getSort());
    }

    @Test
    void testCreatePageable_WithDefaultSort() {
        // When
        Pageable pageable = PaginationUtil.createPageable(2, 15);

        // Then
        assertEquals(2, pageable.getPageNumber());
        assertEquals(15, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getSort());
    }

    @Test
    void testCreateDefaultPageable() {
        // When
        Pageable pageable = PaginationUtil.createDefaultPageable();

        // Then
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getSort());
    }

    @Test
    void testValidateSortField_ValidFields() {
        // Test all valid fields
        for (String field : PaginationUtil.ALLOWED_SORT_FIELDS) {
            assertEquals(field, PaginationUtil.validateSortField(field));
        }
    }

    @Test
    void testValidateSortField_InvalidField() {
        // Given
        String invalidField = "invalidField";

        // When
        String result = PaginationUtil.validateSortField(invalidField);

        // Then
        assertEquals("createdAt", result);
    }

    @Test
    void testValidateSortField_NullAndEmpty() {
        assertEquals("createdAt", PaginationUtil.validateSortField(null));
        assertEquals("createdAt", PaginationUtil.validateSortField(""));
        assertEquals("createdAt", PaginationUtil.validateSortField("   "));
    }

    @Test
    void testValidateSortDirection_ValidDirections() {
        assertEquals(Sort.Direction.ASC, PaginationUtil.validateSortDirection("asc"));
        assertEquals(Sort.Direction.ASC, PaginationUtil.validateSortDirection("ASC"));
        assertEquals(Sort.Direction.DESC, PaginationUtil.validateSortDirection("desc"));
        assertEquals(Sort.Direction.DESC, PaginationUtil.validateSortDirection("DESC"));
    }

    @Test
    void testValidateSortDirection_InvalidDirection() {
        assertEquals(Sort.Direction.DESC, PaginationUtil.validateSortDirection("invalid"));
        assertEquals(Sort.Direction.DESC, PaginationUtil.validateSortDirection(null));
        assertEquals(Sort.Direction.DESC, PaginationUtil.validateSortDirection(""));
    }

    @Test
    void testCreatePageMetadata() {
        // Given
        List<String> content = Arrays.asList("item1", "item2", "item3");
        Pageable pageable = PaginationUtil.createPageable(1, 3, "id", "asc");
        Page<String> page = new PageImpl<>(content, pageable, 10);

        // When
        PaginationUtil.PageMetadata metadata = PaginationUtil.createPageMetadata(page);

        // Then
        assertEquals(1, metadata.getCurrentPage());
        assertEquals(3, metadata.getPageSize());
        assertEquals(10, metadata.getTotalElements());
        assertEquals(4, metadata.getTotalPages()); // ceil(10/3) = 4
        assertTrue(metadata.isHasNext());
        assertTrue(metadata.isHasPrevious());
        assertFalse(metadata.isFirst());
        assertFalse(metadata.isLast());
        assertEquals(3, metadata.getNumberOfElements());
    }

    @Test
    void testCreatePageMetadata_FirstPage() {
        // Given
        List<String> content = Arrays.asList("item1", "item2");
        Pageable pageable = PaginationUtil.createPageable(0, 5, "id", "asc");
        Page<String> page = new PageImpl<>(content, pageable, 2);

        // When
        PaginationUtil.PageMetadata metadata = PaginationUtil.createPageMetadata(page);

        // Then
        assertEquals(0, metadata.getCurrentPage());
        assertFalse(metadata.isHasNext());
        assertFalse(metadata.isHasPrevious());
        assertTrue(metadata.isFirst());
        assertTrue(metadata.isLast());
    }

    @Test
    void testValidatePageInfo_ValidParameters() {
        // When
        PaginationUtil.ValidationResult result = PaginationUtil.validatePageInfo(0, 10);

        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidatePageInfo_InvalidPage() {
        // When
        PaginationUtil.ValidationResult result = PaginationUtil.validatePageInfo(-1, 10);

        // Then
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("페이지 번호는 0 이상이어야 합니다"));
    }

    @Test
    void testValidatePageInfo_InvalidSize() {
        // Test size too small
        PaginationUtil.ValidationResult result1 = PaginationUtil.validatePageInfo(0, 0);
        assertFalse(result1.isValid());
        assertTrue(result1.getErrors().stream().anyMatch(error -> error.contains("페이지 크기는 1 이상이어야 합니다")));

        // Test size too large
        PaginationUtil.ValidationResult result2 = PaginationUtil.validatePageInfo(0, 150);
        assertFalse(result2.isValid());
        assertTrue(result2.getErrors().stream().anyMatch(error -> error.contains("페이지 크기는 100 이하여야 합니다")));
    }

    @Test
    void testValidatePageInfo_MultipleErrors() {
        // When
        PaginationUtil.ValidationResult result = PaginationUtil.validatePageInfo(-1, 0);

        // Then
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
    }

    @Test
    void testPageMetadataBuilder() {
        // When
        PaginationUtil.PageMetadata metadata = PaginationUtil.PageMetadata.builder()
                .currentPage(2)
                .pageSize(20)
                .totalElements(100)
                .totalPages(5)
                .hasNext(true)
                .hasPrevious(true)
                .isFirst(false)
                .isLast(false)
                .numberOfElements(20)
                .build();

        // Then
        assertEquals(2, metadata.getCurrentPage());
        assertEquals(20, metadata.getPageSize());
        assertEquals(100, metadata.getTotalElements());
        assertEquals(5, metadata.getTotalPages());
        assertTrue(metadata.isHasNext());
        assertTrue(metadata.isHasPrevious());
        assertFalse(metadata.isFirst());
        assertFalse(metadata.isLast());
        assertEquals(20, metadata.getNumberOfElements());
    }

    @Test
    void testConstants() {
        assertEquals(10, PaginationUtil.DEFAULT_PAGE_SIZE);
        assertEquals(100, PaginationUtil.MAX_PAGE_SIZE);
        assertFalse(PaginationUtil.ALLOWED_SORT_FIELDS.isEmpty());
        assertTrue(PaginationUtil.ALLOWED_SORT_FIELDS.contains("id"));
        assertTrue(PaginationUtil.ALLOWED_SORT_FIELDS.contains("createdAt"));
    }
} 