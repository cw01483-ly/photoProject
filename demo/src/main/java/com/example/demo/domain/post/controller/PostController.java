package com.example.demo.domain.post.controller;
import com.example.demo.domain.post.dto.PostCreateRequestDto;
import com.example.demo.domain.post.dto.PostLikeCountResponseDto;
import com.example.demo.domain.post.dto.PostLikeToggleResponseDto;
import com.example.demo.domain.post.dto.PostResponseDto;
import com.example.demo.domain.post.service.PostLikeService;
import com.example.demo.domain.post.service.PostService;
import com.example.demo.global.response.ApiResponse;
import com.example.demo.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
// 클래스를 REST API용 컨트롤러로 지정 (메서드 리턴값을 JSON으로 응답)
@RequiredArgsConstructor
// final 필드를 매개변수로 받는 생성자를 롬복이 자동 생성해줌
// -> 스프링이 PostService, PostLikeService 를 자동 주입

@RequestMapping({"/posts","/api/posts"})
// ✅ [수정] 이 컨트롤러의 기본 URL 경로를 "/posts"와 "/api/posts" 둘 다로 열어둠
// 이유:
// - PostControllerTest가 "/posts"로 호출하고 있어서 404가 발생했음
// - 기존에 "/api/posts"를 쓰는 코드가 있을 수 있으니 호환성을 위해 둘 다 지원
// 예) GET /posts, POST /posts, GET /posts/{id} 등
// 예) GET /api/posts, POST /api/posts, GET /api/posts/{id} 등

public class PostController {

    private final PostService postService;
    // 생성자를 통해 주입받는 서비스 빈
    // 게시글 생성/조회/수정/삭제 등의 비즈니스 로직을 호출할 때 사용

    private final PostLikeService postLikeService;
    /* 게시글 좋아요 관련 비즈니스 로직 담당 서비스
        - Like토글, 개수 조회, 특정유저의 Like 여부 확인
    */

    // 1. 게시글 생성 API
    //    [POST] /posts?authorId=1&title=제목&content=내용
    @PostMapping
    // HTTP POST /posts 요청을 이 메서드가 처리하도록 매핑
    public ResponseEntity<ApiResponse<PostResponseDto>> createPost(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PostCreateRequestDto request
            ) {
        Long authorId = principal.getId();

        PostResponseDto responseDto = postService.createPost(
                authorId,
                request.getTitle(),
                request.getContent()
        );

        // 생성된 게시글 정보를 JSON 으로 응답
        // HTTP 상태코드 200 OK + Body에 PostResponseDto 담아서 반환
        return ResponseEntity.ok(
                ApiResponse.success(responseDto, "게시글 생성 완료")
                //  DTO를 ApiResponse로 감싸고 메시지를 함께 내려줌
        );
    }

    // 2. 게시글 단건 조회 + 조회수 증가
    //    [GET] /posts/{id}
    @GetMapping("/{id}")
    // HTTP GET /posts/{id} 요청을 이 메서드가 처리
    // 예: /posts/10 -> id = 10
    public ResponseEntity<ApiResponse<PostResponseDto>> getPostById(
            @PathVariable("id") Long postId
            // URL 경로의 {id} 값을 Long postId 파라미터에 매핑
    ) {

        // PostService의 getPostById 호출
        // 내부에서 조회수 증가 + 게시글 조회 + DTO 변환까지 수행
        PostResponseDto responseDto = postService.getPostById(postId);// 서비스에서 조회수 증가와 함께 단건 조회 수행

        // 조회된 게시글 정보를 200 OK와 함께 반환
        return ResponseEntity.ok( // HTTP 200 OK 응답 생성
                ApiResponse.success(responseDto, "게시글 단건 조회 성공")
                // ⭐ 조회 결과를 ApiResponse로 감싼 뒤 메시지 포함 반환
        );
    }

    // 3. 전체 게시글 최신순 조회 (페이징)
    //    [GET] /posts?page=0&size=10&sort=id,desc

