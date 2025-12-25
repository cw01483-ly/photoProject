package com.example.demo.global.security.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/*
    TokenBlacklistService
    [역할]
    - Access Token을 "선택적으로" 즉시 무효화하기 위한 블랙리스트 서비스
        - 평상시에는 Access는 Stateless(JWT)로 검증
        - "사고 이벤트"가 발생시, 해당 Access(jti)를 Redis에 올려 즉시 차단
    [기본 아이디어]
    - Access 토큰에 jti(UUID)를 넣어 둔다.
    - 로그아웃/침해 발생 시:
        SET auth:bl:access:{jti} "1" EX {남은 Access TTL}
    - 이후 요청에서 필터가 Access를 검증한 뒤:
        EXISTS auth:bl:access:{jti} 가 true면 -> 인증 실패(401) 처리
*/
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    /*
        RedisTemplate
        - Redis에 key/value를 저장하고 조회하기 위한 템플릿
        - String 기반으로 사용
     */
    private final RedisTemplate<String, String> redisTemplate;


    /*
        블랙리스트 키 Prefix
        - 실제 키 예: auth:bl:access:550e8400-e29b-41d4-a716-446655440000
     */
    private static final String ACCESS_BLACKLIST_PREFIX = "auth:bl:access:";


    /*
        블랙리스트 등록
        - 특정 Access Token(jti)을 블랙리스트에 등록
        - TTL은 "Access 남은 만료 시간"만큼만 설정
          (Access가 자연 만료되면 블랙리스트도 자동 삭제되어 Redis 부담 최소화)
        @param jti  Access Token의 고유 식별자(UUID)
        @param ttl  블랙리스트 유지 시간(= Access 남은 TTL)
     */
    public void blacklistAccessToken(String jti, Duration ttl) {

        if (jti == null || jti.isBlank()) {
            // jti가 없다면 차단할 토큰을 특정할 수 없으므로 아무 작업도 하지 않는다.
            return;
        }

        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            // 남은 TTL이 0 이하라면 이미 만료 수준이므로 저장 의미 X
            return;
        }
        String key = generateAccessBlacklistKey(jti);

        redisTemplate.opsForValue().set(
                key,
                "1",   // 값은 의미 없고 "존재 여부"만 중요하므로 고정값 사용
                ttl
        );
    }



    /*
        블랙리스트 여부 확인
        - 필터에서 Access 검증 후, 이 메서드로 차단 여부를 확인
        @param jti Access Token의 고유 식별자
        @return true면 차단 대상, false면 정상
     */
    public boolean isBlacklisted(String jti) {

        if (jti == null || jti.isBlank()) {
            // jti가 없으면 블랙리스트 판단 자체가 불가능하므로 차단 X
            // (단, 설계상 Access에는 반드시 jti가 들어가야 함)
            return false;
        }

        String key = generateAccessBlacklistKey(jti);

        Boolean exists = redisTemplate.hasKey(key);

        // hasKey가 null을 반환할 수 있는 상황을 대비해 안전하게 처리
        return Boolean.TRUE.equals(exists);
    }


    // 블랙리스트 키 생성 - 키 규칙을 한 곳에서 관리하기 위함
    private String generateAccessBlacklistKey(String jti) {
        return ACCESS_BLACKLIST_PREFIX + jti;
    }
}
