package com.example.demo.domain.comment.dto;

import com.example.demo.domain.comment.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;

/*
    CommentAdminResponseDto
    - 관리자 댓글 목록 조회 전용 응답 DTO
    - nickname은 중복 가능하므로 관리자 식별 정보는 authorId 중심으로 제공
    - SoftDelete User(@Where)로 인해 author 엔티티 로딩이 실패할 수 있으므로,
      nickname 등 User 필드 접근은 하지 않는다.
*/
@Getter
@AllArgsConstructor
public class CommentAdminResponseDto {

    private Long commentId; // 댓글 ID
    private Long postId;    // 게시글 ID
    private Long authorId;  // 작성자 ID (관리자 식별 목적)
    private String content; // 댓글 내용

    public static CommentAdminResponseDto from(Comment comment) {
        Long authorId = null;

        /*
            author는 LAZY 프록시일 수 있으며,
            SoftDelete User(@Where)로 인해 실제 로딩 시 실패할 수 있음
            다만 보통 프록시의 getId()는 초기화 없이도 접근 가능,
            관리자 식별 목적의 authorId만 취한다.
         */
        if (comment.getAuthor() != null) {
            authorId = comment.getAuthor().getId();
        }

        return new CommentAdminResponseDto(
                comment.getId(),
                comment.getPost() != null ? comment.getPost().getId() : null,
                authorId,
                comment.getContent()
        );
    }
}
