package com.example.demo.global.security.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

/*
    RefreshTokenService
    [역할]
    - Refresh Token을 Redis에 저장, 관리하는 전용 서비스
    - 단일 로그인 기준, userId 당 Refresh Token은 항상 1개만 유지
    - Refresh Token의 핵심 보안 정책인
        1) 회전(rotate)
        2) 재사용 감지
      를 담당
    [설계 배경]
    - Refresh Token은 Access Token보다 수명이 길기 때문에
      서버가 반드시 "상태(state)"를 관리해야 함
    - Redis를 사용하여:
        - 현재 유효한 Refresh Token 1개만 유지
        - 재사용 시 즉시 침해로 판단 가능하게 한다.
*/
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    /*
        RedisTemplate
        - Redis 접근을 위한 스프링 템플릿
        - key / value 모두 String으로 사용
        - key 예시: auth:refresh:123
        - value 예시: 실제 Refresh Token 문자열 (추후 해시로 변경 가능)
     */
    private final RedisTemplate<String, String> redisTemplate;


    /*
        Refresh Token Redis Key Prefix
        - Refresh Token 저장 키의 공통 접두어
        - userId를 뒤에 붙여 실제 Redis key를 만듦
        - 예: auth:refresh:1, auth:refresh:42
     */
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";


    /*
        Refresh Token 저장
        - 로그인 성공 시
        - 또는 Refresh 회전(rotate) 성공 시 호출
        - 기존 값이 있으면 덮어써서 이전 Refresh를 무효화
        @param userId : 사용자 식별자
        @param refreshToken : 새로 발급된 Refresh Token
        @param ttl : Refresh Token의 만료 시간 (14일)
     */
    public void saveRefreshToken(Long userId, String refreshToken, Duration ttl) {
        String key = generateKey(userId);

        redisTemplate.opsForValue().set(
                key, // Redis key (auth:refresh:{userId})
                refreshToken, // Redis value (Refresh Token)
                ttl // TTL (Refresh 만료 시간)
        );
    }


    /*
        Refresh Token 조회
        - 현재 Redis에 저장된 Refresh Token을 조회
        - 존재하지 않으면 null 반환, 디버깅, 테스트, 검증로직 에 사용 가능
     */
    public String getRefreshToken(Long userId) {

        String key = generateKey(userId);

        return redisTemplate.opsForValue().get(key);
    }


    /*
        Refresh Token 삭제
        - 로그아웃 시
        - 재사용 감지 발생 시
        - 강제 로그아웃 정책 적용 시 사용
        Redis에서 Refresh Token을 제거함으로써
        해당 사용자의 모든 세션을 무효화
     */
    public void deleteRefreshToken(Long userId) {

        String key = generateKey(userId);

        redisTemplate.delete(key);
    }



    /*  ★★★★★★
        Refresh Token 회전 + 재사용 감지
        Refresh Token 보안의 중심
        [동작 규칙]
        1. Redis에 저장된 Refresh Token을 조회
        2. 클라이언트가 보낸 Refresh Token과 비교
        3. 결과에 따라 다음과 같이 처리

        - 저장된 Refresh가 없음
            -> 이미 로그아웃되었거나
              이미 재사용 감지로 폐기된 상태
            -> 침해로 간주

        - 저장된 Refresh != 클라이언트 Refresh
            -> 과거 Refresh 재사용 시도
            -> 침해로 간주

        - 저장된 Refresh == 클라이언트 Refresh
            -> 정상
            -> 새 Refresh Token으로 교체 (회전)

        @return
        - true  : 정상 회전 성공
        - false : 재사용 감지(D-1)
     */
    public boolean rotateRefreshToken(
            Long userId,
            String presentedRefreshToken,
            String newRefreshToken,
            Duration ttl
    ) {
        String key = generateKey(userId);

        // Redis에 저장된 현재 Refresh Token 조회
        String storedRefreshToken = redisTemplate.opsForValue().get(key);

        /*
            저장된 Refresh가 없는 경우
            - 이미 로그아웃됨
            - 이미 재사용 감지로 삭제됨
            - TTL 만료됨
            -> 어떤 경우든 보안상 "정상 상태"가 아니므로 재사용 감지로 처리
         */
        if (storedRefreshToken == null) {
            return false;
        }


        // Refresh Token 비교 (equals 비교를 통해 정확히 일치하는지 확인)

        if (!Objects.equals(storedRefreshToken, presentedRefreshToken)) {
            // 기존 Refresh와 다르면 침해로 판단
            return false;
        }

        /*
            정상적인 Refresh 요청인 경우
            - 새 Refresh Token으로 교체
            - TTL을 다시 설정
            - 기존 Refresh는 즉시 무효화됨
         */
        redisTemplate.opsForValue().set(
                key,
                newRefreshToken,
                ttl
        );

        return true;
    }


    // Redis Key 생성 공통 메서드 - Refresh Token 관련 Redis key 생성 규칙을 한 곳에서 관리하기 위한 메서드

    private String generateKey(Long userId) {
        return REFRESH_KEY_PREFIX + userId;
    }
}
