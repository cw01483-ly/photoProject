package com.example.demo.global.security;

/* 스프링 시큐리티 전역 보안 설정 클래스*/

import com.example.demo.global.security.jwt.filter.JwtAuthenticationFilter;
import com.example.demo.global.security.jwt.handler.JwtAccessDeniedHandler;
import com.example.demo.global.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.example.demo.global.security.jwt.properties.JwtProperties;
import com.example.demo.global.security.jwt.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order; // 체인 우선순위 지정

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // 필터 위치 지정

import static org.springframework.security.config.Customizer.withDefaults;

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
            CustomUserDetailsService userDetailsService // CustomUserDetailsService 빈 주입
    ) {
        return new JwtAuthenticationFilter(jwtService, jwtProperties, userDetailsService);
    }

    /*
        SecurityFilterChain을 3개로 분리
            1) 세션 로그인

            2) /api/** 요청
               - JWT(HttpOnly 쿠키) 기반 인증
               - STATELESS (세션 사용 X)

            3) 그 외 화면 요청
               - 세션(formLogin) 기반 인증
               - 기존 브라우저 화면과 궁합 고려


     */


    /*
        세션 로그인
          - 현재 API체인(/api/**)은 STATELESS, 세션유지 불가하므로
             /api/users/login 만 ★세션 허용★ 체인으로 분리
             >>> UserService.login()에서 SecurityContextHolder에 넣은 인증이 세션에 저장, 다음요청까지 유지됨
     */
    // ⭐ 세션 로그인 체인 (/api/users/login) : 세션 기반 로그인 유지용
    @Bean
    @Order(1) // 우선순위 1 : /api/users/login 은 이 체인이 먼저 적용되도록
    public SecurityFilterChain apiSessionLoginFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/users/login") // 이 체인은 로그인 엔드포인트에만 적용
                .csrf(csrf -> csrf.disable()) // JSON 로그인 테스트 목적 CSRF 비활성화
                // ↓ 세션 허용 >> "로그인 상태" 가 다음 요청까지 이어짐
                .sessionManagement(sm
                        -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(ex ->ex
                        // 비로그인 -> 401
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        // 권한 부족 -> 403
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth
                        // 로그인 시도는 누구나 허용
                        .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                        .anyRequest().denyAll() // 이 체인에서 로그인 외 요청은 차단
                )

                //로그인 체인에서는 formLogin/httpBasic 사용 X (JSON 로그인 API 사용)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }




    // ⭐ API 체인 (/api/**) : JWT 기반
    @Bean
    @Order(2) // 우선순위 2 : /api/**는 이 체인이 먼저 적용되도록 유도
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
                .securityMatcher("/api/**") // 이 체인은 /api/** 요청에만 적용
                .csrf(csrf -> csrf.disable()) // 개발 단계 간편 테스트 목적 CSRF 비활성화
                .sessionManagement //  JWT 기반이므로 서버 세션을 만들지 않음(STATELESS)
                (sm -> sm.sessionCreationPolicy
                        (SessionCreationPolicy.STATELESS))

                .exceptionHandling(ex ->ex
                        // 비로그인 -> 401
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        // 권한 부족 -> 403
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                // 요청별 인가(Authorization) 규칙 정의
                .authorizeHttpRequests(auth -> auth
                        // 회원가입은 비 로그인 허용
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // JWT 로그아웃(POST /api/auth/logout)은 비로그인 허용 (쿠키 만료)
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        // 조회성 GET API는 공개
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        // 댓글 생성 : 로그인 필수
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/comments").authenticated()
                        // 댓글 수정 : 초소한 로그인 필수, PATCH /api/comments/{commentId}
                        .requestMatchers(HttpMethod.PATCH, "/api/comments/*").authenticated()
                        // 댓글 삭제 : 최소한 로그인 필수, DELETE /api/comments/{commentId}
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/*").authenticated()
                        // 그 외 /api/** 쓰기 작업은 인증 필요
                        .anyRequest().authenticated()
                )
                // API 체인에서 formLogin/httpBasic 비활성화
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // /api/** 체인에만 JWT 필터를 붙임
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }



    // ⭐ WEB 체인 (그 외) : 세션(formLogin) 기반
    @Bean
    @Order(3) // /api/** 가 아닌 모든 요청 처리
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // 개발 단계 간편 테스트 목적 CSRF 비활성화, 추 후 ThymeLeaf 사용 시 활성화 계획
                .csrf(csrf -> csrf.disable())

                // 요청별 인가(Authorization) 규칙 정의
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스,공개 페이지 허용
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll()

                        // 나머지 임시 모두 허용 (추후 강화 계획)
                        .anyRequest().permitAll()
                )

                // 폼 로그인 (브라우저 테스트) 기본 로그인 페이지 사용
                .formLogin(withDefaults()); // 람다 Customizer 스타일

        // 화면 체인에서는 httpBasic 굳이 필요 없어서 꺼도 됨
        http.httpBasic(basic -> basic.disable());
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
