package com.example.boardstack.dto;

import com.example.boardstack.entity.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "게시글 등록/수정 요청 DTO")
public class BoardRequestDto {

    @Schema(description = "게시글 제목", example = "Spring Boot와 OpenStack 연동하기", required = true)
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    @Schema(description = "게시글 내용", example = "Spring Boot 애플리케이션에서 OpenStack API를 연동하는 방법에 대해 설명합니다.", required = true)
    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 5000, message = "내용은 5000자를 초과할 수 없습니다")
    private String content;

    @Schema(description = "작성자명", example = "admin", required = true)
    @NotBlank(message = "작성자는 필수입니다")
    @Size(max = 50, message = "작성자는 50자를 초과할 수 없습니다")
    private String writer;

    // DTO를 Entity로 변환
    public Board toEntity() {
        return Board.builder()
                .title(this.title)
                .content(this.content)
                .writer(this.writer)
                .viewCount(0L)
                .build();
    }
} 