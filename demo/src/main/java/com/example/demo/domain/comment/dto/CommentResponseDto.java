package com.example.demo.domain.comment.dto;

import com.example.demo.domain.comment.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 기본 생성자를 protected로 생성하여 외부 직접 생성은 막고, 프레임워크 사용만 허용
@AllArgsConstructor
// 모든 필드를 파라미터로 받는 생성자를 자동 생성
@Builder
public class CommentResponseDto {
    // 클라이언트로 응답 내려줄 때 사용할 DTO

    private Long id; //댓글 PK
    private String content; //댓글 내용
    private Long postId; //게시글 PK
    private Long authorId; //댓글 작성자 ID
    private String authorName; //작성자 닉네임
    private LocalDateTime createdAt; //작성시간
    private LocalDateTime updatedAt; //수정 시간

    public static CommentResponseDto from(Comment comment){
        // Comment 엔티티를 CommentResponseDto로 변환하는 정적 메서드

        return CommentResponseDto.builder()
                .id(comment.getId())//엔티티 id값을 DTO id에 설정
                .content(comment.getContent())// 엔티티의 content 값을 DTO의 content에 설정
                .postId(comment.getPost().getId())// 엔티티가 참조하는 Post의 id를 DTO의 postId에 설정
                .authorId(comment.getAuthor().getId())// 엔티티가 참조하는 User(작성자)의 id를 DTO의 authorId에 설정
                .authorName(comment.getAuthor().getNickname())
                .createdAt(comment.getCreatedAt()) // BaseTimeEntity에서 상속받은 createdAt 값을 DTO에 설정
                .updatedAt(comment.getUpdatedAt())
                .build();
                // 빌더로 설정한 값들을 사용해 CommentResponseDto 객체를 생성하여 반환
    }

}
