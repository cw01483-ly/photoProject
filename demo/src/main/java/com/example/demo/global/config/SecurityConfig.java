package com.example.demo.global.config;

/* 스프링 시큐리티 전역 보안 설정 클래스*/

import com.example.demo.global.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults; // ★ 추가

@Configuration // 설정클래스
@EnableMethodSecurity(prePostEnabled = true) // 메서드 보안 어노테이션 활성화(@PreAuthorize)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                // 개발 단계 간편 테스트 목적 CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // 요청별 인가(Authorization) 규칙 정의
                .authorizeHttpRequests(auth -> auth
                        // 회원가입(POST /api/users), 로그인(POST /api/users/login) 은 비로그인 허용
                        .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login").permitAll()
                        // 그 외 /api/users/**는 인증필요
                        .requestMatchers("/api/users/**").authenticated()
                        // 나머지는 임시로 모두 허용(추후 점진적 강화)
                        .anyRequest().permitAll()
                )

                // 폼 로그인(브라우저 테스트) - 기본 로그인 페이지 사용
                .formLogin(withDefaults())     // ★ 람다/Customizer 스타일

                // HTTP Basic (API 테스트용)
                .httpBasic(withDefaults());    // ★ 람다/Customizer 스타일

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
