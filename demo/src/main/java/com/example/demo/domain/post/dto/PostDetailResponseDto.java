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
// 게시글 상세 화면 전용 응답 DTO, 게시글 자체 정보 + 댓글 목록
public class PostDetailResponseDto {

    private Long id;
    private String title;
    private String content;
    private int views;
    private Long authorId; //User PK
    private String authorName; //User nickname
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponseDto> comments;
    // 게시글에 달린 댓글 목록, 각 요소는 CommentResponseDto로 변환된 댓글 정보

    /*      정적 팩토리 메서드
        - Post 엔티티와 댓글 응답DTO 목록(List<CommentResponseDto>)을 받아
           PostDetailResponseDto 로 변환해주는 메서드
        - 서비스 계층에서 변환 로직을 한 곳에 모아 코드 깔끔하게 만듦
    */
    public static PostDetailResponseDto from(Post post, List<CommentResponseDto> comments){
        // null 방어: comments 가 null 인 상황을 대비하여 빈 리스트로 처리할 수도 있음
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
                .comments(comments) // 서비스 계층에서 미리 CommentResponseDto 리스트로 변환한 댓글 목록을 그대로 설정
                .build();
        // 빌더에 채워진 값들을 사용해서 최종 PostDetailResponseDto 객체를 생성하여 반환
    }
}
