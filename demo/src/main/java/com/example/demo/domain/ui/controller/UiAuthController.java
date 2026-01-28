package com.example.demo.domain.ui.controller;

import com.example.demo.domain.user.dto.UserSignupRequestDto;
import com.example.demo.domain.user.service.UserService;
import jakarta.servlet.http.Cookie; // 로그아웃 쿠키 만료
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑
import org.springframework.web.bind.annotation.RequestMapping; // prefix 매핑
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/ui/auth")
public class UiAuthController {

    private final UserService  userService;
 /*
        JWT 단일화 적용
        - UI 로그인/로그아웃에서 "세션 인증"을 제거하고
        - API(/api/users/login, /api/auth/logout)를 서버에서 호출,
          Set-Cookie(ACCESS/REFRESH)를 그대로 브라우저에 전달
     */

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/login") // GET /ui/auth/login
    public String loginPage() {
        return "pages/auth/login";
    }

    @PostMapping("/login") // POST /ui/auth/login (경로 일치)
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // username 정규화: API(UserService.login)과 동일 정책(소문자+trim)
        String normalizedUsername = (username == null) ? null : username.trim().toLowerCase();

        try {
            // 1) 현재 요청 기반으로 같은 호스트/포트로 API 호출 (하드코딩 방지)
            String baseUrl = buildBaseUrl(request);
            String url = baseUrl + "/api/users/login";

            // 2) API 로그인 요청 바디(JSON): {"username":"...", "password":"..."}
            Map<String, Object> body = new HashMap<>();
            body.put("username", normalizedUsername);
            body.put("password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 3) API 호출 (성공하면 응답 헤더에 Set-Cookie(Access/Refresh)가 담김)
            ResponseEntity<String> apiResp = restTemplate.postForEntity(url, entity, String.class);

            // 4) API가 내려준 Set-Cookie들을 브라우저 응답에 그대로 전달
            List<String> setCookies = apiResp.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                for (String sc : setCookies) {
                    response.addHeader(HttpHeaders.SET_COOKIE, sc);
                }
            }

            // 5) UI 홈으로 이동
            return "redirect:/";

        } catch (RestClientResponseException e) {
            // API가 4xx/5xx로 실패한 경우: 로그인 실패로 처리
            return "redirect:/ui/auth/login?error=true";
        } catch (Exception e) {
            // 그 외 예외도 동일하게 실패 처리
            return "redirect:/ui/auth/login?error=true";
        }
    }

    @GetMapping("/signup") // GET /ui/auth/signup
    public String signupPage() {
        return "pages/auth/signup";
    }

    // UI 로그아웃: JWT 쿠키 만료 기반 로그아웃
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        /*
            JWT 단일화 기준 로그아웃
            - UI 계층에서 세션/컨텍스트 조작 없음
            - 로그아웃의 핵심은 HttpOnly JWT 쿠키 만료
         */

        try {
            // 1) 현재 요청 기반으로 같은 호스트/포트로 API 호출
            String baseUrl = buildBaseUrl(request);
            String url = baseUrl + "/api/auth/logout";

            // 2) 브라우저가 보낸 쿠키를 API 호출에도 그대로 전달 (Refresh 삭제/쿠키 만료에 필요)
            HttpHeaders headers = new HttpHeaders();
            String cookieHeader = request.getHeader(HttpHeaders.COOKIE);
            if (cookieHeader != null && !cookieHeader.isBlank()) {
                headers.add(HttpHeaders.COOKIE, cookieHeader);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 3) API 로그아웃 호출
            ResponseEntity<String> apiResp = restTemplate.postForEntity(url, entity, String.class);

            // 4) API가 내려준 Set-Cookie(만료 쿠키)들을 브라우저 응답에 그대로 전달
            List<String> setCookies = apiResp.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null) {
                for (String sc : setCookies) {
                    response.addHeader(HttpHeaders.SET_COOKIE, sc);
                }
            }

        } catch (Exception ignore) {
            // 로그아웃은 "최대한 진행"이 목표이므로 예외가 있어도 홈으로 보냄
        }

        expireCookie(response, "ACCESS_TOKEN");// API 호출 성공/실패와 무관하게, UI에서 쿠키 만료를 항상 내려서 로그아웃 보장
        expireCookie(response, "REFRESH_TOKEN"); // (HttpOnly 쿠키는 JS로 삭제 불가하므로 서버(Set-Cookie 만료)로만 삭제 가능)
        expireCookie(response, "JSESSIONID"); // 세션 혼용 흔적(브라우저 쿠키)까지 같이 제거

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

    private void expireCookie(HttpServletResponse response, String cookieName) {
        Cookie c = new Cookie(cookieName, ""); // 값 비우기
        c.setPath("/"); // Path가 다르면 삭제가 안 되므로 "/"로 고정
        c.setHttpOnly(true); // 원래 HttpOnly였던 쿠키 삭제를 위해 동일 속성 유지
        c.setMaxAge(0); // 즉시 만료
        response.addCookie(c); // Set-Cookie로 내려가며 브라우저에서 삭제
    }
}
