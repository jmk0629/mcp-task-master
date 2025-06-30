package com.tofumaker.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

@Document(indexName = "boards")
@Setting(settingPath = "/elasticsearch/board-settings.json")
public class BoardDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "korean")
    private String title;

    @Field(type = FieldType.Text, analyzer = "korean")
    private String content;

    @Field(type = FieldType.Keyword)
    private String author;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Boolean)
    private Boolean active;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Text, analyzer = "keyword")
    private String tags;

    @Field(type = FieldType.Long)
    private Long originalId; // JPA 엔티티의 ID

    // Constructors
    public BoardDocument() {}

    public BoardDocument(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.viewCount = 0;
        this.active = true;
    }

    // Static factory method
    public static BoardDocument fromEntity(com.tofumaker.entity.Board board) {
        BoardDocument document = new BoardDocument();
        document.setId(String.valueOf(board.getId()));
        document.setOriginalId(board.getId());
        document.setTitle(board.getTitle());
        document.setContent(board.getContent());
        document.setAuthor(board.getAuthor());
        document.setCreatedAt(board.getCreatedAt());
        document.setUpdatedAt(board.getUpdatedAt());
        document.setViewCount(board.getViewCount() != null ? board.getViewCount().intValue() : 0);
        document.setActive(board.getActive());
        
        // 카테고리나 태그가 있다면 설정
        // document.setCategory(board.getCategory());
        // document.setTags(board.getTags());
        
        return document;
    }

    // Convert to search result
    public BoardSearchResult toSearchResult() {
        BoardSearchResult result = new BoardSearchResult();
        result.setId(this.originalId);
        result.setTitle(this.title);
        result.setContent(this.content);
        result.setAuthor(this.author);
        result.setCreatedAt(this.createdAt);
        result.setUpdatedAt(this.updatedAt);
        result.setViewCount(this.viewCount);
        result.setCategory(this.category);
        result.setTags(this.tags);
        return result;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Long getOriginalId() {
        return originalId;
    }

    public void setOriginalId(Long originalId) {
        this.originalId = originalId;
    }

    // Search result DTO
    public static class BoardSearchResult {
        private Long id;
        private String title;
        private String content;
        private String author;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Integer viewCount;
        private String category;
        private String tags;
        private Float score; // 검색 점수

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public Integer getViewCount() {
            return viewCount;
        }

        public void setViewCount(Integer viewCount) {
            this.viewCount = viewCount;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public Float getScore() {
            return score;
        }

        public void setScore(Float score) {
            this.score = score;
        }
    }
} 