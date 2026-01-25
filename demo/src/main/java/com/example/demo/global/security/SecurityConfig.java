package com.example.demo.global.security;

/* 스프링 시큐리티 전역 보안 설정 클래스
    - 기존:
     1) /api/users/login : 세션(IF_REQUIRED) 체인
     2) /api/**          : JWT(STATELESS) 체인
     3) 그 외 화면 요청   : 세션(formLogin) 체인
   - 변경: JWT 단일화
     1) 단일 체인에서 UI+API 모두 처리
     2) 세션 완전 미사용(STATELESS)
     3) UI(/ui/**) 인증/인가 실패 시 /error/401, /error/403 으로 리다이렉트
     4) API(/api/**) 인증/인가 실패 시 기존 JWT 핸들러(JSON 401/403) 유지

*/

import com.example.demo.global.security.jwt.filter.JwtAuthenticationFilter;
import com.example.demo.global.security.jwt.handler.JwtAccessDeniedHandler;
import com.example.demo.global.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.example.demo.global.security.jwt.properties.JwtProperties;
import com.example.demo.global.security.jwt.service.JwtService;
import com.example.demo.global.security.jwt.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // 필터 위치 지정



@Configuration // 설정클래스
@EnableMethodSecurity(prePostEnabled = true) // 메서드 보안 어노테이션 활성화(@PreAuthorize)
@RequiredArgsConstructor // final 필드 생성자 주입
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    // @Component 제거했으니, 여기서 직접 Bean으로 만들어 주입/사용
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService, // JwtService 빈 주입
            JwtProperties jwtProperties, // JwtProperties 빈 주입
            CustomUserDetailsService userDetailsService, // CustomUserDetailsService 빈 주입
            TokenBlacklistService tokenBlacklistService
    ) {
        return new JwtAuthenticationFilter(
                jwtService, jwtProperties, userDetailsService, tokenBlacklistService);
    }

    // UI(/ui/**) 요청에서만 401 -> /error/401 로 리다이렉트
    @Bean
    public AuthenticationEntryPoint uiAuthenticationEntryPoint() {
        return (request, response, authException) -> response.sendRedirect("/error/401");
    }

    // UI(/ui/**) 요청에서만 403 -> /error/403 로 리다이렉트
    @Bean
    public AccessDeniedHandler uiAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> response.sendRedirect("/error/403");
    }

    /*
        단일 JWT 체인(통합)
        - UI + API 모두 JWT 필터 적용
        - STATELESS 고정(세션 미사용)
        - UI(/ui/**)만 401/403 시 에러 페이지로 리다이렉트
        - API(/api/**)는 기존 JWT EntryPoint/DeniedHandler로 JSON 401/403 유지
    */
    @Bean
    public SecurityFilterChain unifiedJwtSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationEntryPoint uiAuthenticationEntryPoint,
            AccessDeniedHandler uiAccessDeniedHandler
    ) throws Exception {
        http
                //  개발 단계 간편 테스트 목적 CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // JWT 단일화이므로 서버 세션을 만들지 않음(STATELESS)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // UI는 리다이렉트, API는 기존 JSON 응답 유지
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String uri = request.getRequestURI();
                            if (uri != null && uri.startsWith("/ui/")) {
                                uiAuthenticationEntryPoint.commence(request, response, authException);
                                return;
                            }
                            jwtAuthenticationEntryPoint.commence(request, response, authException);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String uri = request.getRequestURI();
                            if (uri != null && uri.startsWith("/ui/")) {
                                uiAccessDeniedHandler.handle(request, response, accessDeniedException);
                                return;
                            }
                            jwtAccessDeniedHandler.handle(request, response, accessDeniedException);
                        })
                )

                // 요청별 인가(Authorization) 규칙 정의 (기존 API 규칙 + 기존 WEB 규칙 통합)
                .authorizeHttpRequests(auth -> auth

                        // Actuator는 필요한 것만 최소 허용
                        .requestMatchers("/actuator/prometheus", "/actuator/health").permitAll()
                        // 그 외 actuator는 전부 차단(정보 노출 방지)
                        .requestMatchers("/actuator/**").denyAll()

                        // 에러 페이지
                        .requestMatchers("/error/**").permitAll()

                        // 정적 리소스 + 홈
                        .requestMatchers("/", "/css/**", "/js/**", "/img/**").permitAll()

                        // 게시글 이미지 파일 (정적 리소스) 배포
                        .requestMatchers("/posts/**").permitAll()

                        // UI 인증 관련
                        .requestMatchers("/ui/auth/**").permitAll() // 로그인/회원가입/로그아웃 UI 등
                        .requestMatchers("/ui/posts/**").authenticated() // 게시글 UI: 로그인 필수

                        // API: 기존 규칙 유지, 회원가입은 비 로그인 허용
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // 로그인 시도는 누구나 허용
                        .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                        // JWT 로그아웃(POST /api/auth/logout)은 비로그인 허용 (쿠키 만료)
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        // Refresh 재발급(POST /api/auth/refresh)도 비로그인 허용, Access만료 상황에서도 동작해야하므로 permitAll
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        // 조회성 GET API는 공개
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        // 댓글 생성 : 로그인 필수
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/comments").authenticated()
                        // 댓글 수정 : 최소한 로그인 필수, PATCH /api/comments/{commentId}
                        .requestMatchers(HttpMethod.PATCH, "/api/comments/*").authenticated()
                        // 댓글 삭제 : 최소한 로그인 필수, DELETE /api/comments/{commentId}
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/*").authenticated()
                        // 그 외 /api/** 쓰기 작업은 인증 필요
                        .requestMatchers("/api/**").authenticated()

                        // 나머지는 허용 (기존 webSecurityFilterChain의 anyRequest().permitAll() 반영)
                        .anyRequest().permitAll()
                )

                // 단일화 체인에서는 formLogin/httpBasic 비활성화
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // 단일화 체인에 JWT 필터를 붙임
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 비밀번호 해시용 빈 등록(UserService에서 주입받아 사용)
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /*
         AuthenticationManager를 명시적으로 등록하여
         Spring Security가 CustomUserDetailsService를 사용하도록 연결
         로그인 시 userDetailsService.loadUserByUsername(username) 실행
         PasswordEncoder로 비밀번호 검증 자동 처리
    */
    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http, // Security 설정 핵심 객체
            PasswordEncoder passwordEncoder,// 비밀번호 검증
            CustomUserDetailsService userDetailsService // DB에서 사용자 조회 담당
    ) throws Exception{
        // SecurityBuilder에서 AuthenticationManagerBuilder를 가져와 설정
        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        // 만든 CustomUserDetailsService + PasswordEncoder 연결
        /*
            1) username -> DB 조회는 CustomUserDetailsService 사용
            2) 비밀번호 비교는 BCryptPasswordEncoder 사용
        */
        authBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);

        // AuthenticationManager 객체 생성 후 반환
        return authBuilder.build();
    }
}
