package com.example.demo.domain.auth.controller;

/*
    ApiAuthController
    - Demo UI에서 호출할 "JWT 로그아웃" 전용 API
    - POST /api/auth/logout
    - 동작: JWT HttpOnly 쿠키를 만료시켜 브라우저에서 삭제되도록 Set-Cookie 내려줌
*/

import com.example.demo.global.response.ApiResponse;
import com.example.demo.global.security.jwt.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ApiAuthController {

    private final JwtProperties jwtProperties; // jwt.cookieName 사용

    @PostMapping("/logout") // POST /api/auth/logout
    public ResponseEntity<ApiResponse<Void>> logout() {

        // 1) 서버 측 SecurityContext 정리 (STATELESS라도 현재 요청 컨텍스트는 비워두는 게 깔끔)
        SecurityContextHolder.clearContext();

         /*2) JWT 쿠키를 만료 시키는 Set-Cookie 생성
            - value를 비우고
            - Max-Age=0 으로 즉시 만료
            - Path="/" 로 기존 로그인 쿠키와 동일 범위로 맞춤*/
        ResponseCookie expiredCookie = ResponseCookie
                .from(jwtProperties.getCookieName(), "") // 쿠키명은 yml(jwt.cookieName)과 동일
                .httpOnly(true) // HttpOnly 유지
                .path("/") // 로그인 때랑 동일하게
                .maxAge(0) // 즉시 만료
                // .secure(true) // HTTPS 적용 시 활성화
                .build();

        // 3) Set-Cookie 내려주고 성공 응답 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(ApiResponse.success("로그아웃 성공"));
    }
}
