package com.example.demo.global.security;

/* 스프링 시큐리티 전역 보안 설정 클래스*/

import com.example.demo.global.security.jwt.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository; // ★ [추가] 세션에 SecurityContext 저장용
import org.springframework.security.web.context.SecurityContextRepository; // ★ [추가] 저장소 인터페이스

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    // @Component 제거했으니, 여기서 직접 Bean으로 만들어 주입/사용
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /*
        ===========================
        [세션 검증용 임시 모드]
        - /api/** 도 세션을 "읽고/저장"할 수 있게 설정
        - 로그인 성공 시 JSESSIONID가 발급되는지 확인
        - JWT 필터는 잠깐 제외(세션 검증이 목적이라 혼선 방지)
        ===========================
     */

    // ★ [추가] SecurityContext를 세션(HttpSession)에 저장/조회하는 Repository
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /*
        세션 로그인
          - /api/users/login 만 ★세션 허용★ 체인으로 분리
          - ★ 핵심 변경: SecurityContextRepository를 통해 "세션 저장"을 명시적으로 보장
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSessionLoginFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository // ★ [추가] 세션 저장소 주입
    ) throws Exception {
        http
                .securityMatcher("/api/users/login")
                .csrf(csrf -> csrf.disable())

                // ↓ 세션 허용 (세션 생성 가능)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // ★ [추가] 인증 결과(SecurityContext)를 세션에 저장/조회하도록 지정
                .securityContext(sc -> sc
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(false) // ★ 추가: SecurityContext를 자동으로 세션에 저장
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                        .anyRequest().denyAll()
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // ★ [중요] 로그인 체인에서는 JWT 필터를 붙이지 않음(세션 검증이 목적)
        return http.build();
    }

    /*
        ★ [변경] /api/** 체인을 "세션 검증용"으로 전환
        - 기존: STATELESS (세션 사용 X)
        - 변경: IF_REQUIRED (세션 사용 가능)
        - JWT 필터 제거(검증 단계에서는 세션만으로 인증 유지 확인)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository // ★ [추가] 세션 저장/조회
    ) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())

                // ★ [변경] 세션 검증 단계에서는 /api/**도 세션을 읽을 수 있어야 함
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // ★ [추가] /api/** 요청에서도 세션에 저장된 SecurityContext를 읽을 수 있게 함
                .securityContext(sc -> sc
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(false) // ★ 추가: SecurityContext를 자동으로 세션에 저장
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        // 회원가입은 비 로그인 허용
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // 로그인은 위 @Order(1) 체인이 담당
                        // 조회성 GET API는 공개
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        // 댓글 생성/수정/삭제 등은 로그인 필수
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/comments").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/comments/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/*").authenticated()
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // ★ [변경] 세션 검증 단계에서는 JWT 필터를 잠깐 제거한다.
        // http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ⭐ WEB 체인 (그 외) : 세션(formLogin) 기반
    @Bean
    @Order(3)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().permitAll()
                )
                .formLogin(withDefaults());

        http.httpBasic(basic -> basic.disable());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            PasswordEncoder passwordEncoder,
            CustomUserDetailsService userDetailsService
    ) throws Exception{
        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);

        return authBuilder.build();
    }
}
