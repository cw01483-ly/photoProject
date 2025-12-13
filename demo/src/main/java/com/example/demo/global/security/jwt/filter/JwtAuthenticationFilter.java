package com.example.demo.global.security.jwt.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
    JwtAuthenticationFilter
    - /api/** 요청에서 쿠키의 JWT를 읽어서 인증 처리할 예정
    - @Component 제거: 서블릿 필터로 "전체 요청"에 자동 등록되는 것을 방지
 */

//@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException{

        System.out.println("[JWT FILTER] request uri = " + request.getRequestURI());

        // 쿠키에서 JWT 꺼내어 SecurityContext 인증 올릴 예정
        filterChain.doFilter(request,response);
    }
}
