package com.example.demo.domain.ui.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller; // UI 컨트롤러
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑
import org.springframework.web.bind.annotation.RequestMapping; // prefix 매핑
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/ui/auth")
public class UiAuthController {

    private final AuthenticationManager authenticationManager;

    @GetMapping("/login") // GET /ui/auth/login
    public String loginPage() {
        return "pages/auth/login";
    }

    @PostMapping("/login") // POST /ui/auth/login (경로 일치)
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request
    ) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(username, password);

        Authentication authentication =
                authenticationManager.authenticate(token);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        request.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return "redirect:/ui/posts";
    }

    @GetMapping("/signup") // GET /ui/auth/signup
    public String signupPage() {
        return "pages/auth/signup";
    }

    //  UI 로그아웃
    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        // 세션 무효화
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // SecurityContext 정리
        SecurityContextHolder.clearContext();

        // 로그아웃 후 홈
        return "redirect:/";
    }

}
