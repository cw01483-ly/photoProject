package com.example.demo.domain.comment.controller;

import com.example.demo.domain.comment.dto.CommentCreateRequestDto;
import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.dto.CommentUpdateRequestDto;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController     // REST API 컨트롤러 (JSON 반환)
@RequestMapping("/api") // 공통 URL prefix 설정
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    // 댓글 비즈니스 로직을 처리하는 서비스 의존성


     /*      댓글 생성
        - HTTP POST /api/posts/{postId}/comments
        - 요청 본문: CommentCreateRequestDto (content만 포함)
        - 로그인한 유저의 ID는 SecurityContext에서 가져온다고 가정
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponseDto> createComment(// 특정 게시글에 댓글 생성 요청을 받는 엔드포인트
            @PathVariable Long postId,// URL 경로에서 게시글 ID 추출
            @Valid @RequestBody CommentCreateRequestDto request,// 요청 본문(JSON)을 DTO로 매핑 + 검증
            @AuthenticationPrincipal(expression = "id") Long userId// 로그인한 사용자 객체에서 id 필드만 주입받는다고 가정
            /*
                @AuthenticationPrincipal(expression = "id")
                - CustomUserDetails 같은 로그인 유저 객체에 getId() 메서드가 있다고 가정
                - 그 객체에서 id 값만 뽑아서 Long userId 파라미터로 주입
                - 실제 구현에 따라 expression 부분은 수정될 수 있음
             */
            ){
        // 1) 서비스 계층을 통해 댓글 생성(엔티티 반환)
        Comment createdComment = commentService.createComment(postId,userId,request.getContent());
        // 2) 엔티티를 응답용 DTO로 변환
        CommentResponseDto responseDto = CommentResponseDto.from(createdComment);
        // 3) HTTP 201(CREATED) 상태 코드와 함께 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }


    /*
        특정 게시글에 달린 댓글 목록 조회
        - HTTP GET /api/posts/{postId}/comments
        - 반환: 해당 게시글에 달린 댓글 목록(List<CommentResponseDto>)
     */
    @GetMapping("/posts/{postId}/comments")   // 게시글 기준 댓글 목록 조회 엔드포인트
    public ResponseEntity<List<CommentResponseDto>> getCommentsByPost(
            @PathVariable Long postId   // URL 경로에서 게시글 ID 추출
    ){
        // 1) 서비스 계층에서 댓글 엔티티 목록 조회
        List<Comment> comments = commentService.getCommentsByPost(postId);
        // 2) 엔티티 목록 > 응답 DTO 목록으로 반환
        List<CommentResponseDto> responseList = comments.stream()
                .map(CommentResponseDto::from)// CommentResponseDto.from(comment) 을 각 요소에 적용
                .toList();
        // 3) HTTP 200(OK) 상태 코드와 함께 댓글 목록 반환
        return ResponseEntity.ok(responseList);
    }


    /*
        댓글 단건 조회
        - HTTP GET /api/comments/{commentId}
        - 반환: CommentResponseDto
     */
    @GetMapping("/comments/{commentId}")                             // 댓글 단건 조회 엔드포인트
    public ResponseEntity<CommentResponseDto> getComment(
            @PathVariable Long commentId                             // URL 경로에서 댓글 ID 추출
    ) {

        // 1) 서비스 계층에서 댓글 엔티티 조회
        Comment comment = commentService.getComment(commentId);

        // 2) 엔티티를 응답 DTO로 변환
        CommentResponseDto responseDto = CommentResponseDto.from(comment);

        // 3) HTTP 200(OK) 상태 코드와 함께 반환
        return ResponseEntity.ok(responseDto);
    }

    /*
        댓글 수정
        - HTTP PATCH /api/comments/{commentId}
        - 요청 본문: CommentUpdateRequestDto (수정할 content)
        - 로그인한 사용자 정보는 Security에서 가져오지만,
          여기서는 "작성자 본인만 수정 가능" 같은 권한 체크는 서비스/추가 로직에서 처리할 수 있음
     */
    @PatchMapping("/comments/{commentId}") // 댓글 수정 요청을 받는 엔드포인트
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long commentId,                            // URL 경로에서 댓글 ID 추출
            @Valid @RequestBody CommentUpdateRequestDto request,     // 요청 본문(JSON)을 DTO로 매핑 + 검증
            @AuthenticationPrincipal(expression = "id") Long userId  // 로그인한 사용자 ID (권한 체크 시 사용 가능)
    ) {

        // 1) 서비스 계층에서 댓글 내용 수정 (엔티티 반환)
        Comment updatedComment = commentService.updateComment(commentId, request.getContent());
        /*
            ️현재 CommentService.updateComment(..)는 userId를 파라미터로 받지 않음
            - "작성자만 수정 가능" 같은 권한 체크를 추가하려면
              CommentService.updateComment(commentId, userId, newContent) 형태로 확장 가능
         */

        // 2) 수정된 엔티티를 응답 DTO로 변환
        CommentResponseDto responseDto = CommentResponseDto.from(updatedComment);

        // 3) HTTP 200(OK) 상태 코드와 함께 수정 결과 반환
        return ResponseEntity.ok(responseDto);
    }

    /*
        댓글 삭제
        - HTTP DELETE /api/comments/{commentId}
        - 반환: HTTP 204(NO_CONTENT) (응답 본문 없음)
        - 실제로는 @SQLDelete 덕분에 "논리 삭제"가 수행됨
     */
    @DeleteMapping("/comments/{commentId}")                          // 댓글 삭제 요청을 받는 엔드포인트
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,                            // URL 경로에서 댓글 ID 추출
            @AuthenticationPrincipal(expression = "id") Long userId  // 로그인 유저 ID (권한 체크용으로 사용 가능)
    ) {
        // 1) 서비스 계층에 삭제 요청
        commentService.deleteComment(commentId);
        /*
            ️"작성자만 삭제 가능" 같은 검증을 추가하려면
            CommentService.deleteComment(commentId, userId) 형태로 확장 가능
         */
        // 2) HTTP 204(NO_CONTENT) 상태 코드만 응답 (본문 없음)
        return ResponseEntity.noContent().build();
    }
}
