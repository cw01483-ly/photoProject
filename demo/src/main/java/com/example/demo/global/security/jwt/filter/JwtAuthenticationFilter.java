package com.example.demo.global.security.jwt.filter;


import com.example.demo.global.security.CustomUserDetailsService;
import com.example.demo.global.security.jwt.properties.JwtProperties;
import com.example.demo.global.security.jwt.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
    JwtAuthenticationFilter
    - /api/** 요청에서 쿠키의 JWT를 읽어서 인증 처리할 예정
    - @Component 제거: 서블릿 필터로 "전체 요청"에 자동 등록되는 것을 방지
 */

//@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService; // JWT 검증,파싱 담당 서비스
    private final JwtProperties jwtProperties; // 쿠키 이름, 만료시간 등 설정 값
    private final CustomUserDetailsService customUserDetailsService; // username으로 UserDetails 로드

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        log.debug("[JWT FILTER] request uri = {}", request.getRequestURI());

        /*
             목표
            1) HttpOnly 쿠키에서 JWT 꺼내기
            2) JWT 유효하면(username 기반) UserDetails 로드
            3) SecurityContextHolder에 Authentication 올리기
            4) 다음 필터로 진행
         */
        try {
            // 1) 이미 인증이 올라가 있으면 통과 (중복 처리 방지)
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                return;
            }

            // 2) 쿠키에서 JWT 토큰 추출
            String token = extractTokenFromCookie(request, jwtProperties.getCookieName());
            // 토큰이 존재하지 않으면 "비로그인" 상태 그대로 다음으로 진행
            if (token == null || token.isBlank()) {
                return;
            }

            // 3) 토큰 검증 (서명,만료,형식)
            if (!jwtService.validateToken(token)) {
                return;
            }

            // 4) 토큰에서 username (subject) 꺼내기
            String username = jwtService.getUsername(token);
            if (username == null || username.isBlank()) {
                return;
            }

            // 5) username 활용하여 UserDetails 로드 ( principal.id를 사용 가능
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            // 6) 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // credentials
                            userDetails.getAuthorities() //권한 목록
                    );

            // 7) SecurityContext 에 인증 저장 (로그인 상태로 인식)
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            /*
                필터에서 예외를 던져버리면 "401/403 정책" 흐름이 꼬일 수 있으니
                여기서는 로깅만 하고, 인증을 올리지 않은 채로 다음 필터로 진행.
                -> 최종적으로 인증이 필요한 API면 401 반환
             */
            log.warn("[JWT FILTER] 인증 처리 실패. uri={}, message={}", request.getRequestURI(), e.getMessage());
        }

        // try/catch 끝나면 다음필터로 1번만 진행
        filterChain.doFilter(request, response);
    }

    // 쿠키 배열에서 특정이름(cookieName)을 가진 쿠키의 value를 찾아 반환, 없으면 null
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (cookieName == null || cookieName.isBlank()) { // cookieName 유효성 검사, null=비교불가, 공백=대상없음
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {// 요청에서 쿠키 배열 꺼냄
            return null;// request.getCookies()는 쿠키가 없으면 null 반환 할 수 있으니 주의
        }

        for (Cookie cookie : cookies) { // 향상된 for문, 쿠키배열 순회하여 쿠키의 key, 쿠키의 value(JWT문자열) 찾기
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
                // 원하는 쿠키 이름과 현재 쿠키의 이름이 같으면 해당 쿠키의 value(JWT)를 반환하고 종료
            }
        }
        return null; // 위 조건들을 모두 통과하면 쿠키가 없다는 뜻

    }
}
