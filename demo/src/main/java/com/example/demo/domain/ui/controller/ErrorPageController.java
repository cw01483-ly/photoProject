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

import java.util.List;

@Controller
@RequestMapping("/error")
@RequiredArgsConstructor
public class ErrorPageController {

    /*
        - 서버 내부에서 자기 자신 API를 호출할 때는
          "공인 IP"가 아니라 반드시 localhost(127.0.0.1) 사용
          해야 EC2 + Docker 환경에서 동작
     */
    private static final String REFRESH_URL =
            "http://127.0.0.1:8008/api/auth/refresh";
    private final RestTemplate restTemplate = new RestTemplate(); // 서버 내부 호출 도구

    @GetMapping("/401")
    public String error401(HttpServletRequest request, HttpServletResponse response) {

        /*
            UI 계층에서 세션 사용 제거
            - getSession(true) 호출 금지
            - JSESSIONID 재생성 방지
            - refresh 무한 루프는 request attribute로 1회만 제어
         */

        Object attempted = request.getAttribute("REFRESH_ATTEMPTED");
        if (attempted != null) {
            return "pages/error/401";
        }
        request.setAttribute("REFRESH_ATTEMPTED", true);

        try {
            // 1) 브라우저 쿠키 그대로 전달
            HttpHeaders headers = new HttpHeaders();
            String cookieHeader = request.getHeader(HttpHeaders.COOKIE); // 요청 Cookie헤더 읽기
            if (cookieHeader != null && !cookieHeader.isBlank()) {
                headers.add(HttpHeaders.COOKIE, cookieHeader);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers); // 헤더만 가진 요청 엔티티 생성

            // 2) 서버 내부에서 refresh API 호출
            ResponseEntity<Void> refreshResp =
                    restTemplate.exchange(
                            REFRESH_URL,
                            HttpMethod.POST,
                            entity,
                            Void.class
                    );

            // refresh 응답 상태코드 출력
            System.out.println("[UI-REFRESH] refresh status: " + refreshResp.getStatusCode()); // 상태코드 로그

            // 3) refresh 응답의 Set-Cookie를 브라우저 응답으로 그대로 전달
            List<String> setCookies =
                    refreshResp.getHeaders().get(HttpHeaders.SET_COOKIE);

            if (setCookies != null) {
                for (String sc : setCookies) {
                    response.addHeader(HttpHeaders.SET_COOKIE, sc);
                }
            }

            // 4) refresh 성공 시 이전 페이지로 복귀
            if (refreshResp.getStatusCode().is2xxSuccessful()) {

                //  redirect 파라미터(상대경로)를 최우선 사용
                String redirect = request.getParameter("redirect"); // 예: /ui/posts/7
                if (redirect != null && !redirect.isBlank()) {

                    // Open Redirect 방지 - 상대경로만 허용 ("/"로 시작하는 경로만)
                    if (redirect.startsWith("/")) {
                        return "redirect:" + redirect;
                    }
                }

                // redirect 파라미터가 없으면 referer 사용 시도
                String referer = request.getHeader("Referer");
                if (referer != null && !referer.isBlank()) {
                    return "redirect:" + referer;
                }
                return "redirect:/";
            }

        } catch (Exception e) {
            System.out.println(
                    "[UI-REFRESH] refresh call failed: "
                            + e.getClass().getName()
                            + " - "
                            + e.getMessage()
            );
        }

        return "pages/error/401";
    }

    @GetMapping("/403") // GET /error/403
    public String error403() {
        return "pages/error/403";
    }
}
