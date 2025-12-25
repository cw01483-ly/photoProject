package com.example.demo.global.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.connection.RedisConnectionFactory; // 스프링이 자동 구성해주는 Redis 연결 팩토리
import org.springframework.data.redis.core.RedisTemplate;  // Redis 조작용 템플릿
import org.springframework.data.redis.serializer.StringRedisSerializer; // key/value를 문자열로 저장하기 위한 직렬화기


/*
    RedisConfig
        Redis 관련 Bean 설정 클래스
         JWT Refresh + Redis + 회전 + 재사용 감지 + 선택적 블랙리스트 에서 Redis를 사용하기 위한 기반 파일
        RedisConnectionFactory(LettuceConnectionFactory 등)는 보통 Spring Boot가 자동 구성(auto-config)해줌
         -> 여기선 "RedisTemplate 직렬화 설정"을 중심으로 구성.
           -RedisConnectionFactory : 자동 연결,자동 구성
           -LettuceConnectionFactory : Redis와 연결해주는 실제 라이브러리
           -RedisTemplate 직렬화 설정 : 데이터를 어떤 형태로 주고받을지
*/
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory){
        RedisTemplate<String, String> template = new RedisTemplate<>();
        // RedisTemplate 생성 (key/value를 String으로 다루기 위한 템플릿)

        template.setConnectionFactory(connectionFactory);
        // 템플릿이 사용할 Redis 연결 팩토리를 주입

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // key/value를 모두 문자열로 저장하기 위한 직렬화기

        template.setKeySerializer(stringSerializer);
        // Redis key 직렬화 설정 (예: "auth:refresh:123")

        template.setValueSerializer(stringSerializer);
        // Redis value 직렬화 설정 (예: refreshHash 문자열)

        template.setHashKeySerializer(stringSerializer);
        // Redis Hash 자료구조를 쓸 경우 hash key도 문자열로 저장

        template.setHashValueSerializer(stringSerializer);
        // Redis Hash 자료구조를 쓸 경우 hash value도 문자열로 저장

        template.afterPropertiesSet();
        // 설정값 반영(초기화)

        return template;
        // String 기반 RedisTemplate 반환
    }

}
