package com.example.demo.domain.ui.controller;

import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.service.CommentService;
import com.example.demo.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Controller
@RequestMapping("/ui")
public class UiCommentController { // 댓글 UI 요청을 처리하는 컨트롤러

    private final CommentService commentService; // 댓글 생성 로직 재사용(서비스 호출)

    // 댓글 작성
    @PostMapping("/posts/{postId}/comments") // POST /ui/posts/{postId}/comments (UI 폼 제출 엔드포인트)
    public String createCommentFromUi( // UI에서 댓글 작성 요청 처리
                                       @PathVariable("postId") Long postId, // 게시글 ID
                                       @AuthenticationPrincipal CustomUserDetails principal, //로그인 사용자(인증 주체)
                                       @RequestParam("content") String content
    ) {

        // CommentService는 CustomUserDetails를 받아 내부에서 userId를 꺼내도록 설계되어 있음
        commentService.createComment(postId, principal, content); // 댓글 저장

        return "redirect:/ui/posts/" + postId; // 저장 후 게시글 상세 페이지로 이동
    }

    // 댓글 수정  GET /ui/comments/{commentId}/edit?postId={postId}
    @GetMapping("/comments/{commentId}/edit")
    public String editCommentPage(
            @PathVariable("commentId") Long commentId,
            @RequestParam("postId") Long postId, // redirect를 위해 postId를 같이 받음
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model
    ) {
        // 1) 비로그인 차단
        if (principal == null) {
            return "redirect:/ui/auth/login?next=/ui/posts/" + postId;
        }

        // 2) 댓글 조회(내용을 edit 폼에 채우기 위함)
        Comment comment = commentService.getCommentEntity(commentId);

        model.addAttribute("comment", comment);
        model.addAttribute("commentId", commentId);
        model.addAttribute("postId", postId);
        return "pages/comments/edit"; // templates/pages/comments/edit.html
    }

    //  댓글 수정 처리: POST /ui/comments/{commentId}/edit
    @PostMapping("/comments/{commentId}/edit")
    public String updateCommentFromUi(
            @PathVariable("commentId") Long commentId,
            @RequestParam("postId") Long postId,
            @RequestParam("content") String content,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        // 1) 비로그인 차단
        if (principal == null) {
            return "redirect:/ui/auth/login?next=/ui/posts/" + postId;
        }

        // 2) 수정 처리
        commentService.updateComment(commentId, principal, content);

        // 3) 상세로 복귀
        return "redirect:/ui/posts/" + postId;
    }

    // 댓글 삭제 처리: POST /ui/comments/{commentId}/delete
    @PostMapping("/comments/{commentId}/delete")
    public String deleteCommentFromUi(
            @PathVariable("commentId") Long commentId,
            @RequestParam("postId") Long postId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        // 1) 비로그인 차단
        if (principal == null) {
            return "redirect:/ui/auth/login?next=/ui/posts/" + postId;
        }

        // 2) 삭제 처리
        commentService.deleteComment(commentId, principal);

        // 3) 상세로 복귀
        return "redirect:/ui/posts/" + postId;
    }
}