    @GetMapping
    // HTTP GET /posts 요청을 이 메서드가 처리
    // 쿼리 파라미터로 page, size, sort를 자동으로 Pageable에 매핑
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> getPosts(Pageable pageable) {
        // 스프링이 page, size, sort 쿼리 파라미터를 분석해서 Pageable 객체를 자동 생성
        // 예: /posts?page=0&size=5&sort=id,desc

        // PostService의 getPosts 호출
        // Page<PostResponseDto> 형태로 페이징된 결과를 받음
        Page<PostResponseDto> responsePage = postService.getPosts(pageable);

        // 200 OK + 페이징된 게시글 목록 반환
        return ResponseEntity.ok( // HTTP 200 OK 응답 생성
                ApiResponse.success(responsePage, "게시글 목록 조회 성공"));
        // ⭐ 페이징 결과를 ApiResponse로 감싸서 반환
    }

    // 4. 작성자 기준 게시글 조회 (페이징)
    //    [GET] /posts/author/{authorId}?page=0&size=10
    @GetMapping("/author/{authorId}")
    // HTTP GET /posts/author/{authorId} 요청을 이 메서드가 처리
    // 예: /posts/author/3?page=0&size=5
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> getPostsByAuthor(//  반환 타입 변경
            @PathVariable Long authorId,
            // URL 경로에서 {authorId} 값을 Long authorId에 매핑

            Pageable pageable
            // page, size, sort 쿼리 파라미터를 담는 Pageable
    ) {
        // PostService의 getPostsByAuthor 호출
        Page<PostResponseDto> responsePage = postService.getPostsByAuthor(authorId, pageable);

        // 200 OK + 해당 작성자의 글 목록 반환
        return ResponseEntity.ok( // HTTP 200 OK 응답 생성
                ApiResponse.success(responsePage, "작성자별 게시글 목록 조회 성공")
                // 작성자별 게시글 목록을 ApiResponse로 감싸서 반환
        );
    }

    // 5. 제목 + 내용 키워드 검색 (대소문자 무시, 페이징)
    //    [GET] /posts/search?keyword=스프링&page=0&size=10

    @GetMapping("/search")
    // HTTP GET /posts/search 요청을 이 메서드가 처리
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> searchPosts( // 반환 타입 변경
            @RequestParam String keyword,
            // 요청 파라미터에서 검색어(keyword) 값을 가져옴
            // 예: /posts/search?keyword=스프링

            Pageable pageable
            // page, size, sort 정보 자동 주입
    ) {
        // PostService의 searchPosts 호출
        Page<PostResponseDto> responsePage = postService.searchPosts(keyword, pageable);
        // 서비스에서 키워드 기준 게시글 검색

        // 검색 결과(페이징)를 200 OK와 함께 반환
        return ResponseEntity.ok( // HTTP 200 OK 응답 생성
                ApiResponse.success(responsePage, "게시글 검색 성공")
                // 검색 결과를 ApiResponse로 감싸서 반환
        );
    }

    // 6. 게시글 수정
    //    [PUT] /posts/{id}?title=새제목&content=새내용

    @PutMapping("/{id}")
    // HTTP PUT /posts/{id} 요청을 이 메서드가 처리
    // 예: PUT /posts/5?title=변경&content=내용변경
    public ResponseEntity<ApiResponse<PostResponseDto>> updatePost(
            @PathVariable("id") Long postId,
            // URL 경로에서 {id} 값을 postId에 매핑

            @RequestParam String title,
            // 변경할 제목을 요청 파라미터에서 가져옴

            @RequestParam String content
            // 변경할 내용을 요청 파라미터에서 가져옴
    ) {
        // PostService의 updatePost 호출
        // 서비스 시그니처: updatePost(Long postId, String title, String content)
        PostResponseDto responseDto = postService.updatePost(postId, title, content);

        // 수정된 게시글 정보를 200 OK로 반환
        return ResponseEntity.ok( // HTTP 200 OK 응답 생성
                ApiResponse.success(responseDto, "게시글 수정 성공")
                // 수정 결과를 ApiResponse로 감싼 뒤 반환
        );
    }

    // 7. 게시글 삭제 (Soft Delete)
    //    [DELETE] /posts/{id}

