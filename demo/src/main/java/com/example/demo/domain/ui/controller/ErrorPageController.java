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
            // 현재 요청 기반으로 같은 호스트/포트로 API 호출 (하드코딩 방지)
            String baseUrl = buildBaseUrl(request);
            String refreshUrl = baseUrl + "/api/auth/refresh";

            // 브라우저 쿠키를 그대로 전달
            HttpHeaders headers = new HttpHeaders();

            // Cookie 헤더가 null/blank일 수 있으니 방어
            String cookieHeader = request.getHeader(HttpHeaders.COOKIE);
            if (cookieHeader != null && !cookieHeader.isBlank()) {
                headers.add(HttpHeaders.COOKIE, cookieHeader);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> refreshResp =
                    restTemplate.exchange(refreshUrl, HttpMethod.POST, entity, Void.class);

            // refresh 성공 시, API가 내려준 Set-Cookie(새 Access 쿠키 등)를 브라우저 응답에 그대로 전달
            List<String> setCookies = refreshResp.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                for (String sc : setCookies) {
                    response.addHeader(HttpHeaders.SET_COOKIE, sc);
                }
            }

            // refresh 성공, 이전 페이지 복귀
            if (refreshResp.getStatusCode().is2xxSuccessful()) {

                // 성공 시 루프 방지 플래그 제거(다음 만료 상황에서도 재시도 가능)
                request.getSession().removeAttribute("REFRESH_ATTEMPTED");

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

    /*
        현재 요청을 기준으로 "같은 서버"에 API를 호출하기 위한 baseUrl 생성
        - 예: http://15.165.160.212:8008
        - 로컬/배포 환경 모두 대응(포트 하드코딩 제거)
     */
    private String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme(); // http / https
        String host = request.getServerName(); // 도메인 or IP
        int port = request.getServerPort(); // 8008 등

        // 기본 포트(http 80 / https 443)면 포트 표기 생략
        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);

        if (isDefaultPort) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }
}
