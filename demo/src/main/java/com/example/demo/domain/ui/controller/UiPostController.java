package com.example.demo.domain.ui.controller; // UI(Thymeleaf) 전용 컨트롤러 패키지

import com.example.demo.domain.post.dto.PostDetailResponseDto;
import com.example.demo.domain.post.dto.PostListResponseDto;
import com.example.demo.domain.post.dto.PostResponseDto;
import com.example.demo.domain.post.service.PostLikeService;
import com.example.demo.domain.post.service.PostService;
import com.example.demo.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;


import java.security.Principal;

@RequiredArgsConstructor
@Controller // REST(JSON) 응답이 아니라 "HTML 뷰"를 반환하는 컨트롤러
@RequestMapping("/ui/posts")
public class UiPostController { // Posts(게시글) UI 화면 라우팅 담당 컨트롤러

    private final PostService postService;
    private final PostLikeService postLikeService;

    @GetMapping // GET /ui/posts
    public String listPage(  // 게시글 목록 화면
        @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)Pageable pageable,
        Model model
        ){ // 게시글 목록 화면
            Page<PostListResponseDto> page = postService.getPosts(pageable); // 실 게시글 목록

            long totalElements = page.getTotalElements(); // 화면용 시작 번호 (역순 정렬)
            long startNumber =
                    totalElements - (long) page.getNumber() * page.getSize();

            model.addAttribute("page", page); // 페이지 정보
            model.addAttribute("posts", page.getContent()); // 화면에서 th:each로 돌릴 실제 목록
            model.addAttribute("startNumber", startNumber);
            return "pages/posts/list"; // templates/pages/posts/list.html 로 이동
        }


    @GetMapping("/write") // GET /ui/posts/write
    public String writePage(Principal principal) { // 게시글 작성 화면
        // 비로그인 시 로그인 화면으로
        if (principal == null) {
            return "redirect:/ui/auth/login?next=/ui/posts/write";
        }
        // 로그인 시 글쓰기 화면
        return "pages/posts/write"; // templates/pages/posts/write.html 로 이동
    }

    @PostMapping
    public String createPostFromUi(
            @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("title") String title, // form input name="title"
            @RequestParam("content") String content, // textarea name="content"
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        Long authorId = principal.getId(); // 로그인 사용자 ID

        PostResponseDto created = postService.createPost(authorId, title, content, image); // API와 동일 서비스 호출

        return "redirect:/ui/posts/" + created.getId(); // 생성 후 상세로 이동
    }

    @GetMapping("/{id}") // GET /ui/posts/{id}
    public String detailPage(
            @PathVariable("id") Long id, // URL의 {id} 값을 Long으로 받음
            Model model, // 화면에 데이터 전달을 위해 Model 사용
            @AuthenticationPrincipal CustomUserDetails principal
            ) { // 게시글 상세 화면
        PostDetailResponseDto post = postService.getPostDetailWithViewIncrease(id); // 상세 데이터 바인딩
        model.addAttribute("post", post);
        model.addAttribute("postId", id); // 화면에서 사용할 수 있도록 postId라는 이름으로 전달

        // 로그인 사용자 id (비로그인 = null)
        Long viewerId = (principal != null) ? principal.getId() : null;
        model.addAttribute("viewerId", viewerId);
        return "pages/posts/detail"; // templates/pages/posts/detail.html 로 이동
    }


    // GET /ui/posts/{id}/refresh
    // 좋아요 토글 후 재진입 전용 (조회수 증가 없음)
    @GetMapping("/{id}/refresh")
    public String detailPageWithoutViewIncrease(
            @PathVariable("id") Long id,
            Model model,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        // 조회수 증가 없는 상세 조회
        PostDetailResponseDto post = postService.getPostDetail(id);

        model.addAttribute("post", post);
        model.addAttribute("postId", id);

        Long viewerId = (principal != null) ? principal.getId() : null;
        model.addAttribute("viewerId", viewerId);

        return "pages/posts/detail";
    }


    @GetMapping("/{id}/edit") // GET /ui/posts/{id}/edit
    public String editFormPage(
            @PathVariable("id") Long id, // 수정할 게시글 id
            Model model, // 화면에 데이터 전달
            @AuthenticationPrincipal CustomUserDetails principal
            ) { // 게시글 수정 폼 화면 (form.html 사용)
        // 1) 비로그인 차단
        if (principal == null) {
            return "redirect:/ui/auth/login?next=/ui/posts/" + id + "/edit";
        }
        // 2) 다른 유저 수정 차단 (authorId 비교)
        PostDetailResponseDto post = postService.getPostDetail(id);
        if (!post.getAuthorId().equals(principal.getId())) {
            return "redirect:/error/403";
        }
        model.addAttribute("postId", id); // 화면에서 id 기반으로 기존 데이터 조회/표시할 수 있게 전달
        return "pages/posts/form"; // templates/pages/posts/form.html 로 이동
    }

    @PostMapping("/{id}/edit") // POST /ui/posts/{id}/edit (수정 폼 제출)
    public String updatePostFromUi(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        // 1) 비로그인 차단
        if (principal == null) {
            return "redirect:/ui/auth/login?next=/ui/posts/" + id + "/edit";
        }

        // 2) 서버에서 작성자 검증 (DTO의 authorId 사용)
        PostDetailResponseDto post = postService.getPostDetail(id);
        if (!post.getAuthorId().equals(principal.getId())) {
            return "redirect:/error/403";
        }

        // 3) 실제 수정 처리 (PostService 시그니처에 정확히 맞춤)
        postService.updatePost(id, principal.getId(), title, content , image);

        // 4) 수정 완료 후 상세로 이동
        return "redirect:/ui/posts/" + id;
    }

    // 게시글 삭제: POST /ui/posts/{id}/delete
    @PostMapping("/{id}/delete")
    public String deletePostFromUi(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        // 1) 비로그인 차단
        if (principal == null) {
            return "redirect:/ui/auth/login?next=/ui/posts/" + id;
        }

        // 2) 작성자 검증 (서버에서도 다시 검사)
        PostDetailResponseDto post = postService.getPostDetail(id);
        if (!post.getAuthorId().equals(principal.getId())) {
            return "error/403";
        }

        // 3) 삭제 처리
        postService.deletePost(id, principal.getId());

        // 4) 목록으로 이동
        return "redirect:/ui/posts";
    }


    // POST /ui/posts/{id}/likes  (UI에서 좋아요 토글)
    @PostMapping("/{id}/likes")
    public String toggleLikeFromUi(
            @PathVariable("id") Long postId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        // 1) 비로그인 차단 (UI 패턴과 동일)
        if (principal == null) {
            return "redirect:/ui/auth/login?next=/ui/posts/" + postId;
        }

        // 2) 좋아요 토글 (서비스의 기존 토글 로직 재사용)
        postLikeService.toggleLike(postId, principal.getId());

        // 3) 다시 상세로 리다이렉트 (PRG)
        return "redirect:/ui/posts/" + postId + "/refresh";
    }

}
