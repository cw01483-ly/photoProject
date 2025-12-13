package com.example.demo.global.security.jwt.service;


import com.example.demo.global.security.CustomUserDetails;
import com.example.demo.global.security.jwt.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/*
    JwtService
    - JWT(Access Token) 발급/검증/파싱을 담당하는 서비스
    - A안(세션 + JWT 병행)에서 /api/** 는 "JWT(HttpOnly 쿠키)"로 인증할 예정
    - payload: userId, username, role 을 JWT claims에 담음
*/
@Service // 스프링 빈 등록
@RequiredArgsConstructor //final 필드 생성자 주입
public class JwtService {

    private final JwtProperties jwtProperties;

    /*
        [1] Access Token 생성 (아직 구현 X)
        - 로그인 성공 시 호출
        - CustomUserDetails 에서 userId, username, role 추출
        - JWT 문자열 생성 후 반환
     */
    public String createAccessToken(CustomUserDetails userDetails) {

        // TODO
        // 1. userDetails 에서 id, username, role 추출
        // 2. 만료 시간 계산
        // 3. JWT 서명 후 토큰 생성

        return null;
    }

    /*
        [2] 토큰 검증 (아직 구현 X)
        - JwtAuthenticationFilter 에서 호출
        - 토큰 위변조 / 만료 여부 확인
     */
    public boolean validateToken(String token) {

        // TODO
        // 1. 서명 검증
        // 2. 만료 시간 체크

        return false;
    }

    /*
        [3] 토큰에서 사용자 정보 추출 (아직 구현 X)
        - 검증이 끝난 토큰에서 Claims 파싱
        - CustomUserDetails 재구성
     */
    public CustomUserDetails getUserDetailsFromToken(String token) {

        // TODO
        // 1. Claims 파싱
        // 2. id, username, role 추출
        // 3. CustomUserDetails 생성 후 반환

        return null;
    }
}
