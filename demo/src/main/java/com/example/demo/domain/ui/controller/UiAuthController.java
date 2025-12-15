package com.example.demo.domain.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiAuthController {

    @GetMapping("/ui/auth/login")
    public String loginPage() {
        return "pages/auth/login";
    }

    @GetMapping("/ui/auth/signup")
    public String signupPage() {
        return "pages/auth/signup";
    }
}
