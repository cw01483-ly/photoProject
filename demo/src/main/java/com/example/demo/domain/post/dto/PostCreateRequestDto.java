package com.example.demo.domain.post.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    PostCreateRequestDto
    - 게시글 생성 요청 바디 DTO
    - Controller는 @RequestBody DTO만 받도록 설계
    - authorId는 받지 않고(SecurityContext에서만 꺼냄) title/content만 받는다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequestDto {
    @NotBlank(message = "제목은 공백일 수 없습니다..")
    private String title;

    @NotBlank(message = "내용은 공백일 수 없습니다.")
    private String content;
}
