package com.example.demo.domain.post.dto;
import lombok.Builder;
import lombok.Getter;

/*
    PostLikeCountResponseDto
    - 특정 게시글의 좋아요 개수만 단독으로 조회할 때 사용하는 응답 DTO
    - 필드 설명
        postId    : 대상 게시글 ID
        likeCount : 해당 게시글에 달린 전체 좋아요 개수
 */
@Getter 
@Builder
public class PostLikeCountResponseDto {

    private final Long postId;    // 좋아요 개수를 조회한 게시글 ID
    private final long likeCount; // 해당 게시글의 전체 좋아요 개수

}
