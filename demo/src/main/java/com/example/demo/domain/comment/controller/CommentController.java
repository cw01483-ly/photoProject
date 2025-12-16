package com.example.demo.domain.comment.controller;

import com.example.demo.domain.comment.dto.CommentCreateRequestDto;
import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.dto.CommentUpdateRequestDto;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.service.CommentService;
import com.example.demo.global.response.ApiResponse;
import com.example.demo.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    @PostMapping("/posts/{postId}/comments")// 특정 게시글에 댓글 생성 요청을 받는 엔드포인트
    public ResponseEntity<ApiResponse<CommentResponseDto>> createComment(
            @PathVariable Long postId,// URL 경로에서 게시글 ID 추출
            @Valid @RequestBody CommentCreateRequestDto request,// 요청 본문(JSON)을 DTO로 매핑 + 검증
            @AuthenticationPrincipal CustomUserDetails userDetails // JWT 인증된 사용자 전체 주입
            ){

        // 1) 서비스 계층을 통해 댓글 생성(엔티티 반환)
        Comment createdComment = commentService.createComment(postId,userDetails,request.getContent());
        // 2) 엔티티를 응답용 DTO로 변환
        CommentResponseDto responseDto = CommentResponseDto.from(createdComment);
        // 3) HTTP 201(CREATED) 상태 코드와 함께 응답 반환 (ApiResponse로 감싸서 반환)
        return ResponseEntity.status(HttpStatus.CREATED) // 201 Created 상태 코드 설정
                .body(ApiResponse.success(responseDto, "댓글 생성 성공"));
        // DTO를 ApiResponse.success로 감싸고 메시지를 포함하여 응답
    }


    /*
        특정 게시글에 달린 댓글 목록 조회
        - HTTP GET /api/posts/{postId}/comments
        - 반환: 해당 게시글에 달린 댓글 목록(List<CommentResponseDto>)
     */
    @GetMapping("/posts/{postId}/comments")   // 게시글 기준 댓글 목록 조회 엔드포인트
    public ResponseEntity<ApiResponse<List<CommentResponseDto>>> getCommentsByPost(
            // 반환 타입을 ApiResponse<List<CommentResponseDto>>로 변경
            @PathVariable Long postId   // URL 경로에서 게시글 ID 추출
    ){
        // 1) 서비스 계층에서 댓글 엔티티 목록 조회, 특정 게시글에 달린 모든 댓글 엔티티 목록을 조회
        List<Comment> comments = commentService.getCommentsByPost(postId);
        // 2) 엔티티 목록 > 응답 DTO 목록으로 반환, 엔티티 리스트를 스트림으로 변환
        List<CommentResponseDto> responseList = comments.stream()
                .map(CommentResponseDto::from)// CommentResponseDto.from(comment) 을 각 요소에 적용
                .toList();// 변환된 DTO들을 리스트로 수집
        // 3) HTTP 200(OK) 상태 코드와 함께 댓글 목록 반환
        return ResponseEntity.ok( // 200 OK 상태 코드로 응답
                ApiResponse.success(responseList, "댓글 목록 조회 성공"));
        // 댓글 목록 DTO 리스트를 ApiResponse로 감싸서 반환
    }

    /* 특정 게시글 댓글 목록 페이징 조회
        - HTTP GET /api/posts/{postId}/comments/paging
        - 요청 파라미터 :
            page : 조회할 페이지 번호 (프론트1시작, 백엔드는0으로 맞춰줄것)
            size : 한 페이지에 몇 개의 댓글 가져올 지( 기본 10 )
        - 반환 : Page<CommentResponseDto>
                (댓글 데이터 + 페이징 메타정보 포함)
    */
    @GetMapping("/posts/{postId}/comments/paging") //게시글 기준 댓글페이징 조회
    public ResponseEntity<ApiResponse<Page<CommentResponseDto>>> getCommentsByPostWithPaging(
            // 반환 타입을 ApiResponse<Page<CommentResponseDto>>로 변경
            @PathVariable Long postId,// URL 경로에서 게시글 ID 추출
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        // 0) 스프링에게 페이지 초기값 0으로 변환
        int zeroBasedPage = Math.max(0, page - 1);
        //프론트가 실수로 0보냈을 때 방지, 0이나 음수면 자동으로 0페이지로 고정
        // 1) 서비스 계층에서 페이징 된 댓글 엔티티 페이지 조회
        Page<Comment> commentPage =
                commentService.getCommentsByPostWithPaging(
                postId, zeroBasedPage, size); //zeroBasedPage 사용
        /* 2) Page<Comment> -> Page<CommentResponseDto> 로 변환
            Page 타입에는 map(Function)을 제공해서 요소 타입만 바꾸고
            페이지 정보(전체 개수, 전체 페이지 수 등)는 그대로 유지
        */
        Page<CommentResponseDto> responsePage = commentPage
                .map(CommentResponseDto::from);
        // 3) HTTP 200(OK) 상태 코드와 함께 Page<CommentResponseDto> 반환
        return ResponseEntity.ok( // 200 OK 상태 코드로 응답
                ApiResponse.success(responsePage, "댓글 페이징 조회 성공"));
                //  페이징된 댓글 DTO 페이지를 ApiResponse로 감싸서 반환
        }

    /*
        댓글 단건 조회
        - HTTP GET /api/comments/{commentId}
        - 반환: CommentResponseDto
     */
    @GetMapping("/comments/{commentId}") // 댓글 단건 조회 엔드포인트
    public ResponseEntity<ApiResponse<CommentResponseDto>> getComment(
            // 반환 타입을 ApiResponse<CommentResponseDto>로 변경
            @PathVariable Long commentId  // URL 경로에서 댓글 ID 추출
    ) {

        // 1) 서비스 계층에서 댓글 엔티티 조회, ID로 댓글 엔티티 단건 조회
        Comment comment = commentService.getComment(commentId);

        // 2) 엔티티를 응답 DTO로 변환, Comment 엔티티를 CommentResponseDto로 변환
        CommentResponseDto responseDto = CommentResponseDto.from(comment);

        // 3) HTTP 200(OK) 상태 코드와 함께 반환
        return ResponseEntity.ok( // 200 OK 응답
                ApiResponse.success(responseDto, "댓글 단건 조회 성공"));
                // 단건 댓글 DTO를 ApiResponse로 감싸서 반환
    }

    /*
        댓글 수정
        - HTTP PATCH /api/comments/{commentId}
        - 요청 본문: CommentUpdateRequestDto (수정할 content)
        - 로그인한 사용자 정보는 Security에서 가져오지만,
          여기서는 "작성자 본인만 수정 가능" 같은 권한 체크는 서비스/추가 로직에서 처리할 수 있음
     */
    @PatchMapping("/comments/{commentId}") // 댓글 수정 요청을 받는 엔드포인트
    public ResponseEntity<ApiResponse<CommentResponseDto>> updateComment(
            @PathVariable Long commentId,                            // URL 경로에서 댓글 ID 추출
            @Valid @RequestBody CommentUpdateRequestDto request,     // 요청 본문(JSON)을 DTO로 매핑 + 검증
            @AuthenticationPrincipal CustomUserDetails userDetails  // JWT 인증된 사용자 전체 주입
    ) {

        // 1) 서비스 계층에서 댓글 내용 수정 (엔티티 반환)
        Comment updatedComment = commentService.updateComment(
                commentId,
                userDetails,
                request.getContent());// 서비스에 댓글 ID, 사용자 ID, 새 내용 전달 후 수정된 엔티티 반환
        /*
            현재 코드는 이미 CommentService.updateComment(commentId, userId, newContent) 형태로 구현되어 있으며,
            서비스 계층에서 userId와 comment.getAuthor().getId()를 비교하여
            "댓글 작성자 본인만 수정 가능"하도록 권한 검증을 수행
         */

        // 2) 수정된 엔티티를 응답 DTO로 변환, 수정된 엔티티를 CommentResponseDto로 변환
        CommentResponseDto responseDto = CommentResponseDto.from(updatedComment);

        // 3) HTTP 200(OK) 상태 코드와 함께 수정 결과 반환
        return ResponseEntity.ok( // 200 OK 응답
                ApiResponse.success(responseDto, "댓글 수정 성공")); //
        // 수정된 댓글 DTO를 ApiResponse로 감싸서 반환
    }

    /*
        댓글 삭제
        - HTTP DELETE /api/comments/{commentId}
        - 반환: HTTP 200(OK) + ApiResponse (본문에 "댓글 삭제 성공" 메시지)
        - 실제로는 @SQLDelete 덕분에 "논리 삭제"가 수행됨
     */
    @DeleteMapping("/comments/{commentId}")  // 댓글 삭제 요청을 받는 엔드포인트
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,                            // URL 경로에서 댓글 ID 추출
            @AuthenticationPrincipal CustomUserDetails userDetails  // JWT 인증된 사용자 전체 주입
    ) {
        // 1) 서비스 계층에 삭제 요청, 서비스에 댓글 ID와 로그인 유저 ID를 전달해 삭제(논리 삭제) 수행
        commentService.deleteComment(commentId,userDetails);
        // 2) HTTP 200 OK 상태 코드와 함께 성공 메시지 반환 (ApiResponse 사용)
        return ResponseEntity.ok( // 204 No Content 대신 200 OK + 메시지 반환으로 통일
                ApiResponse.success("댓글 삭제 성공"));
                // 데이터는 없고, 성공 메시지만 담은 ApiResponse 반환
    }
}
