package com.tofumaker.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;

/**
 * 페이징 관련 유틸리티 클래스
 */
public class PaginationUtil {

    // 기본 페이지 크기
    public static final int DEFAULT_PAGE_SIZE = 10;
    
    // 최대 페이지 크기
    public static final int MAX_PAGE_SIZE = 100;
    
    // 허용되는 정렬 필드
    public static final List<String> ALLOWED_SORT_FIELDS = Arrays.asList(
        "id", "title", "author", "createdAt", "updatedAt", "viewCount"
    );

    /**
     * 안전한 Pageable 객체 생성
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 필드
     * @param direction 정렬 방향
     * @return 검증된 Pageable 객체
     */
    public static Pageable createPageable(int page, int size, String sort, String direction) {
        // 페이지 번호 검증
        int validPage = Math.max(0, page);
        
        // 페이지 크기 검증
        int validSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        
        // 정렬 필드 검증
        String validSort = ALLOWED_SORT_FIELDS.contains(sort) ? sort : "createdAt";
        
        // 정렬 방향 검증
        Sort.Direction validDirection;
        try {
            validDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            validDirection = Sort.Direction.DESC;
        }
        
        return PageRequest.of(validPage, validSize, Sort.by(validDirection, validSort));
    }

    /**
     * 기본 Pageable 객체 생성 (최신순 정렬)
     * 
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return Pageable 객체
     */
    public static Pageable createDefaultPageable(int page, int size) {
        return createPageable(page, size, "createdAt", "desc");
    }

    /**
     * 검색용 Pageable 객체 생성 (관련도순 정렬)
     * 
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return Pageable 객체
     */
    public static Pageable createSearchPageable(int page, int size) {
        return createPageable(page, size, "createdAt", "desc");
    }

    /**
     * 페이지 정보 검증
     * 
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검증된 페이지 정보 배열 [page, size]
     */
    public static int[] validatePageInfo(int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return new int[]{validPage, validSize};
    }

    /**
     * 정렬 필드 검증
     * 
     * @param sort 정렬 필드
     * @return 검증된 정렬 필드
     */
    public static String validateSortField(String sort) {
        return ALLOWED_SORT_FIELDS.contains(sort) ? sort : "createdAt";
    }

    /**
     * 정렬 방향 검증
     * 
     * @param direction 정렬 방향
     * @return 검증된 정렬 방향
     */
    public static Sort.Direction validateSortDirection(String direction) {
        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            return Sort.Direction.DESC;
        }
    }

    /**
     * 페이지 메타데이터 생성
     * 
     * @param page 현재 페이지
     * @param size 페이지 크기
     * @param totalElements 전체 요소 수
     * @return 페이지 메타데이터
     */
    public static PageMetadata createPageMetadata(int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;
        
        return new PageMetadata(page, size, totalElements, totalPages, hasNext, hasPrevious);
    }

    /**
     * 페이지 메타데이터 클래스
     */
    public static class PageMetadata {
        private final int currentPage;
        private final int pageSize;
        private final long totalElements;
        private final int totalPages;
        private final boolean hasNext;
        private final boolean hasPrevious;

        public PageMetadata(int currentPage, int pageSize, long totalElements, 
                          int totalPages, boolean hasNext, boolean hasPrevious) {
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }

        // Getters
        public int getCurrentPage() { return currentPage; }
        public int getPageSize() { return pageSize; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public boolean isHasNext() { return hasNext; }
        public boolean isHasPrevious() { return hasPrevious; }
        public boolean isFirst() { return currentPage == 0; }
        public boolean isLast() { return currentPage == totalPages - 1; }
    }
} 