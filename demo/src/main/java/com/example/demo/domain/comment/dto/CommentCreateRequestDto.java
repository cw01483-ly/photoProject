package com.example.demo.domain.comment.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 파라미터 없는 기본 생성자를 protected로 생성하여 외부에서 직접 생성을 막고, 프레임워크(JPA, Jackson)만 사용하게 함
@AllArgsConstructor
// 모든 필드를 파라미터로 받는 생성자를 자동 생성 (테스트나 빌더 내부에서 사용 가능)
@Builder
public class CommentCreateRequestDto {
    // 댓글 생성 시 클라이언트로부터 받을 데이터 담는 DTO

    @NotBlank(message = "댓글 내용은 공백일 수 없습니다.")
    @Size(max = 100, message = "댓글은 최대 100자까지 가능합니다.")
    private String content; //실제 클라이언트가 보내는 댓글 내용
}
