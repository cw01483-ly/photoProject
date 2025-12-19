package com.example.demo.domain.ui.controller;


import com.example.demo.global.security.CustomUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home(
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model
    ){
        // 로그인 여부 전달
        model.addAttribute("isLogin",principal != null);

        // 로그인 상태인 경우 username 전달
        model.addAttribute("username",principal != null ? principal.getUsername() : null);

        return "demo/index";
    }
}
