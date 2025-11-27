package com.example.demo.domain.post.dto;

import lombok.Builder;
import lombok.Getter;

/*
    PostLikeToggleResponseDto
    - 좋아요 토글 API 호출 결과를 클라이언트에게 내려줄 때 사용하는 응답 DTO
    - 필드 설명
        postId    : 대상 게시글 ID
        userId    : 요청을 보낸 사용자 ID
        liked     : 현재 상태 (true = 좋아요 눌러진 상태, false = 좋아요 취소된 상태)
        likeCount : 해당 게시글의 전체 좋아요 개수
 */
@Getter
@Builder 
public class PostLikeToggleResponseDto {

    private final Long postId;   // 좋아요 대상 게시글 ID
    private final Long userId;   // 요청을 보낸 사용자 ID
    private final boolean liked; // 현재 좋아요 상태 (true: 좋아요, false: 취소)
    private final long likeCount; // 현재 게시글의 전체 좋아요 개수

}
