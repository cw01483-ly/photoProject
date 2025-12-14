package com.example.demo.global.security.jwt.handler;

import com.example.demo.global.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
    JwtAuthenticationEntryPoint
    - "비로그인(인증 실패)" 상황에서 Spring Security가 호출하는 진입점(EntryPoint)
    -  비로그인 = 401
    - API 서버 관점에서 redirect(302) 같은 동작 대신, 명확히 401로 내려주기 위해 사용
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException{

        // 어떤 요청이 인증 실패로 떨어졌는지 로그로 남기기 (비로그인)
        log.debug("[SECURITY] 401 Unauthorized. uri={}, message={}",
                request.getRequestURI(),
                authException.getMessage()
        );

        // 비로그인(인증X) 이므로 401로 통일
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> body =
                ApiResponse.fail("인증이 필요합니다.");

        response.getWriter().write(
                objectMapper.writeValueAsString(body)
        );
    }
}
