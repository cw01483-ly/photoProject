package com.example.demo.domain.ui.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/error")
@RequiredArgsConstructor
public class ErrorPageController {

    // 서버 내부 호출용 (localhost:8008)
    private static final String REFRESH_URL = "http://localhost:8008/api/auth/refresh";
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/401")
    public String error401(HttpServletRequest request, HttpServletResponse response) {

        // 무한 루프 방지: refresh 시도는 1회만
        Object attempted = request.getSession(true).getAttribute("REFRESH_ATTEMPTED");
        if (attempted != null) {
            request.getSession().removeAttribute("REFRESH_ATTEMPTED");
            return "pages/error/401";
        }
        request.getSession().setAttribute("REFRESH_ATTEMPTED", true);

        try {
            // 브라우저 쿠키를 그대로 전달
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, request.getHeader(HttpHeaders.COOKIE));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> refreshResp =
                    restTemplate.exchange(REFRESH_URL, HttpMethod.POST, entity, Void.class);

            // refresh 성공, 이전 페이지 복귀
            if (refreshResp.getStatusCode().is2xxSuccessful()) {
                String referer = request.getHeader("Referer");
                if (referer != null && !referer.isBlank()) {
                    return "redirect:" + referer;
                }
                return "redirect:/";
            }

        } catch (Exception ignore) {
            // 실패 시 아래로 떨어져 401 페이지 렌더링
        }

        return "pages/error/401";
    }

    @GetMapping("/403")
    public String error403() {
        return "pages/error/403";
    }
}
