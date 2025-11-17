package com.example.demo.domain.post.controller;
import com.example.demo.domain.post.dto.PostResponseDto;
import com.example.demo.domain.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
// 클래스를 REST API용 컨트롤러로 지정 (메서드 리턴값을 JSON으로 응답)
@RequiredArgsConstructor
// final 필드를 매개변수로 받는 생성자를 롬복이 자동 생성해줌
// -> 스프링이 PostService를 자동 주입해 줄 수 있음

@RequestMapping("/posts")
// 이 컨트롤러의 기본 URL 경로를 "/posts"로 설정
// 예) GET /posts, POST /posts, GET /posts/{id} 등

public class PostController {

    private final PostService postService;
    // 생성자를 통해 주입받는 서비스 빈
    // 게시글 생성/조회/수정/삭제 등의 비즈니스 로직을 호출할 때 사용

    // 1. 게시글 생성 API
    //    [POST] /posts?authorId=1&title=제목&content=내용

    @PostMapping
    // HTTP POST /posts 요청을 이 메서드가 처리하도록 매핑

    public ResponseEntity<PostResponseDto> createPost(
            @RequestParam Long authorId,
            // 쿼리스트링 또는 폼데이터에서 authorId 값을 가져옴
            // 예: /posts?authorId=1&title=...&content=...

            @RequestParam String title,
            // 요청 파라미터에서 title 값을 가져옴

            @RequestParam String content
            // 요청 파라미터에서 content 값을 가져옴
    ) {

        // PostService의 createPost 호출
        // 서비스 시그니처: createPost(Long authorId, String title, String content)
        PostResponseDto responseDto = postService.createPost(authorId, title, content);

        // 생성된 게시글 정보를 JSON 으로 응답
        // HTTP 상태코드 200 OK + Body에 PostResponseDto 담아서 반환
        return ResponseEntity.ok(responseDto);
    }

    // 2. 게시글 단건 조회 + 조회수 증가
    //    [GET] /posts/{id}
    @GetMapping("/{id}")
    // HTTP GET /posts/{id} 요청을 이 메서드가 처리
    // 예: /posts/10 -> id = 10

    public ResponseEntity<PostResponseDto> getPostById(
            @PathVariable("id") Long postId
            // URL 경로의 {id} 값을 Long postId 파라미터에 매핑
    ) {

        // PostService의 getPostById 호출
        // 내부에서 조회수 증가 + 게시글 조회 + DTO 변환까지 수행
        PostResponseDto responseDto = postService.getPostById(postId);

        // 조회된 게시글 정보를 200 OK와 함께 반환
        return ResponseEntity.ok(responseDto);
    }

    // 3. 전체 게시글 최신순 조회 (페이징)
    //    [GET] /posts?page=0&size=10&sort=id,desc

    @GetMapping
    // HTTP GET /posts 요청을 이 메서드가 처리
    // 쿼리 파라미터로 page, size, sort를 자동으로 Pageable에 매핑

    public ResponseEntity<Page<PostResponseDto>> getPosts(Pageable pageable) {
        // 스프링이 page, size, sort 쿼리 파라미터를 분석해서 Pageable 객체를 자동 생성
        // 예: /posts?page=0&size=5&sort=id,desc

        // PostService의 getPosts 호출
        // Page<PostResponseDto> 형태로 페이징된 결과를 받음
        Page<PostResponseDto> responsePage = postService.getPosts(pageable);

        // 200 OK + 페이징된 게시글 목록 반환
        return ResponseEntity.ok(responsePage);
    }

    // 4. 작성자 기준 게시글 조회 (페이징)
    //    [GET] /posts/author/{authorId}?page=0&size=10
    @GetMapping("/author/{authorId}")
    // HTTP GET /posts/author/{authorId} 요청을 이 메서드가 처리
    // 예: /posts/author/3?page=0&size=5

    public ResponseEntity<Page<PostResponseDto>> getPostsByAuthor(
            @PathVariable Long authorId,
            // URL 경로에서 {authorId} 값을 Long authorId에 매핑

            Pageable pageable
            // page, size, sort 쿼리 파라미터를 담는 Pageable
    ) {

        // PostService의 getPostsByAuthor 호출
        Page<PostResponseDto> responsePage = postService.getPostsByAuthor(authorId, pageable);

        // 200 OK + 해당 작성자의 글 목록 반환
        return ResponseEntity.ok(responsePage);
    }

    // 5. 제목 + 내용 키워드 검색 (대소문자 무시, 페이징)
    //    [GET] /posts/search?keyword=스프링&page=0&size=10

    @GetMapping("/search")
    // HTTP GET /posts/search 요청을 이 메서드가 처리

    public ResponseEntity<Page<PostResponseDto>> searchPosts(
            @RequestParam String keyword,
            // 요청 파라미터에서 검색어(keyword) 값을 가져옴
            // 예: /posts/search?keyword=스프링

            Pageable pageable
            // page, size, sort 정보 자동 주입
    ) {

        // PostService의 searchPosts 호출
        Page<PostResponseDto> responsePage = postService.searchPosts(keyword, pageable);

        // 검색 결과(페이징)를 200 OK와 함께 반환
        return ResponseEntity.ok(responsePage);
    }

    // 6. 게시글 수정
    //    [PUT] /posts/{id}?title=새제목&content=새내용

    @PutMapping("/{id}")
    // HTTP PUT /posts/{id} 요청을 이 메서드가 처리
    // 예: PUT /posts/5?title=변경&content=내용변경

    public ResponseEntity<PostResponseDto> updatePost(
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
        return ResponseEntity.ok(responseDto);
    }

    // 7. 게시글 삭제 (Soft Delete)
    //    [DELETE] /posts/{id}

    @DeleteMapping("/{id}")
    // HTTP DELETE /posts/{id} 요청을 이 메서드가 처리
    // 예: DELETE /posts/10

    public ResponseEntity<Void> deletePost(
            @PathVariable("id") Long postId
            // URL 경로의 {id}를 postId로 매핑
    ) {

        // PostService의 deletePost 호출
        // 내부에서 soft delete 처리(is_deleted=true) 수행
        postService.deletePost(postId);

        // 응답 바디 없이 204 No Content 반환
        // "요청은 성공했지만 돌려줄 데이터는 없다"는 의미
        return ResponseEntity.noContent().build();
    }
}