    @DeleteMapping("/{id}")
    // HTTP DELETE /posts/{id} 요청을 이 메서드가 처리
    // 예: DELETE /posts/10
    public ResponseEntity<ApiResponse<Void>> deletePost( // 반환 타입을 ApiResponse<Void>로 변경
            @PathVariable("id") Long postId
            // URL 경로의 {id}를 postId로 매핑
    ) {

        // PostService의 deletePost 호출
        // 내부에서 soft delete 처리(is_deleted=true) 수행
        postService.deletePost(postId);

        // 응답 바디 없이 204 No Content 반환
        // "요청은 성공했지만 돌려줄 데이터는 없다"는 의미
        // → ApiResponse를 사용해 일관된 성공 응답 구조를 유지
        return ResponseEntity.ok( // 삭제 결과를 메시지와 함께 전달하기 위해 200 OK 사용
                ApiResponse.success("게시글 삭제 성공") // 데이터는 없고, 성공 메시지만 담은 공통 응답 포맷 반환
        );
    }

    // 8. 게시글 LIKE 토글
    /*  [POST] /posts/{postId}/likes?userId=1
            - 이미 눌린 상태면 취소, 아직 안눌렸다면 추가
    */
    @PostMapping("/{postId}/likes") // HTTP POST /posts/{postId}/likes 요청시 처리 메서드
    public ResponseEntity<ApiResponse<PostLikeToggleResponseDto>> togglePostLike(
            // 반환 타입을 ApiResponse<PostLikeToggleResponseDto>로 변경
            @PathVariable Long postId, //URL에서 postId 값 가져오기
            @RequestParam Long userId  //쿼리 파라미터로 userId 전달받음
    ){
        // 1) 좋아요 토글 수행 ( true : 누른상태 , false : 취소 상태 )
        boolean liked = postLikeService.toggleLike(postId,userId);

        // 2) 현재 게시글의 전체 LIKE 수 조회
        long likeCount = postLikeService.getLikeCount(postId);

        // 3) 응답 DTO 구성
        PostLikeToggleResponseDto responseDto = PostLikeToggleResponseDto.builder()
                .postId(postId)
                .userId(userId)
                .liked(liked)
                .likeCount(likeCount)
                .build();

        // 4) 200 ok + 응답DTO 반환
        return ResponseEntity.ok( // HTTP 200 OK 응답 생성
                ApiResponse.success(responseDto, "게시글 좋아요 토글 성공")
                // 좋아요 토글 결과를 ApiResponse로 감싸 반환
        );
    }

    // 9. 게시글 좋아요 개수 조회     [GET] /posts/{postId}/likes/count
    @GetMapping("/{postId}/likes/count")// HTTP GET /posts/{postId}/likes/count 요청시 처리 메서드
    public ResponseEntity<ApiResponse<PostLikeCountResponseDto>> getPostLikeCount(
            // 반환 타입을 ApiResponse<PostLikeCountResponseDto>로 변경
            @PathVariable Long postId //URL에서 postId 값 가져오기
    ){
        // 1) 해당 게시글 전체 좋아요 개수 조회
        long likeCount = postLikeService.getLikeCount(postId);

        // 2) 응답 DTO
        PostLikeCountResponseDto responseDto = PostLikeCountResponseDto.builder()
                .postId(postId)
                .likeCount(likeCount)
                .build();// 게시글 ID와 좋아요 개수를 담은 DTO 생성

        // 3) 200 ok + 응답 DTO 반환
        return ResponseEntity.ok( // HTTP 200 OK 응답 생성
                ApiResponse.success(responseDto, "게시글 좋아요 개수 조회 성공"));
                // ⭐ 좋아요 개수 DTO를 ApiResponse로 감싸서 반환
        }

    /* 10. 테스트용 에러 발생 API (임시)
             [GET] /posts/test/error
             - 항상 IllegalArgumentException 을 발생시켜
             GlobalExceptionHandler 가 제대로 동작하는지 확인하는 용도
    @GetMapping("/test/error")
    public void testError() {
        // 이 예외는 GlobalExceptionHandler 의
        // handleIllegalArgumentException 메서드에서 처리됨
        throw new IllegalArgumentException("테스트용 에러입니다.");
    }*/
}
