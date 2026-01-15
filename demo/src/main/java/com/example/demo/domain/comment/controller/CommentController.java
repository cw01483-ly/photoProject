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
import com.example.demo.domain.comment.dto.CommentAdminResponseDto;
import com.example.demo.domain.comment.dto.CommentViewDto;

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
        - 로그인한 유저 정보는 @AuthenticationPrincipal로 주입받은 CustomUserDetails에서 획득
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
    public ResponseEntity<ApiResponse<List<CommentViewDto>>> getCommentsByPost( // 관리자/일반 응답 DTO 변경가능성에 따른 와일드카드 사용
            @PathVariable Long postId,   // URL 경로에서 게시글 ID 추출
            @AuthenticationPrincipal CustomUserDetails userDetails // 관리자 여부 판별을 위해 주입
    ){
        // ADMIN 여부 판별 (userDetails가 null이면 비회원/익명 요청)
        boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // ADMIN은 엔티티 조회 후 Admin DTO로 변환
        if (isAdmin) {
            List<Comment> comments = commentService.getCommentsByPostForAdmin(postId);
            List<CommentViewDto> responseList = comments.stream()
                    .map(CommentAdminResponseDto::from)
                    .map(dto -> (CommentViewDto) dto)
                    .toList();

            return ResponseEntity.ok(
                    ApiResponse.success(responseList, "댓글 목록 조회 성공")
            );
        }
        //  USER/비회원은 엔티티 조회 후 User DTO로 변환
        List<Comment> comments = commentService.getCommentsByPostForUser(postId);
        List<CommentViewDto> responseList = comments.stream()
                .map(CommentResponseDto::from)
                .map(dto -> (CommentViewDto) dto)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(responseList, "댓글 목록 조회 성공")
        );
    }

    /* 특정 게시글 댓글 목록 페이징 조회
        - HTTP GET /api/posts/{postId}/comments/paging
        - 요청 파라미터 :
            page : 조회할 페이지 번호 (프론트1시작, 백엔드는0으로 맞춰줄것)
            size : 한 페이지에 몇 개의 댓글 가져올 지( 기본 10 )
        - 반환 : Page<CommentViewDto>
                (댓글 데이터 + 페이징 메타정보 포함)
    */
    @GetMapping("/posts/{postId}/comments/paging") //게시글 기준 댓글페이징 조회
    public ResponseEntity<ApiResponse<Page<CommentViewDto>>> getCommentsByPostWithPaging(
            @PathVariable Long postId,// URL 경로에서 게시글 ID 추출
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        // 0) 스프링에게 페이지 초기값 0으로 변환
        int zeroBasedPage = Math.max(0, page - 1);
        //프론트가 실수로 0보냈을 때 방지, 0이나 음수면 자동으로 0페이지로 고정
        // 1) ADMIN 판별
        boolean isAdmin = userDetails != null &&
                userDetails.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        // 2) 서비스 계층 호출 USER/ADMIN 분기, DTO 선택은 Controller
        Page<Comment> commentPage =
                commentService.getCommentsByPostWithPaging(
                        postId, zeroBasedPage, size);
        // 3) 엔티티(Page<Comment>) → DTO(Page<CommentViewDto>) 변환
        Page<CommentViewDto> responsePage = commentPage.map(comment -> {
            if (isAdmin) {
                // 관리자: User JOIN / nickname 접근 없는 Admin DTO
                return (CommentViewDto) CommentAdminResponseDto.from(comment);
            }
            // 일반 사용자 UI 표시용 DTO
            return (CommentViewDto) CommentResponseDto.from(comment);
        });
        // 4) 공통 ApiResponse로 감싸서 반환
        // 데이터 : Page<CommentViewDto>, 메시지 : 조회 성공 메시지
        return ResponseEntity.ok(
                ApiResponse.success(responsePage, "댓글 페이징 조회 성공"));
        }

    /*
        댓글 단건 조회
        - HTTP GET /api/comments/{commentId}
        - 반환: CommentResponseDto
     */
    @GetMapping("/comments/{commentId}") // 댓글 단건 조회 엔드포인트
    public ResponseEntity<ApiResponse<CommentViewDto>> getComment(
            @PathVariable Long commentId,  // URL 경로에서 댓글 ID 추출
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        /*
        - ADMIN : SoftDelete User여도 조회 가능 >> User JOIN 불필요
        - USER  : SoftDelete User 댓글은 숨김 >> author JOIN FETCH + 없으면 404
         */
        // ADMIN 판별
        boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // 2) ADMIN 경로
        if (isAdmin) {
            // ADMIN은 nickname 접근이 없으므로 일반 엔티티 조회로 충분
            Comment comment = commentService.getCommentEntity(commentId);
            CommentViewDto dto = CommentAdminResponseDto.from(comment);

            return ResponseEntity.ok(
                    ApiResponse.success(dto, "댓글 단건 조회 성공")
            );
        }

        // 3) USER 경로
        /*
        USER는 nickname(author) 접근이 필요, author JOIN FETCH 전용 메서드 사용
        - SoftDelete User면 JOIN 결과 자체가 없어져 null
        - 이 경우 404로 처리됨 (Service에서 예외 발생)
        */
        Comment comment = commentService.getCommentForUser(commentId);
        CommentViewDto dto = CommentResponseDto.from(comment);

        return ResponseEntity.ok(
                ApiResponse.success(dto, "댓글 단건 조회 성공")
        );
    }


    /*
        댓글 수정
        - HTTP PATCH /api/comments/{commentId}
        - 요청 본문: CommentUpdateRequestDto (수정할 content)
        - 작성자 본인 또는 ADMIN 여부 검증은 CommentService.updateComment 내부에서 수행됨
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
