package com.example.demo.domain.ui.controller;
/*
    UiDemoController
    - Demo UI 전용 렌더 컨트롤러
    - @AuthenticationPrincipal 사용 금지
    - 실제 인증/권한은 /api/** + JWT 쿠키에서 해결
*/

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui/demo")
public class UiDemoController {

    @GetMapping("/login") // GET /ui/demo/login
    public String loginPage() {
        return "demo/login"; // templates/demo/login.html
    }

    @GetMapping // GET /ui/demo
    public String demoHome(){
        return "demo/index"; // templates/demo/index.html
    }
}
