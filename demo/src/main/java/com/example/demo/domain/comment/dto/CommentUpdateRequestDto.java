package com.example.demo.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// JPA, Jackson 등을 위한 기본 생성자를 protected로 생성하여 외부 직접 사용을 제한
@AllArgsConstructor
// 모든 필드를 파라미터로 받는 생성자를 자동 생성
@Builder
public class CommentUpdateRequestDto {
    // 댓글 수정시 클라이언트로 부터 받을 데이터 DTO

    @NotBlank(message = "댓글 내용은 공백일 수 없습니다.")
    @Size(max = 100, message = "댓글은 최대 100자까지 가능합니다.")
    private String content; // 수정할 댓글
}
