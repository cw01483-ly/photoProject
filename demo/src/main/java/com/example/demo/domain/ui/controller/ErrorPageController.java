package com.example.demo.domain.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class ErrorPageController {

    @GetMapping("/401")
    public String error401() {
        return "pages/error/401";
    }

    @GetMapping("/403")
    public String error403() {
        return "pages/error/403";
    }
}
