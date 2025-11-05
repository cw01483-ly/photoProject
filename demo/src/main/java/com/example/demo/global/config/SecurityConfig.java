package com.example.demo.global.config;

/* 스프링 시큐리티 전역 보안 설정 클래스*/

import lombok.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                        // 회원가입(POST /api/users) 은 비로그인 허용
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
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
}
