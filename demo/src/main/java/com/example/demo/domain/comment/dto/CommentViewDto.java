package com.example.demo.domain.comment.dto;

/*
    CommentViewDto (Marker Interface)
    [역할]
    - 댓글 API에서 "USER/ADMIN 응답 DTO가 달라도"
      컨트롤러의 반환 타입(ApiResponse 제네릭)을 하나로 고정하기 위한 공통 타입.

    - USER/비회원 응답 DTO: CommentResponseDto
    - ADMIN 응답 DTO: CommentAdminResponseDto
*/
public interface CommentViewDto {

}
