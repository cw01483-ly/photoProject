package com.example.demo.domain.ui.controller;

import com.example.demo.domain.user.dto.UserSignupRequestDto;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.AuthenticationException; // 인증 실패 예외
import org.springframework.stereotype.Controller; // UI 컨트롤러
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑
import org.springframework.web.bind.annotation.RequestMapping; // prefix 매핑
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/ui/auth")
public class UiAuthController {

    private final UserService  userService;
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
        // username 정규화: API(UserService.login)과 동일 정책(소문자+trim)
        String normalizedUsername = (username == null) ? null : username.trim().toLowerCase();

        try{
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(normalizedUsername, password);

        Authentication authentication =
                authenticationManager.authenticate(token);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        request.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return "redirect:/";
        }catch (AuthenticationException e){
            // 로그인 실패 시 500이 아닌 정상 실패 처리
            return "redirect:/ui/auth/login?error=true";
        }
    }

    @GetMapping("/signup") // GET /ui/auth/signup
    public String signupPage() {
        return "pages/auth/signup";
    }

    //  UI 로그아웃: 세션 무효화 + SecurityContext 정리
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


    @PostMapping("/signup") // POST /ui/auth/signup
    public String signup(
            @RequestParam String username,
            @RequestParam String nickname,
            @RequestParam String email,
            @RequestParam String password
    ) {
        UserSignupRequestDto requestDto =
                UserSignupRequestDto.builder()
                        .username(username)
                        .password(password)
                        .nickname(nickname)
                        .email(email)
                        .build();

        userService.register(requestDto);

        return "redirect:/ui/auth/login?signup=success";
    }
}
