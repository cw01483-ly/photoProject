package com.example.demo.domain.ui.controller;

import com.example.demo.domain.comment.service.CommentService;
import com.example.demo.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
@RequestMapping("/ui/posts")
public class UiCommentController { // 댓글 UI 요청을 처리하는 컨트롤러

    private final CommentService commentService; // 댓글 생성 로직 재사용(서비스 호출)

    @PostMapping("/{postId}/comments") // POST /ui/posts/{postId}/comments (UI 폼 제출 엔드포인트)
    public String createCommentFromUi( // UI에서 댓글 작성 요청 처리
                                       @PathVariable("postId") Long postId, // 게시글 ID
                                       @AuthenticationPrincipal CustomUserDetails principal, //로그인 사용자(인증 주체)
                                       @RequestParam("content") String content
    ) {

        // 🔥 (중요) CommentService는 CustomUserDetails를 받아 내부에서 userId를 꺼내도록 설계되어 있음
        commentService.createComment(postId, principal, content); // 댓글 저장

        return "redirect:/ui/posts/" + postId; // 저장 후 게시글 상세 페이지로 이동
    }
}
