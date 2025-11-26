package com.example.demo.domain.post.dto;


import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.post.entity.Post;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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



    /*      정적 팩토리 메서드
        - Post 엔티티와 댓글 응답DTO 목록(List<CommentResponseDto>)을 받아
           PostDetailResponseDto 로 변환해주는 메서드
        - 서비스 계층에서 변환 로직을 한 곳에 모아 코드 깔끔하게 만듦
    */
    public static PostDetailResponseDto from(
            Post post,
            List<CommentResponseDto> latestComments, //최신 댓글 목록 (최대 10)
            long totalCommentsCount, //전체 댓글 개수
            int latestCommentsSize
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
                .build();
        // 빌더에 채워진 값들을 사용해서 최종 PostDetailResponseDto 객체를 생성하여 반환
    }
}
