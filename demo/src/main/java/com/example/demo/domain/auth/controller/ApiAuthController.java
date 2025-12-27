package com.example.demo.domain.auth.controller;

/*
    ApiAuthController
    - Demo UI에서 호출할 JWT 인증 관련  API ( logout / refresh )
    - POST /api/auth/logout
    - POST /api/auth/refresh
    - 동작:
            1) logout: JWT HttpOnly 쿠키를 만료시켜 브라우저에서 삭제되도록 Set-Cookie 내려줌
            2) refresh: Refresh 쿠키 기반으로 Access/Refresh 쿠키 재발급(회전 포함)하여 Set-Cookie 내려줌
*/

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.service.UserService;
import com.example.demo.global.response.ApiResponse;
import com.example.demo.global.security.CustomUserDetails;
import com.example.demo.global.security.jwt.properties.JwtProperties;
import com.example.demo.global.security.jwt.service.JwtService;
import com.example.demo.global.security.jwt.service.RefreshTokenService;
import com.example.demo.global.security.jwt.service.TokenBlacklistService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ApiAuthController {

    private final JwtProperties jwtProperties; // 쿠키명/만료/옵션(jwt.*) 설정값 사용
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserService userService;

    @PostMapping("/logout") // POST /api/auth/logout
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {

        // 1) 서버 측 SecurityContext 정리 (STATELESS라도 현재 요청 컨텍스트는 비워두는 게 깔끔)
        SecurityContextHolder.clearContext();

        // Refresh 쿠키에서 userId추출 (Redis Refresh 삭제용)
        Long userId = null;
        Cookie[] cookies = request.getCookies(); // 요청 쿠키 배열

        if (cookies != null) { // 쿠키 존재하면 탐색
            for (Cookie c : cookies) { // Refresh 쿠키 찾기
                if (jwtProperties.getRefreshCookieName().equals(c.getName())) {
                    String refreshToken = c.getValue(); // Refresh쿠기 값
                    // Refresh가 유효한 경우에만 userId 추출(만료/위조면 userId 추출 시도하지 않음)
                    if (refreshToken != null && !refreshToken.isBlank() && jwtService.validateToken(refreshToken)) {
                        userId = jwtService.getUserId(refreshToken); // Refresh claim에서 userId 추출
                    }
                    break; // Refresh 쿠키 확인 후 종료
                }
            }
        }

        // Redis에 저장된 Refresh 삭제 (단일 로그인: userId 당 Refresh 1개)
        if  (userId != null) {
            refreshTokenService.deleteRefreshToken(userId);
        }

         /*2) Access + Refresh JWT 쿠키를 모두 만료시키는 Set-Cookie 생성
            - value를 비우고
            - Max-Age=0 으로 즉시 만료
            - Path="/" 로 기존 로그인 쿠키와 동일 범위로 맞춤*/
        ResponseCookie expiredAccessCookie  = ResponseCookie
                .from(jwtProperties.getCookieName(), "") // 쿠키명은 yml(jwt.cookieName)과 동일
                .httpOnly(true) // HttpOnly 유지
                .path("/") // 로그인 때랑 동일하게
                .maxAge(0) // 즉시 만료
                // .secure(true) // HTTPS 적용 시 활성화
                .build();

        // Refresh JWT 쿠키 만료(브라우저에서 삭제되도록)
        ResponseCookie expiredRefreshCookie = ResponseCookie
                .from(jwtProperties.getRefreshCookieName(), "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        // 3) Set-Cookie 내려주고 성공 응답 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredAccessCookie.toString(), expiredRefreshCookie.toString())
                .body(ApiResponse.success("로그아웃 성공"));
    }

    // Refresh API
    @PostMapping("/refresh") // POST /api/auth/refresh
    public ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request) {
        // 1) Refresh 쿠키 값 꺼내기
        String refreshToken = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (jwtProperties.getRefreshCookieName().equals(c.getName())) {
                    refreshToken = c.getValue();
                    break;
                }
            }
        }

        // 2) Refresh 쿠키 존재 여부 확인
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh 토큰이 없습니다.");
        }

        // 3) Refresh 토큰 유효성 검증(서명/만료) validateToken(boolean) 결과를 반드시 반영
        boolean refreshValid = jwtService.validateToken(refreshToken);
        if (!refreshValid) {
            throw new IllegalArgumentException("Refresh 토큰이 유효하지 않습니다.");
        }

        // 4) Refresh에서 userId 추출
        Long userId = jwtService.getUserId(refreshToken);

        // 5) 새 Refresh 토큰 생성
        String newRefreshToken = jwtService.generateRefreshToken(userId);

        // 6) Redis Refresh 회전 (단일 로그인)
        Duration refreshTtl = Duration.ofDays(jwtProperties.getRefreshTokenExpDays());

        // rotateRefreshToken은 성공/실패(boolean) 반환
        boolean rotated = refreshTokenService.rotateRefreshToken(
                userId,
                refreshToken,      // presentedRefreshToken
                newRefreshToken,   // newRefreshToken
                refreshTtl
        );

        // 재사용 감지 사고 대응
        if (!rotated) {
            // 6-1) Redis에 저장된 Refresh 삭제
            refreshTokenService.deleteRefreshToken(userId);
            // 6-2) 요청에 포함된 Access 토큰 추출
            String accessToken = null;
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if (jwtProperties.getCookieName().equals(c.getName())) {
                        accessToken = c.getValue();
                        break;
                    }
                }
            }

            // 6-3) Access jti 추출 & 블랙리스트 등록
            if (accessToken != null && !accessToken.isBlank()) {
                String jti = jwtService.getJti(accessToken);// Access 토큰에서 jti를 뽑아 차단 대상을 특정
                Duration accessRemainingTtl = jwtService.getRemainingTtl(accessToken);
                // Access 남은 만료 시간(Duration)을 계산, 블랙리스트TTL을 Access 남은 TTL과 맞춰 부담 최소화
                tokenBlacklistService.blacklistAccessToken(jti, accessRemainingTtl);
                // jti를 Redis 블랙리스트에 등록
            }

            // 6-4) Access + Refresh 쿠키 모두 만료 처리
            ResponseCookie expiredAccessCookie = ResponseCookie// jwt.cookie-name을 사용해 동일한 쿠키를 만료시키기
                    .from(jwtProperties.getCookieName(), "")
                    .httpOnly(true)
                    .path("/")
                    .maxAge(0)
                    .build();

            ResponseCookie expiredRefreshCookie = ResponseCookie
                    .from(jwtProperties.getRefreshCookieName(), "")
                    .httpOnly(true)
                    .path("/")
                    .maxAge(0) // 즉시 만료
                    .build();

            // 6-5) 401 실패 응답 반환
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE,
                            expiredAccessCookie.toString(),
                            expiredRefreshCookie.toString())
                    .body(ApiResponse.fail("비정상적인 토큰 재사용이 감지되어 로그아웃되었습니다."));
        }


        // 7) userId -> User -> CustomUserDetails 복원
        User user = userService.getById(userId);
        CustomUserDetails principal = new CustomUserDetails(user);

        // 8) 새 Access Token 발급
        String newAccessToken = jwtService.generateAccessToken(principal);

        // 9) Access 쿠키 생성
        ResponseCookie accessCookie = ResponseCookie.from(
                        jwtProperties.getCookieName(), newAccessToken
                )
                .httpOnly(true)
                .path("/")
                .maxAge(jwtProperties.getAccessTokenExpMinutes() * 60L)
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .build();

        // 10) Refresh 쿠키 생성
        ResponseCookie refreshCookie = ResponseCookie.from(
                        jwtProperties.getRefreshCookieName(), newRefreshToken
                )
                .httpOnly(true)
                .path("/")
                .maxAge(refreshTtl)
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .build();

        // 12) 쿠키 갱신 + 성공 응답
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString(), refreshCookie.toString())
                .body(ApiResponse.success(null, "토큰 재발급 성공"));
    }

}
