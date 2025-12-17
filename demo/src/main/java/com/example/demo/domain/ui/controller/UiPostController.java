package com.example.demo.domain.ui.controller; // UI(Thymeleaf) 전용 컨트롤러 패키지

import com.example.demo.domain.post.dto.PostDetailResponseDto;
import com.example.demo.domain.post.dto.PostResponseDto;
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


import java.security.Principal;

@RequiredArgsConstructor
@Controller // REST(JSON) 응답이 아니라 "HTML 뷰"를 반환하는 컨트롤러
@RequestMapping("/ui/posts")
public class UiPostController { // Posts(게시글) UI 화면 라우팅 담당 컨트롤러

    private final PostService postService;

    @GetMapping // GET /ui/posts
    public String listPage(  // 게시글 목록 화면
        @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)Pageable pageable,
        Model model
        ){ // 게시글 목록 화면
            Page<PostResponseDto> page = postService.getPosts(pageable); // 실 게시글 목록

            model.addAttribute("page", page); // 페이지 정보
            model.addAttribute("posts", page.getContent()); // 화면에서 th:each로 돌릴 실제 목록
            return "pages/posts/list"; // templates/pages/posts/list.html 로 이동
        }


    @GetMapping("/write") // GET /ui/posts/write
    public String writePage(Principal principal) { // 게시글 작성 화면
        // 비로그인 시 로그인 화면으로
        if (principal == null) {
            return "redirect:/ui/auth/login?next=/ui/posts/write";
        }
        // 로그인 시 글씨기 화면
        return "pages/posts/write"; // templates/pages/posts/write.html 로 이동
    }

    @PostMapping
    public String createPostFromUi(
            @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("title") String title, // form input name="title"
            @RequestParam("content") String content // textarea name="content"
    ) {
        Long authorId = principal.getId(); // 로그인 사용자 ID

        PostResponseDto created = postService.createPost(authorId, title, content); // API와 동일 서비스 호출

        return "redirect:/ui/posts/" + created.getId(); // 생성 후 상세로 이동
    }

    @GetMapping("/{id}") // GET /ui/posts/{id}
    public String detailPage(
            @PathVariable("id") Long id, // URL의 {id} 값을 Long으로 받음
            Model model, // 화면에 데이터 전달을 위해 Model 사용
            @AuthenticationPrincipal CustomUserDetails principal
            ) { // 게시글 상세 화면
        PostDetailResponseDto post = postService.getPostDetail(id); // 상세 데이터 바인딩
        model.addAttribute("post", post);
        model.addAttribute("postId", id); // 화면에서 사용할 수 있도록 postId라는 이름으로 전달

        // 로그인 사용자 id (비로그인 = null)
        Long viewerId = (principal != null) ? principal.getId() : null;
        model.addAttribute("viewerId", viewerId);
        return "pages/posts/detail"; // templates/pages/posts/detail.html 로 이동
    }

    @GetMapping("/{id}/edit") // GET /ui/posts/{id}/edit
    public String editFormPage(
            @PathVariable("id") Long id, // 수정할 게시글 id
            Model model // 화면에 데이터 전달
    ) { // 게시글 수정 폼 화면 (form.html 사용)
        model.addAttribute("postId", id); // 화면에서 id 기반으로 기존 데이터 조회/표시할 수 있게 전달
        return "pages/posts/form"; // templates/pages/posts/form.html 로 이동
    }
}
