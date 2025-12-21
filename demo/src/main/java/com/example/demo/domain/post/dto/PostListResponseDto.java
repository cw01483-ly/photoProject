package com.example.demo.domain.post.dto;


import lombok.Getter;

import java.time.LocalDateTime;

/*
    PostListResponseDto : 목록 조회 전용 DTO
        목록 조회 1쿼리로 게시글 + 작성자 정보 + 좋아요 수 까지 한번에 내려주기 위해 생성
*/
@Getter
public class PostListResponseDto {

    private final Long id; // posts PK
    private final Long displayNumber; // 게시글 번호
    private final String title;
    private final String content;
    private final int views;
    private final String authorName; // 작성자
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Long likeCount;

    public PostListResponseDto(
            Long id,
            Long displayNumber,
            String title,
            String content,
            int views,
            String authorName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long likeCount
    ){
        this.id = id;
        this.displayNumber = displayNumber;
        this.title = title;
        this.content = content;
        this.views = views;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.likeCount = likeCount;
    }
}
