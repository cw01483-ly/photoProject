package com.example.demo.global.security.jwt.service;

import com.example.demo.domain.user.role.UserRole;
import com.example.demo.global.security.CustomUserDetails;
import com.example.demo.global.security.jwt.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

/*
    JwtService
    - JWT "생성 / 검증 / 파싱"을 한 곳에 모아둔 서비스
    - (2) JwtAuthenticationFilter는 여기서 제공하는 기능을 이용해
      토큰이 유효한지 확인하고, 토큰 안의 user 정보(클레임)를 꺼내 SecurityContext를 구성

    [(세션 + JWT 병행) 전제]
    - 세션 인증을 완전히 버리는 게 아니라, JWT도 함께 사용.
    - 다만 JWT 자체의 생성/검증/파싱 책임은 JwtService가 전담.
*/
@Service // 스프링 빈 등록
@RequiredArgsConstructor //final 필드 생성자 주입
public class JwtService {

    // yml의 jwt.* 설정값 주입
    private final JwtProperties jwtProperties;

    /* 토큰에 넣을(클레임) 키 이름
        - "id"    : principal.id 를 토큰에 담아 필터가 DB조회 없이 principal 구성을 쉽게함
        - "role"  : ROLE_USER / ROLE_ADMIN 판단에 필요
    */
    private static final String CLAIM_USER_ID = "id";
    private static final String CLAIM_ROLE = "role";

    /* ⭐ 토근 생성
        - 로그인 성공 시 AccessToken 생성
        - subject엔 일반적으로 username 삽입. ( CustomUserDetails 기준 getUsername() == User.username )
        - id/role은 claims로 담는다.
     */
    public String generateAccessToken(CustomUserDetails principal) {

        // 만료 시간 계산 ( 현재 시간 + 설정된 만료 시간 )
        Instant now = Instant.now(); // 현재
        Instant exp = now.plusSeconds(jwtProperties.getAccessTokenExpMinutes() * 60L); // 만료시간
        // 30 * 60 = 1800초 = 30분

        return Jwts.builder()
                .setSubject(principal.getUsername()) // subject = username
                .claim(CLAIM_USER_ID, principal.getId()) // principal.id
                .claim(CLAIM_ROLE, extractRoleName(principal)) // "USER"/"ADMIN"
                .setIssuedAt(Date.from(now)) // 발급 시간
                .setExpiration(Date.from(exp)) // 만료 시간
                .signWith(getSigningKey()) // 서명 키
                .compact(); // JWT 문자열 생성
    }

    /* ⭐ 토큰 검증
        - 서명 검증 + 만료 검증 + 파싱 가능 여부 검증
        - 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // 서명/만료 포함 검증
            return true;
        } catch (ExpiredJwtException e) {
            return false; // 만료
        } catch (JwtException | IllegalArgumentException e) {
            return false; // 위조/형식 오류 등
        }
    }

    /* ⭐ 토큰 파싱
        - Claims(본문) 꺼내기
        - 내부적으로 서명 검증 수행
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // 검증 키
                .build()
                .parseClaimsJws(token) // 파싱(검증 포함)
                .getBody(); // Claims 반환
    }

    // ⭐ 토큰에서 username(subject) 꺼내기
    public String getUsername(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    // ⭐ 토큰에서 userId(claim "id") 꺼내기
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);

        Object raw = claims.get(CLAIM_USER_ID);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("JWT claim 'id' is not a number.");
    }

    /* ⭐ 토큰에서 role(claim "role") 꺼내기
        - "USER"/"ADMIN" -> UserRole enum
     */
    public UserRole getRole(String token) {
        Claims claims = parseClaims(token);

        String roleName = claims.get(CLAIM_ROLE, String.class);
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        return UserRole.valueOf(roleName);
    }

    /* ⭐ 서명 키 생성
        - yml의 secret은 Base64가 아니라 일반 문자열로 보이므로 UTF-8 bytes로 처리
        - HS256은 최소 32바이트 이상 권장
     */
    private Key getSigningKey() {

        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret is missing.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret is too short. Use at least 32 bytes (256-bit) for HS256.");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    /* ⭐ principal authorities 예: "ROLE_USER"
        -> 토큰에는 "USER"만 저장해두면 UserRole.valueOf()로 바로 복구 가능
     */
    private String extractRoleName(CustomUserDetails principal) {

        String first = principal.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority()) // "ROLE_USER"
                .orElse("ROLE_USER");

        if (first.startsWith("ROLE_")) {
            return first.substring("ROLE_".length()); // "USER"
        }

        return first;
    }


}
