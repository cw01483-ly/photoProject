package com.example.demo.global.security.jwt.handler;


import com.example.demo.global.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;

/*
    JwtAccessDeniedHandler
    - 로그인은 되었지만 권한이 없는 경우 호출
    - 형님 정책: 권한 부족 = 403
    - ApiResponse JSON 형식으로 응답 통일
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException{

        log.debug("[SECURITY] 403 Forbidden. uri={}, message={}",
                request.getRequestURI(),
                accessDeniedException.getMessage()
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> body =
                ApiResponse.fail("접근 권한이 없습니다.");

        response.getWriter().write(
                objectMapper.writeValueAsString(body)
        );
    }
}
