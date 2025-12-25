package com.example.demo.domain.post.dto;


import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.post.entity.Post;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

// 게시글 정보 + 최신 댓글 목록 + 전체 댓글 개수
public class PostDetailResponseDto {

    private Long id;
    private String title;
    private String content;
    private int views;
    private Long authorId; //User PK
    private String authorName; //User nickname
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 서비스 계층에서 최신 10개만 추려서 댓글목록을 담는 필드로 설계
    private List<CommentResponseDto> latestComments;

    // 전체 댓글 개수 표시
    private long totalCommentsCount;

    // 응답에서 내려준 최신 댓글 개수
    private int latestCommentsSize;

    // 해당 게시글 Like 개수
    private long likeCount;

    @Builder
    private PostDetailResponseDto(
            Long id,
            String title,
            String content,
            int views,
            Long authorId,
            String authorName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<CommentResponseDto> latestComments,
            long totalCommentsCount,
            int latestCommentsSize,
            long likeCount
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.views = views;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.latestComments = latestComments;
        this.totalCommentsCount = totalCommentsCount;
        this.latestCommentsSize = latestCommentsSize;
        this.likeCount = likeCount;
    }



    /*      정적 팩토리 메서드 (기존 버전) -> 기존 호출부 깨지 않도록 기존 버전 유지 & 간단 호출버전
         - Post 엔티티와 댓글 응답DTO 목록(List<CommentResponseDto>)을 받아
            PostDetailResponseDto 로 변환해주는 메서드
         - 좋아요 개수가 필요 없는 경우 기본값 0으로 세팅하여 사용
     */
    public static PostDetailResponseDto from( // 기존 호출부 보호용(likeCount X)
            Post post,
            List<CommentResponseDto> latestComments, //최신 댓글 목록 (최대 10)
            long totalCommentsCount, //전체 댓글 개수
            int latestCommentsSize
    ){
        // likeCount 를 아직 사용하지 않는 호출을 위해 기본값 0으로 위임
        return from(post, latestComments, totalCommentsCount, latestCommentsSize, 0L);
    }

    /*      정적 팩토리 메서드 (likeCount 포함 확장 버전)
        - Post 엔티티 + 최신 댓글 목록 + 전체 댓글 개수 + 응답에 포함할 최신 댓글 수 + 좋아요 개수
          를 받아서 PostDetailResponseDto 를 생성
    */
    public static PostDetailResponseDto from(
            Post post,
            List<CommentResponseDto> latestComments, //최신 댓글 목록 (최대 10)
            long totalCommentsCount, //전체 댓글 개수
            int latestCommentsSize, // 응답에 포함된 최신 댓글 개수
            long likeCount // 게시글 좋아요 개수
    ){
        // null 방어: latestComments 가 null 인 상황을 대비하여 빈 리스트로 처리할 수도 있음
        // (현재는 서비스 계층에서 알아서 List를 넘겨준다고 가정하고 그대로 사용)
        return PostDetailResponseDto.builder()
                .id(post.getId()) // Post 엔티티의 id값을 DTO id필드에 설정
                .title(post.getTitle())// Post의title > DTO의 title필드에 설정
                .content(post.getContent())
                .views(post.getViews())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .latestComments(latestComments)           // 최신 댓글 목록 설정
                .totalCommentsCount(totalCommentsCount)   // 전체 댓글 개수 설정
                .latestCommentsSize(latestCommentsSize)   // 이번 응답에 포함된 댓글 개수 설정
                .likeCount(likeCount)                    // 게시글 좋아요 개수 설정
                .build();
        // 빌더에 채워진 값들을 사용해서 최종 PostDetailResponseDto 객체를 생성하여 반환
    }
}
